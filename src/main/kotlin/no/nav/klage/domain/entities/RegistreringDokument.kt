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
    var mellomlagerId: String,
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
 */
enum class DokumentStatus {
    /** Initial status. The row exists, but the client has not (successfully) uploaded anything yet. */
    UPLOADING,

    /** The upload has been verified to exist in the bucket. */
    UPLOADED,

    /** The file is being scanned for viruses. */
    VIRUS_SCANNING,

    /** The file is being converted to PDF. Only relevant for files that are not already PDF. */
    CONVERTING,

    /** The file is scanned, converted if necessary, and ready for use. */
    DONE,

    /** Terminal failure: the uploaded file contained a virus and has been deleted from the bucket. */
    VIRUS_FOUND,

    /** Terminal failure: the uploaded file could not be turned into a PDF. */
    CONVERSION_FAILED,
    ;

    fun isTerminal(): Boolean = this in TERMINAL

    companion object {
        private val TERMINAL = setOf(DONE, VIRUS_FOUND, CONVERSION_FAILED)
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
