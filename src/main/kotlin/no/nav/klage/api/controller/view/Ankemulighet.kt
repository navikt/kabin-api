package no.nav.klage.api.controller.view

import no.nav.klage.clients.KabalApiClient
import no.nav.klage.kodeverk.Fagsystem
import java.time.LocalDateTime
import java.util.*

data class Ankemulighet (
    val behandlingId: UUID,
    val ytelseId: String,
    val utfallId: String,
    val vedtakDate: LocalDateTime,
    val sakenGjelder: KabalApiClient.SakenGjelderView,
    val klager: KabalApiClient.KlagerView,
    val fullmektig: KabalApiClient.PartView?,
    val tilknyttedeDokumenter: List<KabalApiClient.TilknyttetDokument>,
    val sakFagsakId: String,
    val fagsakId: String,
    val sakFagsystem: Fagsystem,
    val fagsystem: Fagsystem,
    val fagsystemId: String,
    val klageBehandlendeEnhet: String,
)