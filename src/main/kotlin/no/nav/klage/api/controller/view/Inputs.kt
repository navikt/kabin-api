package no.nav.klage.api.controller.view

import java.time.LocalDate

data class IdnummerInput(val idnummer: String)

data class SearchPartInput(
    val identifikator: String
)

data class CalculateFristInput(
    val fromDate: LocalDate,
    val fristInWeeks: Int,
)