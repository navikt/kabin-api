package no.nav.klage.domain.entities

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "registrering_dokument", schema = "klage")
class RegistreringDokument(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "mellomlager_id")
    var mellomlagerId: String?,
    @Column(name = "name")
    var name: String,
    @Column(name = "size")
    var size: Long,
    @Column(name = "content_type")
    var contentType: String,
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: DokumentStatus = DokumentStatus.UPLOADING,
    @Column(name = "scanned_generation")
    var scannedGeneration: Long? = null,
    @Column(name = "created")
    val created: LocalDateTime = LocalDateTime.now(),
) {
    val isDone: Boolean
        get() = status == DokumentStatus.DONE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RegistreringDokument
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "RegistreringDokument(id=$id, mellomlagerId='$mellomlagerId', name='$name', size=$size, contentType='$contentType', status=$status, created=$created)"
    }

    companion object {
        //Must match MAX_NAME_LENGTH in kabal-api, which is where the name ends up as a DokumentUnderArbeid.
        //TODO Dokumentløsninger has a TODO for fixing this. As of now it's actually 200 bytes that's the limit.
        const val MAX_NAME_LENGTH = 196
    }
}

/**
 * Lifecycle of an uploaded document. Stored and exposed to clients as the enum name, both in JSON and
 * in the data of the `status` events in the confirm stream.
 *
 * The processing statuses come in pairs: a `X` status while a step is running, and an `X_DONE` status
 * once it has finished. The `X_DONE` statuses are the points processing can be picked up from, both
 * when a confirm request is resumed and when a failed document is reset, so that a step that has
 * already succeeded is never redone.
 */
enum class DokumentStatus {
    /** Initial status. The row exists, but the client has not (successfully) uploaded anything yet. */
    UPLOADING,

    /** The upload has been verified to exist in the bucket. Nothing has been done with it yet. */
    UPLOADING_DONE,

    /** The file is being scanned for viruses. */
    VIRUS_SCANNING,

    /** The file has been scanned and is clean. [RegistreringDokument.scannedGeneration] holds the
     * generation that was scanned, so a later conversion can verify it converts those exact bytes. */
    VIRUS_SCANNING_DONE,

    /** The file is being converted to PDF. Only relevant for files that are not already PDF. */
    CONVERTING,

    /** The file has been converted to PDF, and the size and content type of the PDF are recorded. */
    CONVERTING_DONE,

    /** The file is scanned, converted if necessary, and ready for use. */
    DONE,

    /** Terminal failure: the uploaded file contained a virus and has been deleted from the bucket. */
    VIRUS_FOUND,

    /** Terminal failure: the uploaded file could not be turned into a PDF, including when the
     * conversion itself failed unexpectedly. Resettable, since the failure may be transient. */
    CONVERSION_FAILED,

    /** Terminal failure: the virus scanning service failed unexpectedly. */
    VIRUS_SCAN_FAILED,

    /** Terminal failure: the requested content type is not supported for upload. */
    UNSUPPORTED_TYPE,

    /**
     * Terminal failure: an unexpected error occurred during processing. No longer set; conversion
     * failures are reported as [CONVERSION_FAILED] and scan failures as [VIRUS_SCAN_FAILED]. Kept
     * because documents stored before that change can still have this status.
     */
    UNEXPECTED_ERROR,
    ;

    fun isTerminal(): Boolean = this in TERMINAL

    /**
     * Whether the reset-status endpoint may put this document back into processing, and the status
     * it should be put back to. Only failures that can plausibly be transient are resettable; a virus
     * and an unsupported file type stay failed no matter how many times they are tried.
     */
    fun resetStatus(): DokumentStatus? = RESET_MAP[this]

    companion object {
        private val TERMINAL = setOf(DONE, VIRUS_FOUND, CONVERSION_FAILED, UNSUPPORTED_TYPE, VIRUS_SCAN_FAILED, UNEXPECTED_ERROR)

        private val RESET_MAP = mapOf(
            VIRUS_SCAN_FAILED to UPLOADING_DONE,
            UNEXPECTED_ERROR to UPLOADING_DONE,
            CONVERSION_FAILED to VIRUS_SCANNING_DONE,
        )
    }
}

enum class InngaaendeKanal {
    ALTINN_INNBOKS,
    E_POST,
}

enum class RegistreringSource {
    JOURNALPOST,
    UPLOADED_DOCUMENTS,
}
