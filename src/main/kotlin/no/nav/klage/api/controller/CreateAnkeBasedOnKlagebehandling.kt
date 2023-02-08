package no.nav.klage.api.controller

import java.time.LocalDate
import java.util.*

data class CreateAnkeBasedOnKlagebehandling(
    val klagebehandlingId: UUID,
    val mottattNav: LocalDate,
    val klager: OversendtKlager?,
) {
    data class OversendtKlager(
        val id: OversendtPartId,
        val klagersProsessfullmektig: OversendtProsessfullmektig? = null
    )

    data class OversendtProsessfullmektig(
        val id: OversendtPartId,
        val skalKlagerMottaKopi: Boolean
    )

    data class OversendtPartId(
        val type: OversendtPartIdType,
        val verdi: String
    )

    enum class OversendtPartIdType { PERSON, VIRKSOMHET }
}