package no.nav.klage.api.controller.view

import java.time.LocalDate

data class CreateKlageInput(
    val sakId: String,
    val mottattVedtaksinstans: LocalDate,
    val mottattKlageinstans: LocalDate,
    val fristInWeeks: Int,
    val klager: PartId,
    val fullmektig: PartId?,
    val klageJournalpostId: String,
    val ytelseId: String,
    val hjemmelIdList: List<String>,
    val avsender: PartId?,
)

data class CreateKlageInputView(
    val sakId: String?,
    val mottattVedtaksinstans: LocalDate?,
    val mottattKlageinstans: LocalDate?,
    val fristInWeeks: Int?,
    val klager: PartId?,
    val fullmektig: PartId?,
    val klageJournalpostId: String?,
    val journalpostId: String?,
    val ytelseId: String?,
    val hjemmelIdList: List<String>?,
    val avsender: PartId?,
)