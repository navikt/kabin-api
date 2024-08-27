package no.nav.klage.domain.entities

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.time.LocalDate
import java.time.LocalDateTime

@Embeddable
class PartStatus(
    @Enumerated(value = EnumType.STRING)
    @Column(name = "status")
    val status: Status,
    @Column(name = "date")
    val date: LocalDate?,
    @Column(name = "created")
    val created: LocalDateTime = LocalDateTime.now(),
) {
    enum class Status {
        DEAD,
        DELETED,
        FORTROLIG,
        STRENGT_FORTROLIG,
        EGEN_ANSATT,
        VERGEMAAL,
        FULLMAKT,
        RESERVERT_I_KRR,
        DELT_ANSVAR,
    }

}