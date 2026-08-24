package no.nav.klage.domain.entities

import jakarta.persistence.*
import no.nav.klage.kodeverk.PartIdType
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
    /**
     * The numbers are deliberately spread out with [SORT_INDEX_GAP] between them, so that moving a
     * document only changes the number of that one document: the client picks any number between the new
     * neighbours, and nothing else has to be touched. Only the relative order matters, and the values
     * themselves are never sent anywhere outside Kabin.
     */
    @Column(name = "sort_index")
    var sortIndex: Double = FIRST_SORT_INDEX,
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
        return "RegistreringDokument(id=$id, mellomlagerId='$mellomlagerId', name='$name', size=$size, contentType='$contentType', status=$status, sortIndex=$sortIndex, created=$created)"
    }

    companion object {
        //Must match MAX_NAME_LENGTH in kabal-api, which is where the name ends up as a DokumentUnderArbeid.
        //TODO Dokumentløsninger has a TODO for fixing this. As of now it's actually 200 bytes that's the limit.
        const val MAX_NAME_LENGTH = 196

        /** A fresh set of documents starts here, so there is as much room on either side as possible. */
        const val FIRST_SORT_INDEX = 0.0

        //The range is the safe integer range of a JavaScript number, since that is what the client works
        //with. Numbers outside it cannot be represented exactly there, and would not survive a round trip.
        const val MIN_SORT_INDEX = -9007199254740991.0
        const val MAX_SORT_INDEX = 9007199254740991.0

        /**
         * The distance between two documents that are added one after the other. A tenth of a promille of
         * the range leaves room for ~10000 appended documents before the numbers start closing in on
         * [MAX_SORT_INDEX], and for a very large number of moves in between two documents before the gap
         * runs out of precision.
         */
        const val SORT_INDEX_GAP = MAX_SORT_INDEX / 10000
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
     * Terminal failure: something went wrong that is not the fault of the file itself, such as
     * kabal-file-api failing to convert a file whose type it does support, or the object being
     * replaced in the bucket after it was scanned. Resettable to [UPLOADING_DONE], which re-runs the
     * scan and therefore picks up a fresh generation to convert.
     */
    UNEXPECTED_ERROR,
    ;

    fun isTerminal(): Boolean = this in TERMINAL

    /**
     * Whether the document ended in a terminal state that is not [DONE], meaning it can never be
     * journalført. Such documents must be removed (or, when resettable, retried) before the
     * registrering can be finished, so that nothing the user uploaded is silently dropped.
     */
    fun isFailed(): Boolean = isTerminal() && this != DONE

    /**
     * Whether a step is currently being carried out on the document, meaning some request has
     * committed this status and is now waiting for kabal-file-api. Used to let a second confirm
     * request tag along instead of doing the same work over again.
     */
    fun isInProgress(): Boolean = this in IN_PROGRESS

    /**
     * Whether the reset-status endpoint may put this document back into processing, and the status
     * it should be put back to. Only failures that can plausibly be transient are resettable; a virus
     * and an unsupported file type stay failed no matter how many times they are tried.
     */
    fun resetStatus(): DokumentStatus? = RESET_MAP[this]

    companion object {
        private val TERMINAL = setOf(DONE, VIRUS_FOUND, CONVERSION_FAILED, UNSUPPORTED_TYPE, VIRUS_SCAN_FAILED, UNEXPECTED_ERROR)

        private val IN_PROGRESS = setOf(VIRUS_SCANNING, CONVERTING)

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
    /**
     * Anke received from Trygderetten. Behaves exactly like [UPLOADED_DOCUMENTS], but avsender,
     * inngående kanal and type are given by the source itself and set automatically.
     */
    ANKE,
    ;

    /**
     * Whether the behandling is based on documents uploaded in Kabin instead of an existing journalpost.
     */
    val isBasedOnUploadedDocuments: Boolean
        get() = this != JOURNALPOST

    companion object {
        const val TRYGDERETTEN_ORGNR = "974761084"

        /** Avsender is always Trygderetten when the source is [ANKE], and cannot be changed by the user. */
        val TRYGDERETTEN_AVSENDER = PartId(type = PartIdType.VIRKSOMHET, value = TRYGDERETTEN_ORGNR)
    }
}
