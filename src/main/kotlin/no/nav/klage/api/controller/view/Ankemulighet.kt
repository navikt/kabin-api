package no.nav.klage.api.controller.view

import no.nav.klage.clients.kabalapi.TilknyttetDokument
import no.nav.klage.kodeverk.Fagsystem
import java.time.LocalDateTime
import java.util.*

data class Ankemulighet(
    val behandlingId: UUID,
    val ytelseId: String,
    val utfallId: String,
    val vedtakDate: LocalDateTime,
    val sakenGjelder: PartView,
    val klager: PartView,
    val fullmektig: PartView?,
    val tilknyttedeDokumenter: List<TilknyttetDokument>,
    val fagsakId: String,
    val fagsystemId: String,
    val klageBehandlendeEnhet: String,
)