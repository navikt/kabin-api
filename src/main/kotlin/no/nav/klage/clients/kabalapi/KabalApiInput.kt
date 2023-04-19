package no.nav.klage.clients.kabalapi

import java.time.LocalDate
import java.util.*

data class CreateAnkeBasedOnKlagebehandling(
    val klagebehandlingId: UUID,
    val mottattNav: LocalDate,
    val fristInWeeks: Int,
    val klager: OversendtPartId?,
    val fullmektig: OversendtPartId?,
    val ankeDocumentJournalpostId: String,
)
