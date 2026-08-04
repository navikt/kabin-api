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
import no.nav.klage.util.withExtension
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

/**
 * Drives an uploaded document from [DokumentStatus.UPLOADING] to a terminal status, reporting every
 * status change to the caller through `emit` as it happens.
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
     * Blocks until the document reaches a terminal status, calling [emit] for the current status and
     * for every status change along the way.
     *
     * Errors that happen before anything has been emitted are thrown, so the caller can turn them
     * into a normal error response. Once the first status has been emitted, failures are reported as
     * terminal statuses instead.
     */
    fun confirmDokument(registreringId: UUID, dokumentId: UUID, emit: (DokumentStatus) -> Unit) {
        val deadline = System.currentTimeMillis() + TOTAL_TIMEOUT_MILLIS
        var lastEmitted: DokumentStatus? = null

        val emitOnChange: (DokumentStatus) -> Unit = { status ->
            if (status != lastEmitted) {
                emit(status)
                lastEmitted = status
            }
        }

        while (System.currentTimeMillis() < deadline) {
            val state = dokumentStateService.getState(registreringId = registreringId, dokumentId = dokumentId)

            if (state.status.isTerminal()) {
                emitOnChange(state.status)
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

    private fun drive(registreringId: UUID, dokumentId: UUID, emit: (DokumentStatus) -> Unit) {
        var state = dokumentStateService.getState(registreringId = registreringId, dokumentId = dokumentId)

        //Nothing has been emitted yet, so a missing upload can still be reported as a normal error.
        if (state.status == DokumentStatus.UPLOADING) {
            val metadata = fileApiClient.getDocumentMetadata(state.mellomlagerId)
            if (!metadata.exists) {
                throw IllegalInputException("Fant ikke opplastet dokument. Ble opplastingen fullført?")
            }
            state = dokumentStateService.setStatus(
                registreringId = registreringId,
                dokumentId = dokumentId,
                status = DokumentStatus.UPLOADED,
            )
        }

        //Always let the client know where we are before doing any more work, so a resumed stream is
        //immediately useful.
        emit(state.status)

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
            emit(state.status)

            val scanResult = try {
                fileApiClient.scanDocument(state.mellomlagerId)
            } catch (e: AttachmentCouldNotBeConvertedException) {
                emit(fail(registreringId, dokumentId, DokumentStatus.CONVERSION_FAILED))
                return
            }

            if (scanResult.hasVirus) {
                logger.warn("Virus found in uploaded document {}, deleting it.", dokumentId)
                fileApiClient.deleteDocument(state.mellomlagerId)
                emit(fail(registreringId, dokumentId, DokumentStatus.VIRUS_FOUND))
                return
            }

            if (scanResult.requiresConversion) {
                state = dokumentStateService.setScanned(
                    registreringId = registreringId,
                    dokumentId = dokumentId,
                    scannedGeneration = scanResult.generation,
                )
            } else {
                val size = scanResult.size ?: 0L
                if (isTooLarge(size = size, dokumentId = dokumentId, mellomlagerId = state.mellomlagerId)) {
                    emit(fail(registreringId, dokumentId, DokumentStatus.CONVERSION_FAILED))
                    return
                }
                state = dokumentStateService.setDone(
                    registreringId = registreringId,
                    dokumentId = dokumentId,
                    size = size,
                )
            }
            emit(state.status)
        }

        if (state.status == DokumentStatus.CONVERTING) {
            val convertResult = try {
                fileApiClient.convertDocument(
                    id = state.mellomlagerId,
                    scannedGeneration = state.scannedGeneration!!,
                )
            } catch (e: AttachmentCouldNotBeConvertedException) {
                emit(fail(registreringId, dokumentId, DokumentStatus.CONVERSION_FAILED))
                return
            }

            val size = convertResult.size ?: 0L
            if (isTooLarge(size = size, dokumentId = dokumentId, mellomlagerId = state.mellomlagerId)) {
                emit(fail(registreringId, dokumentId, DokumentStatus.CONVERSION_FAILED))
                return
            }

            state = dokumentStateService.setDone(
                registreringId = registreringId,
                dokumentId = dokumentId,
                size = size,
            )
            emit(state.status)
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

    private fun fail(registreringId: UUID, dokumentId: UUID, status: DokumentStatus): DokumentStatus =
        dokumentStateService.setStatus(
            registreringId = registreringId,
            dokumentId = dokumentId,
            status = status,
        ).status

    /**
     * Follows the progress made by another request. Returns true if the document reached a terminal
     * status, and false if nothing happened for [FOLLOW_STALL_TIMEOUT_MILLIS], meaning the caller
     * should take over the work.
     */
    private fun followProgress(registreringId: UUID, dokumentId: UUID, emit: (DokumentStatus) -> Unit): Boolean {
        var lastSeen: DokumentStatus? = null
        var lastChange = System.currentTimeMillis()

        while (System.currentTimeMillis() - lastChange < FOLLOW_STALL_TIMEOUT_MILLIS) {
            val status = dokumentStateService.getState(
                registreringId = registreringId,
                dokumentId = dokumentId,
            ).status

            if (status != lastSeen) {
                emit(status)
                lastSeen = status
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
    val mellomlagerId: String,
    val scannedGeneration: Long?,
)

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
        getRegistrering(registreringId).findDokument(dokumentId).toState()

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun setStatus(registreringId: UUID, dokumentId: UUID, status: DokumentStatus): DokumentState {
        val registrering = getRegistrering(registreringId)
        val dokument = registrering.findDokument(dokumentId)

        dokument.status = status
        registrering.modified = LocalDateTime.now()

        return dokument.toState()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun setScanned(registreringId: UUID, dokumentId: UUID, scannedGeneration: Long): DokumentState {
        val registrering = getRegistrering(registreringId)
        val dokument = registrering.findDokument(dokumentId)

        dokument.scannedGeneration = scannedGeneration
        dokument.status = DokumentStatus.CONVERTING
        registrering.modified = LocalDateTime.now()

        return dokument.toState()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun setDone(registreringId: UUID, dokumentId: UUID, size: Long): DokumentState {
        val registrering = getRegistrering(registreringId)
        val dokument = registrering.findDokument(dokumentId)

        //Anything that reaches DONE is stored as PDF: the scan rejects everything that is neither a
        //PDF nor a convertible image. Setting the extension here (instead of only when we actually
        //converted) also keeps a resumed conversion correct, since converting an already converted
        //document is a no-op in kabal-file-api.
        dokument.name = withExtension(name = dokument.name, extension = "pdf")
        dokument.size = size
        dokument.status = DokumentStatus.DONE
        registrering.modified = LocalDateTime.now()

        return dokument.toState()
    }

    private fun getRegistrering(registreringId: UUID): Registrering =
        registreringRepository.findById(registreringId)
            .orElseThrow { RegistreringNotFoundException("Registrering ikke funnet.") }
            .also {
                if (it.createdBy != tokenUtil.getCurrentIdent()) {
                    throw MissingAccessException("Registreringen tilhører ikke deg.")
                }
                if (it.finished != null) {
                    throw IllegalUpdateException("Registreringen er allerede ferdigstilt.")
                }
            }

    private fun Registrering.findDokument(dokumentId: UUID): RegistreringDokument =
        dokumenter.find { it.id == dokumentId }
            ?: throw RegistreringNotFoundException("Dokument ikke funnet.")

    private fun RegistreringDokument.toState() = DokumentState(
        status = status,
        mellomlagerId = mellomlagerId,
        scannedGeneration = scannedGeneration,
    )
}
