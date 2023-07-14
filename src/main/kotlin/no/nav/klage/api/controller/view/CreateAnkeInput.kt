package no.nav.klage.api.controller.view

import java.time.LocalDate
import java.util.*

data class CreateAnkeInput(
    val klagebehandlingId: UUID?,
    val eksternBehandlingId: String?,
    val mottattKlageinstans: LocalDate,
    val fristInWeeks: Int,
    val klager: PartId,
    val fullmektig: PartId?,
    val ankeDocumentJournalpostId: String,
    val ytelseId: String,
    val hjemmelId: String,
    val avsender: PartId?,
    val saksbehandlerIdent: String?
)

data class CreateAnkeInputView(
    val behandlingId: UUID?,
    val eksternBehandlingId: String?,
    val mottattKlageinstans: LocalDate?,
    val fristInWeeks: Int?,
    val klager: PartId?,
    val fullmektig: PartId?,
    val journalpostId: String?,
    val ytelseId: String?,
    val hjemmelId: String?,
    val avsender: PartId?,
    val saksbehandlerIdent: String?
)
