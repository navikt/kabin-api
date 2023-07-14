package no.nav.klage.api.controller.view

import java.time.LocalDate

data class CreateKlageInput(
    val eksternBehandlingId: String,
    val mottattVedtaksinstans: LocalDate,
    val mottattKlageinstans: LocalDate,
    val fristInWeeks: Int,
    val klager: PartId,
    val fullmektig: PartId?,
    val klageJournalpostId: String,
    val ytelseId: String,
    val hjemmelId: String,
    val avsender: PartId?,
    val saksbehandlerIdent: String?,
)

data class CreateKlageInputView(
    val behandlingId: String?,
    val eksternBehandlingId: String?,
    val mottattVedtaksinstans: LocalDate?,
    val mottattKlageinstans: LocalDate?,
    val fristInWeeks: Int?,
    val klager: PartId?,
    val fullmektig: PartId?,
    val journalpostId: String?,
    val ytelseId: String?,
    val hjemmelId: String?,
    //TODO: Remove after FE change
    val hjemmelIdList: List<String>?,
    val avsender: PartId?,
    val saksbehandlerIdent: String?
)