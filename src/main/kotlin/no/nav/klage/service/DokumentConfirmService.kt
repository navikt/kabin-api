package no.nav.klage.service

import no.nav.klage.clients.fileapi.FileApiClient
import no.nav.klage.config.FileApiClientConfiguration
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

/**
 * Drives an uploaded document from [DokumentStatus.UPLOADING] to a terminal status, reporting every
 * state change to the caller through `emit` as it happens.
 *
 * Processing runs as four steps, each of which commits an `X_DONE` status when it succeeds: the
 * upload is verified, the file is virus scanned, it is converted to PDF if it is not one already,
 * and finally it is checked against the size limit and marked [DokumentStatus.DONE].
 *
 * The whole thing is idempotent and resumable: every status change is committed on its own, so a
 * client that loses the connection (or a request that dies) can simply call confirm again and the
 * machine picks up from the last committed `X_DONE` status without redoing the steps before it.
 * Every individual step is safe to repeat.
 *
 * Concurrent confirm requests for the same document are coordinated through the persisted status
 * alone, which works across pods: a request that finds the document in an in-progress status follows
 * the request that is already doing that step instead of repeating it, and takes over if that
 * request stops making progress.
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

        /**
         * How often a following request checks whether the request that is driving the document has
         * made any progress.
         *
         * Each check loads the registrering and its documents in its own transaction, and following
         * can now go on for [FOLLOW_STALL_TIMEOUT_MILLIS], so this is kept at a second rather than
         * something snappier. Status changes are rare enough that relaying one up to a second late
         * makes no practical difference to the client.
         */
        private const val FOLLOW_POLL_INTERVAL_MILLIS = 1000L

        /**
         * How long we follow another request without seeing any progress before taking over the work
         * ourselves. Covers the case where the request that was driving the document died.
         *
         * A request that is driving is usually just waiting for kabal-file-api, and nothing changes
         * while it does, so this has to outlast a single call there. At 60 seconds a perfectly healthy
         * conversion of a large file looked like a request that had died, and following requests took
         * over work that was still being done.
         *
         * A takeover that turns out to be premature is safe: once either request finishes the
         * document, [DokumentStateService] refuses the other one's writes. It only costs a scan or a
         * conversion that nobody needed, which is why this is not simply set to the worst case of
         * every retry in [FileApiClient] timing out. That would be around 24 minutes, and a document
         * whose driver really did die would be stuck for all of it.
         */
        private val FOLLOW_STALL_TIMEOUT_MILLIS =
            FileApiClientConfiguration.RESPONSE_TIMEOUT.plusSeconds(30).toMillis()
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
        var lastEmitted: EmitKey? = null

        val emitOnChange: (DokumentState) -> Unit = { state ->
            val current = state.emitKey()
            if (current != lastEmitted) {
                emit(state)
                lastEmitted = current
            }
        }

        val state = dokumentStateService.getState(registreringId = registreringId, dokumentId = dokumentId)

        if (state.status == DokumentStatus.UNSUPPORTED_TYPE) {
            throw IllegalInputException("Dokumentet har en filtype som ikke støttes og kan ikke bekreftes.")
        }

        if (state.status.isTerminal()) {
            emitOnChange(state)
            return
        }

        //Someone else is in the middle of a step. Tag along instead of doing the work twice, but fall
        //through and take over if they stop making progress, which is what happens when the request
        //that was driving the document died.
        if (state.status.isInProgress()) {
            if (followProgress(registreringId = registreringId, dokumentId = dokumentId, emit = emitOnChange)) {
                return
            }
            logger.warn("No progress on document {} while following, taking over.", dokumentId)
        }

        drive(registreringId = registreringId, dokumentId = dokumentId, emit = emitOnChange)
    }

    /**
     * Carries out the steps the document still needs, one at a time, committing each result before
     * starting the next.
     *
     * Every write can be refused by [DokumentStateService] if another request finished the document
     * in the meantime. The state that comes back is then that request's result rather than the one we
     * asked for, so each write is followed by a check that reports it and stops. The checks sit
     * before the calls to kabal-file-api on purpose: carrying on would not only waste a scan or a
     * conversion, it could delete the file belonging to the document the other request just finished.
     */
    private fun drive(registreringId: UUID, dokumentId: UUID, emit: (DokumentState) -> Unit) {
        var state = dokumentStateService.getState(registreringId = registreringId, dokumentId = dokumentId)

        val mellomlagerId = state.mellomlagerId
            ?: error("drive() called for document $dokumentId without a mellomlagerId")

        //Nothing has been emitted yet, so a missing upload can still be reported as a normal error.
        //UPLOADING_DONE is checked as well as UPLOADING, since a document can also enter drive()
        //already verified: after a reset, or when an earlier request died between the upload check
        //and the scan. Reporting a missing file here beats letting the scan fail with a vague error.
        if (state.status == DokumentStatus.UPLOADING || state.status == DokumentStatus.UPLOADING_DONE) {
            val metadata = fileApiClient.getDocumentMetadata(mellomlagerId)
            if (!metadata.exists) {
                throw IllegalInputException("Fant ikke opplastet dokument. Ble opplastingen fullført?")
            }
            state = dokumentStateService.setStatus(
                registreringId = registreringId,
                dokumentId = dokumentId,
                status = DokumentStatus.UPLOADING_DONE,
                size = metadata.size,
                contentType = metadata.contentType,
            )
            if (state.status != DokumentStatus.UPLOADING_DONE) {
                emit(state)
                return
            }
        }

        //Always let the client know where we are before doing any more work, so a resumed stream is
        //immediately useful.
        emit(state)

        //A document waiting to be converted without a scanned generation cannot be converted safely,
        //so it goes through the scan again.
        val needsScan = state.status in setOf(DokumentStatus.UPLOADING_DONE, DokumentStatus.VIRUS_SCANNING) ||
                (state.status in setOf(DokumentStatus.VIRUS_SCANNING_DONE, DokumentStatus.CONVERTING) &&
                        state.scannedGeneration == null)

        if (needsScan) {
            state = dokumentStateService.setStatus(
                registreringId = registreringId,
                dokumentId = dokumentId,
                status = DokumentStatus.VIRUS_SCANNING,
            )
            emit(state)

            //Another request finished the document first, so there is nothing left to scan. Bailing
            //out here also keeps us from deleting a file that the finished document is using.
            if (state.status != DokumentStatus.VIRUS_SCANNING) {
                return
            }

            val scanResult = try {
                fileApiClient.scanDocument(mellomlagerId)
            } catch (e: AttachmentUnsupportedTypeException) {
                emit(fail(registreringId, dokumentId, DokumentStatus.UNSUPPORTED_TYPE))
                return
            } catch (e: Exception) {
                logger.error("Unexpected error while scanning document {}", dokumentId, e)
                emit(fail(registreringId, dokumentId, DokumentStatus.VIRUS_SCAN_FAILED))
                return
            }

            if (scanResult.hasVirus) {
                logger.warn("Virus found in uploaded document {}, deleting it.", dokumentId)
                fileApiClient.deleteDocument(mellomlagerId)
                emit(fail(registreringId, dokumentId, DokumentStatus.VIRUS_FOUND))
                return
            }

            state = dokumentStateService.setScanned(
                registreringId = registreringId,
                dokumentId = dokumentId,
                scannedGeneration = scanResult.generation,
                size = scanResult.size,
                contentType = scanResult.contentType,
            )
            emit(state)

            if (state.status != DokumentStatus.VIRUS_SCANNING_DONE) {
                return
            }

            //Already a PDF, so there is nothing to convert and the scanned file is the final one.
            if (!scanResult.requiresConversion) {
                val size = scanResult.size ?: 0L
                if (isTooLarge(size = size, dokumentId = dokumentId, mellomlagerId = mellomlagerId)) {
                    emit(fail(registreringId, dokumentId, DokumentStatus.CONVERSION_FAILED))
                    return
                }
                emit(
                    dokumentStateService.setDone(
                        registreringId = registreringId,
                        dokumentId = dokumentId,
                        size = size,
                        contentType = scanResult.contentType ?: MediaType.APPLICATION_PDF_VALUE,
                    )
                )
                return
            }
        }

        if (state.status in setOf(DokumentStatus.VIRUS_SCANNING_DONE, DokumentStatus.CONVERTING)) {
            state = dokumentStateService.setStatus(
                registreringId = registreringId,
                dokumentId = dokumentId,
                status = DokumentStatus.CONVERTING,
            )
            emit(state)

            //As above: the document is already finished, so converting it again would only risk
            //deleting the file it ended up with.
            if (state.status != DokumentStatus.CONVERTING) {
                return
            }

            val convertResult = try {
                fileApiClient.convertDocument(
                    id = mellomlagerId,
                    scannedGeneration = state.scannedGeneration!!,
                )
            } catch (e: AttachmentUnsupportedTypeException) {
                emit(fail(registreringId, dokumentId, DokumentStatus.UNSUPPORTED_TYPE))
                return
            } catch (e: AttachmentConversionFailedException) {
                logger.error("Unexpected conversion failure for document {}", dokumentId, e)
                emit(fail(registreringId, dokumentId, DokumentStatus.UNEXPECTED_ERROR))
                return
            } catch (e: Exception) {
                logger.error("Unexpected error while converting document {}", dokumentId, e)
                emit(fail(registreringId, dokumentId, DokumentStatus.UNEXPECTED_ERROR))
                return
            }

            state = dokumentStateService.setConverted(
                registreringId = registreringId,
                dokumentId = dokumentId,
                size = convertResult.size ?: 0L,
                contentType = convertResult.contentType ?: MediaType.APPLICATION_PDF_VALUE,
            )
            emit(state)

            if (state.status != DokumentStatus.CONVERTING_DONE) {
                return
            }
        }

        //The size limit is enforced on the converted file, since that is the one that is journalført.
        if (state.status == DokumentStatus.CONVERTING_DONE) {
            if (isTooLarge(size = state.size, dokumentId = dokumentId, mellomlagerId = mellomlagerId)) {
                emit(fail(registreringId, dokumentId, DokumentStatus.CONVERSION_FAILED))
                return
            }
            emit(
                dokumentStateService.setDone(
                    registreringId = registreringId,
                    dokumentId = dokumentId,
                    size = state.size,
                    contentType = state.contentType,
                )
            )
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
 *
 * A document that has reached a terminal status is never written again: two requests can end up
 * driving the same document, and without this a slow one could overwrite the result of a fast one and
 * move the document backwards out of its terminal status. Every write therefore checks the status it
 * is about to replace and returns the current state untouched instead of overwriting a finished
 * document, which makes the write a compare-and-set rather than a blind write.
 *
 * The check has to live here rather than in the caller: [getRegistrering] takes a pessimistic lock on
 * the registrering row, and since these methods run in their own transaction, the status is read and
 * written while that lock is held. Doing the same check in the caller would read in one transaction
 * and write in another, and two requests could both pass it before either of them wrote.
 *
 * The reset endpoint deliberately moves a document backwards out of a terminal status, but it writes
 * the entity directly rather than going through this service, so it is unaffected.
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

        if (dokument.isFinishedByAnotherRequest()) {
            return dokument.toState()
        }

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

    /** Records a clean scan and the generation it was performed on, moving to VIRUS_SCANNING_DONE. */
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

        if (dokument.isFinishedByAnotherRequest()) {
            return dokument.toState()
        }

        dokument.scannedGeneration = scannedGeneration
        dokument.status = DokumentStatus.VIRUS_SCANNING_DONE
        //The size as scanned. Better than nothing while a conversion is running, and it is replaced
        //with the size of the PDF once the document has been converted.
        if (size != null) {
            dokument.size = size
        }
        //Likewise the content type as scanned, replaced with the PDF content type once converted.
        if (contentType != null) {
            dokument.contentType = contentType
        }
        registrering.modified = LocalDateTime.now()

        return dokument.toState()
    }

    /**
     * Records the result of a finished conversion, moving to CONVERTING_DONE. The size and content
     * type are persisted here so that a resumed request can finish the document without converting
     * it a second time.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun setConverted(
        registreringId: UUID,
        dokumentId: UUID,
        size: Long,
        contentType: String,
    ): DokumentState {
        val registrering = getRegistrering(registreringId)
        val dokument = registrering.findDokument(dokumentId)

        if (dokument.isFinishedByAnotherRequest()) {
            return dokument.toState()
        }

        dokument.size = size
        dokument.contentType = contentType
        dokument.status = DokumentStatus.CONVERTING_DONE
        registrering.modified = LocalDateTime.now()

        return dokument.toState()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun setDone(registreringId: UUID, dokumentId: UUID, size: Long, contentType: String): DokumentState {
        val registrering = getRegistrering(registreringId)
        val dokument = registrering.findDokument(dokumentId)

        if (dokument.isFinishedByAnotherRequest()) {
            return dokument.toState()
        }

        dokument.size = size
        dokument.contentType = contentType
        dokument.status = DokumentStatus.DONE
        registrering.modified = LocalDateTime.now()

        return dokument.toState()
    }

    /**
     * Whether another request has already taken this document to a terminal status, meaning the write
     * we are about to make is stale and the status already stored is the one that counts.
     */
    private fun RegistreringDokument.isFinishedByAnotherRequest(): Boolean = status.isTerminal()

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
