package no.nav.klage.domain

import no.nav.klage.api.controller.view.PartId
import no.nav.klage.util.AnkemulighetSource
import java.time.LocalDate

data class CreateAnkeInput(
    val id: String,
    val ankemulighetSource: AnkemulighetSource,
    val mottattKlageinstans: LocalDate,
    val fristInWeeks: Int,
    val klager: PartId,
    val fullmektig: PartId?,
    val ankeDocumentJournalpostId: String,
    val ytelseId: String?,
    val hjemmelIdList: List<String>?,
    val avsender: PartId?,
    val saksbehandlerIdent: String?
)

data class CreateKlageInput(
    val eksternBehandlingId: String,
    val mottattVedtaksinstans: LocalDate,
    val mottattKlageinstans: LocalDate,
    val fristInWeeks: Int,
    val klager: PartId,
    val fullmektig: PartId?,
    val klageJournalpostId: String,
    val ytelseId: String,
    val hjemmelIdList: List<String>,
    val avsender: PartId?,
    val saksbehandlerIdent: String?,
)
