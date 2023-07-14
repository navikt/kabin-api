package no.nav.klage.api.controller.view

import java.time.LocalDate

data class Klagemulighet(
    val sakId: String,
    val behandlingId: String,
    val temaId: String,
    val utfallId: String,
    val vedtakDate: LocalDate,
    val sakenGjelder: PartView,
    val fagsakId: String,
    val fagsystemId: String,
    val klageBehandlendeEnhet: String,
)