package no.nav.klage.api.controller.view

import no.nav.klage.kodeverk.Fagsystem
import java.time.LocalDate
import java.util.*

data class CreateAnkeBasedOnKlagebehandling(
    val klagebehandlingId: UUID,
    val mottattNav: LocalDate,
    val klager: OversendtPartId?,
    val prosessfullmektig: OversendtPartId?,
    val ankeDocumentJournalpostId: String,
    val sakFagsakId: String,
    val sakFagsystem: Fagsystem
) {
    data class OversendtPartId(
        val type: OversendtPartIdType,
        val value: String
    )

    enum class OversendtPartIdType { PERSON, VIRKSOMHET }
}