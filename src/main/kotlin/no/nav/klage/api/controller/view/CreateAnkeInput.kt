package no.nav.klage.api.controller.view

import no.nav.klage.util.AnkemulighetSource
import java.time.LocalDate
import java.util.*

data class CreateAnkeInput(
    val id: String,
    val ankemulighetSource: AnkemulighetSource,
    val mottattKlageinstans: LocalDate,
    val fristInWeeks: Int,
    val klager: PartId,
    val fullmektig: PartId?,
    val ankeDocumentJournalpostId: String,
    val ytelseId: String?,
    val hjemmelId: String?,
    val avsender: PartId?,
    val saksbehandlerIdent: String?
)

data class CreateAnkeInputView(
    @Deprecated("use id instead")
    val behandlingId: UUID?,
    val id: String?,
    val sourceId: String,
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
