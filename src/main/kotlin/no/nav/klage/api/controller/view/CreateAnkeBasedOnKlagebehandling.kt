package no.nav.klage.api.controller.view

import java.time.LocalDate
import java.util.*

data class CreateAnkeBasedOnKlagebehandling(
    val klagebehandlingId: UUID,
    val mottattNav: LocalDate,
    val klager: OversendtKlager?,
    val prosessfullmektig: OversendtProsessfullmektig?
) {
    data class OversendtKlager(
        val id: OversendtPartId,
    )

    data class OversendtProsessfullmektig(
        val id: OversendtPartId,
    )

    data class OversendtPartId(
        val type: OversendtPartIdType,
        val verdi: String
    )

    enum class OversendtPartIdType { PERSON, VIRKSOMHET }
}