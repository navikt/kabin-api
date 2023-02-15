package no.nav.klage.api.controller.view

import java.time.LocalDate
import java.util.*

data class CreateAnkeBasedOnKlagebehandling(
    val klagebehandlingId: UUID,
    val mottattNav: LocalDate,
    val klager: OversendtPartId?,
    val fullmektig: OversendtPartId?,
    val ankeDocumentJournalpostId: String,
) {
    data class OversendtPartId(
        val type: OversendtPartIdType,
        val value: String
    )

    enum class OversendtPartIdType { PERSON, VIRKSOMHET }
}