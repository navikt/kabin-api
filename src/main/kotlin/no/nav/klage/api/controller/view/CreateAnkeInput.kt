package no.nav.klage.api.controller.view

import java.time.LocalDate
import java.util.*

data class CreateAnkeInput(
    val klagebehandlingId: UUID,
    val mottattKlageinstans: LocalDate,
    val fristInWeeks: Int,
    val klager: PartId,
    val fullmektig: PartId?,
    val ankeDocumentJournalpostId: String,
    val avsender: PartId?,
)

data class CreateAnkeInputView(
    val behandlingId: UUID?,
    val mottattKlageinstans: LocalDate?,
    val fristInWeeks: Int?,
    val klager: PartId?,
    val fullmektig: PartId?,
    val journalpostId: String?,
    val avsender: PartId?,
)
