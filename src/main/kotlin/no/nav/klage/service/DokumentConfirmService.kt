package no.nav.klage.service

import no.nav.klage.clients.fileapi.FileApiClient
import no.nav.klage.domain.entities.DokumentStatus
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.domain.entities.RegistreringDokument
import no.nav.klage.exceptions.*
import no.nav.klage.repository.RegistreringRepository
import no.nav.klage.service.DokumentConfirmService.Companion.FOLLOW_STALL_TIMEOUT_MILLIS
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

/**
 * Drives an uploaded document from [DokumentStatus.UPLOADING] to a terminal status, reporting every
 * state change to the caller through `emit` as it happens.
 *
 * The whole thing is idempotent and resumable: every status change is committed on its own, so a
 * client that loses the connection (or a request that dies) can simply call confirm again and the
 * machine picks up from the last persisted status. Every individual step is safe to repeat.
 */
@Service
class DokumentConfirmService(
    private val dokumentStateService: DokumentStateService,
    private val fileApiClient: FileApiClient,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)

        //Enforced on the FINAL (converted) file, since that is what gets journalført to Joark via
        //kabal-document. 512 MB.
        private const val MAX_SIZE = 536870912L

        private const val FOLLOW_POLL_INTERVAL_MILLIS = 500L

        //How long we follow another request without seeing any progress before taking over the work
        //ourselves. Covers the case where the request that was driving the document died.
        private const val FOLLOW_STALL_TIMEOUT_MILLIS = 60 * 1000L

        //Total time a single confirm request is willing to spend before giving up.
        private const val TOTAL_TIMEOUT_MILLIS = 10 * 60 * 1000L

        //Only coordinates requests within one pod. Across pods the persisted status plus the fact
        //that every step is repeatable is what keeps things correct.
        private val locks = ConcurrentHashMap<UUID, ReentrantLock>()
    }

    /**
     * Blocks until the document reaches a terminal status, calling [emit] with the current state and
     * with every state change along the way.
     *
     * Errors that happen before anything has been emitted are thrown, so the caller can turn them
     * into a normal error response. Once the first state has been emitted, failures are reported as
     * terminal statuses instead.
     */
    fun confirmDokument(registreringId: UUID, dokumentId: UUID, emit: (DokumentState) -> Unit) {
        val deadline = System.currentTimeMillis() + TOTAL_TIMEOUT_MILLIS
        var lastEmitted: EmitKey? = null

        val emitOnChange: (DokumentState) -> Unit = { state ->
            val current = state.emitKey()
            if (current != lastEmitted) {
                emit(state)
                lastEmitted = current
            }
        }

        while (System.currentTimeMillis() < deadline) {
            val state = dokumentStateService.getState(registreringId = registreringId, dokumentId = dokumentId)

            if (state.status == DokumentStatus.UNSUPPORTED_TYPE) {
                throw IllegalInputException("Dokumentet har en filtype som ikke støttes og kan ikke bekreftes.")
            }

            if (state.status.isTerminal()) {
                emitOnChange(state)
                return
            }

            val lock = acquireLock(dokumentId)

            if (lock != null) {
                try {
                    drive(registreringId = registreringId, dokumentId = dokumentId, emit = emitOnChange)
                    return
                } finally {
                    //Removed while still holding it, so a request that captured this lock cannot end
                    //up driving the same document as a request that creates the replacement.
                    locks.remove(dokumentId, lock)
                    lock.unlock()
                }
            }

            //Someone else is already driving this document forward. Tag along instead of doing the
            //work twice, but take over if they stop making progress.
            if (followProgress(registreringId = registreringId, dokumentId = dokumentId, emit = emitOnChange)) {
                return
            }

            logger.warn("No progress on document {} while following, taking over.", dokumentId)
        }

        logger.warn("Gave up confirming document {} after {} ms.", dokumentId, TOTAL_TIMEOUT_MILLIS)
        throw IllegalInputException("Dokumentet ble ikke ferdig behandlet. Prøv igjen.")
    }

    /**
     * Returns the lock if it was acquired, or null if another request in this pod holds it. Retries
     * on the race where the lock we found in the map was removed just before we got it.
     */
    private fun acquireLock(dokumentId: UUID): ReentrantLock? {
        while (true) {
            val lock = locks.computeIfAbsent(dokumentId) { ReentrantLock() }

            if (!lock.tryLock()) {
                return null
            }

            if (locks[dokumentId] === lock) {
                return lock
            }

            //The owner removed this lock from the map on its way out, so it no longer guards anything.
            lock.unlock()
        }
    }

    private fun drive(registreringId: UUID, dokumentId: UUID, emit: (DokumentState) -> Unit) {
        var state = dokumentStateService.getState(registreringId = registreringId, dokumentId = dokumentId)

        val mellomlagerId = state.mellomlagerId
            ?: error("drive() called for document $dokumentId without a mellomlagerId")

        //Nothing has been emitted yet, so a missing upload can still be reported as a normal error.
        if (state.status == DokumentStatus.UPLOADING) {
            val metadata = fileApiClient.getDocumentMetadata(mellomlagerId)
            if (!metadata.exists) {
                throw IllegalInputException("Fant ikke opplastet dokument. Ble opplastingen fullført?")
            }
            state = dokumentStateService.setStatus(
                registreringId = registreringId,
                dokumentId = dokumentId,
                status = DokumentStatus.UPLOADED,
                size = metadata.size,
                contentType = metadata.contentType,
            )
        }

        //Always let the client know where we are before doing any more work, so a resumed stream is
        //immediately useful.
        emit(state)

        //A document left in CONVERTING without a scanned generation cannot be converted safely, so
        //it goes through the scan again.
        val needsScan = state.status in setOf(DokumentStatus.UPLOADED, DokumentStatus.VIRUS_SCANNING) ||
                (state.status == DokumentStatus.CONVERTING && state.scannedGeneration == null)

        if (needsScan) {
            state = dokumentStateService.setStatus(
                registreringId = registreringId,
                dokumentId = dokumentId,
                status = DokumentStatus.VIRUS_SCANNING,
            )
            emit(state)

            val scanResult = try {
                fileApiClient.scanDocument(mellomlagerId)
            } catch (e: AttachmentCouldNotBeConvertedException) {
                emit(fail(registreringId, dokumentId, DokumentStatus.CONVERSION_FAILED))
                return
            }

            if (scanResult.hasVirus) {
                logger.warn("Virus found in uploaded document {}, deleting it.", dokumentId)
                fileApiClient.deleteDocument(mellomlagerId)
                emit(fail(registreringId, dokumentId, DokumentStatus.VIRUS_FOUND))
                return
            }

            if (scanResult.requiresConversion) {
                state = dokumentStateService.setScanned(
                    registreringId = registreringId,
                    dokumentId = dokumentId,
                    scannedGeneration = scanResult.generation,
                    size = scanResult.size,
                    contentType = scanResult.contentType,
                )
            } else {
                val size = scanResult.size ?: 0L
                if (isTooLarge(size = size, dokumentId = dokumentId, mellomlagerId = mellomlagerId)) {
                    emit(fail(registreringId, dokumentId, DokumentStatus.CONVERSION_FAILED))
                    return
                }
                state = dokumentStateService.setDone(
                    registreringId = registreringId,
                    dokumentId = dokumentId,
                    size = size,
                    contentType = scanResult.contentType ?: MediaType.APPLICATION_PDF_VALUE,
                )
            }
            emit(state)
        }

        if (state.status == DokumentStatus.CONVERTING) {
            val convertResult = try {
                fileApiClient.convertDocument(
                    id = mellomlagerId,
                    scannedGeneration = state.scannedGeneration!!,
                )
            } catch (e: AttachmentCouldNotBeConvertedException) {
                emit(fail(registreringId, dokumentId, DokumentStatus.CONVERSION_FAILED))
                return
            }

            val size = convertResult.size ?: 0L
            if (isTooLarge(size = size, dokumentId = dokumentId, mellomlagerId = mellomlagerId)) {
                emit(fail(registreringId, dokumentId, DokumentStatus.CONVERSION_FAILED))
                return
            }

            state = dokumentStateService.setDone(
                registreringId = registreringId,
                dokumentId = dokumentId,
                size = size,
                contentType = convertResult.contentType ?: MediaType.APPLICATION_PDF_VALUE,
            )
            emit(state)
        }
    }

    private fun isTooLarge(size: Long, dokumentId: UUID, mellomlagerId: String): Boolean {
        if (size <= MAX_SIZE) {
            return false
        }
        logger.warn("Document {} is too large ({} bytes), deleting it.", dokumentId, size)
        fileApiClient.deleteDocument(mellomlagerId)
        return true
    }

    private fun fail(registreringId: UUID, dokumentId: UUID, status: DokumentStatus): DokumentState =
        dokumentStateService.setStatus(
            registreringId = registreringId,
            dokumentId = dokumentId,
            status = status,
        )

    /**
     * Follows the progress made by another request. Returns true if the document reached a terminal
     * status, and false if nothing happened for [FOLLOW_STALL_TIMEOUT_MILLIS], meaning the caller
     * should take over the work.
     */
    private fun followProgress(registreringId: UUID, dokumentId: UUID, emit: (DokumentState) -> Unit): Boolean {
        var lastSeen: EmitKey? = null
        var lastChange = System.currentTimeMillis()

        while (System.currentTimeMillis() - lastChange < FOLLOW_STALL_TIMEOUT_MILLIS) {
            val state = dokumentStateService.getState(
                registreringId = registreringId,
                dokumentId = dokumentId,
            )
            val status = state.status

            val current = state.emitKey()
            if (current != lastSeen) {
                emit(state)
                lastSeen = current
                lastChange = System.currentTimeMillis()
            }

            if (status.isTerminal()) {
                return true
            }

            Thread.sleep(FOLLOW_POLL_INTERVAL_MILLIS)
        }

        return false
    }
}

data class DokumentState(
    val status: DokumentStatus,
    val size: Long,
    val contentType: String,
    val mellomlagerId: String?,
    val scannedGeneration: Long?,
)

/** The part of [DokumentState] that is reported to the client, used to only emit actual changes. */
private data class EmitKey(
    val status: DokumentStatus,
    val size: Long,
    val contentType: String,
)

private fun DokumentState.emitKey() = EmitKey(status = status, size = size, contentType = contentType)

/**
 * Reads and writes the state of a single [RegistreringDokument] in its own transaction, so that every
 * status change is visible to other requests (and other pods) the moment it happens.
 */
@Service
class DokumentStateService(
    private val registreringRepository: RegistreringRepository,
    private val tokenUtil: TokenUtil,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    fun getState(registreringId: UUID, dokumentId: UUID): DokumentState =
        getRegistrering(registreringId = registreringId, lock = false).findDokument(dokumentId).toState()

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun setStatus(
        registreringId: UUID,
        dokumentId: UUID,
        status: DokumentStatus,
        size: Long? = null,
        contentType: String? = null,
    ): DokumentState {
        val registrering = getRegistrering(registreringId)
        val dokument = registrering.findDokument(dokumentId)

        dokument.status = status
        if (size != null) {
            dokument.size = size
        }
        if (contentType != null) {
            dokument.contentType = contentType
        }
        registrering.modified = LocalDateTime.now()

        return dokument.toState()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun setScanned(
        registreringId: UUID,
        dokumentId: UUID,
        scannedGeneration: Long,
        size: Long?,
        contentType: String?,
    ): DokumentState {
        val registrering = getRegistrering(registreringId)
        val dokument = registrering.findDokument(dokumentId)

        dokument.scannedGeneration = scannedGeneration
        dokument.status = DokumentStatus.CONVERTING
        //The size before conversion. Better than nothing while the conversion is running, and it is
        //replaced with the size of the PDF when the document is done.
        if (size != null) {
            dokument.size = size
        }
        //Likewise the content type before conversion, replaced with the PDF content type when done.
        if (contentType != null) {
            dokument.contentType = contentType
        }
        registrering.modified = LocalDateTime.now()

        return dokument.toState()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun setDone(registreringId: UUID, dokumentId: UUID, size: Long, contentType: String): DokumentState {
        val registrering = getRegistrering(registreringId)
        val dokument = registrering.findDokument(dokumentId)

        dokument.size = size
        dokument.contentType = contentType
        dokument.status = DokumentStatus.DONE
        registrering.modified = LocalDateTime.now()

        return dokument.toState()
    }

    private fun getRegistrering(registreringId: UUID, lock: Boolean = true): Registrering {
        val registrering = if (lock) {
            registreringRepository.findById(registreringId).orElse(null)
        } else {
            registreringRepository.findByIdWithoutLock(registreringId)
        } ?: throw RegistreringNotFoundException("Registrering ikke funnet.")

        if (registrering.createdBy != tokenUtil.getCurrentIdent()) {
            throw MissingAccessException("Registreringen tilhører ikke deg.")
        }
        if (registrering.finished != null) {
            throw IllegalUpdateException("Registreringen er allerede ferdigstilt.")
        }

        return registrering
    }

    private fun Registrering.findDokument(dokumentId: UUID): RegistreringDokument =
        dokumenter.find { it.id == dokumentId }
            ?: throw RegistreringNotFoundException("Dokument ikke funnet.")

    private fun RegistreringDokument.toState() = DokumentState(
        status = status,
        size = size,
        contentType = contentType,
        mellomlagerId = mellomlagerId,
        scannedGeneration = scannedGeneration,
    )
}
