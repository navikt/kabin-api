package no.nav.klage.api.controller.view

import no.nav.klage.clients.KabalApiClient
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class CreatedBehandlingStatusView(
    val typeId: String,
    val behandlingId: UUID,
    val ytelseId: String,
    val utfallId: String,
    val vedtakDate: LocalDateTime,
    val sakenGjelder: KabalApiClient.SakenGjelderView,
    val klager: KabalApiClient.KlagerView,
    val fullmektig: KabalApiClient.PartView?,
    val tilknyttedeDokumenter: List<KabalApiClient.TilknyttetDokument>,
    val mottattNav: LocalDate,
    val mottattKlageinstans: LocalDate,
    val frist: LocalDate,
    val fagsakId: String,
    val fagsystemId: String,
    val journalpost: DokumentReferanse,
)