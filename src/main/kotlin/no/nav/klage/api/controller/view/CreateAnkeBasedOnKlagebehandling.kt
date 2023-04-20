package no.nav.klage.api.controller.view

import java.time.LocalDate
import java.util.*

data class CreateAnkeBasedOnKlagebehandlingView(
    val klagebehandlingId: UUID,
    val mottattKlageinstans: LocalDate,
    val fristInWeeks: Int,
    val klager: PartId?,
    val fullmektig: PartId?,
    val ankeDocumentJournalpostId: String,
    val avsender: PartId?,
)