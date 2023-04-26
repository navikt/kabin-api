package no.nav.klage.api.controller.view

import java.time.LocalDate

data class Klagemulighet(
    val sakId: String,
    val temaId: String,
    val utfall: String,
    val utfallId: String,
    val vedtakDate: LocalDate,
    val fagsakId: String,
    val fagsystemId: String,
    val klageBehandlendeEnhet: String,
    val sakenGjelder: PartView,
)