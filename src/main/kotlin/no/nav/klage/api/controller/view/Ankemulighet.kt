package no.nav.klage.api.controller.view

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
    val fagsakId: String,
    val fagsystemId: String,
    val klageBehandlendeEnhet: String,
    val previousSaksbehandler: PreviousSaksbehandler?,
)

data class PreviousSaksbehandler(
    val ident: String,
    val navn: String,
)