package no.nav.klage.domain.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
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
    @Column(name = "is_hoveddokument")
    var isHoveddokument: Boolean,
    @Column(name = "confirmed")
    var confirmed: Boolean = false,
    @Column(name = "created")
    val created: LocalDateTime = LocalDateTime.now(),
) {
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
        return "RegistreringDokument(id=$id, mellomlagerId='$mellomlagerId', name='$name', size=$size, isHoveddokument=$isHoveddokument, confirmed=$confirmed, created=$created)"
    }

    companion object {
        //Must match MAX_NAME_LENGTH in kabal-api, which is where the name ends up as a DokumentUnderArbeid.
        //TODO Dokumentløsninger has a TODO for fixing this. As of now it's actually 200 bytes that's the limit.
        const val MAX_NAME_LENGTH = 196
    }
}

enum class InngaaendeKanal {
    ALTINN_INNBOKS,
    E_POST,
}
