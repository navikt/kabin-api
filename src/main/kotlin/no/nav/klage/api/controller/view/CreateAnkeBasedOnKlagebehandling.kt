package no.nav.klage.api.controller.view

import no.nav.klage.kodeverk.Fagsystem
import java.time.LocalDate
import java.util.*

data class CreateAnkeBasedOnKlagebehandling(
    val klagebehandlingId: UUID,
    val mottattNav: LocalDate,
    val klager: OversendtKlager?,
    val prosessfullmektig: OversendtProsessfullmektig?,
    val ankeDocumentJournalpostId: String,
    val sakFagsakId: String,
    val sakFagsystem: Fagsystem
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