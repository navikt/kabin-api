package no.nav.klage.api.controller.view

import java.time.LocalDate

data class CreateKlageInput(
    val sakId: String,
    val mottattVedtaksinstans: LocalDate,
    val mottattKlageinstans: LocalDate,
    val fristInWeeks: Int,
    val klager: PartId?,
    val fullmektig: PartId?,
    val klageJournalpostId: String,
    val hjemmelIdList: List<String>,
    val ytelseId: String,
    val avsender: PartId?,
)