package no.nav.klage.api.controller.view

import no.nav.klage.kodeverk.Fagsystem
import java.time.LocalDate

data class Klagemulighet(
    val sakId: String,
    val tema: String,
    val utfall: String,
    val vedtakDate: LocalDate,
    val sakFagsakId: String,
    val sakFagsystem: Fagsystem,
    val klageBehandlendeEnhet: String,
)