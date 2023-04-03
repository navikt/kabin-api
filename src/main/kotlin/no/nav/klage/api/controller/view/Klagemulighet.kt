package no.nav.klage.api.controller.view

import no.nav.klage.clients.KabalApiClient
import java.time.LocalDate

data class Klagemulighet(
    val sakId: String,
    val temaId: String,
    val utfall: String,
    val vedtakDate: LocalDate,
    val fagsakId: String,
    val fagsystemId: String,
    val klageBehandlendeEnhet: String,
    val sakenGjelder: KabalApiClient.PartView,
)