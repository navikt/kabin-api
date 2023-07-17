package no.nav.klage.api.controller.view

import java.time.LocalDate
import java.util.*

data class Ankemulighet(
    @Deprecated("Use id instead.")
    val behandlingId: UUID?,
    val id: String,
    //where the "mulighet" comes from. Kabal or Infotrygd (or other).
    val sourceId: String,
    val ytelseId: String?,
    val hjemmelId: String?,
    val utfallId: String,
    val temaId: String,
    val vedtakDate: LocalDate,
    val sakenGjelder: PartView,
    val klager: PartView?,
    val fullmektig: PartView?,
    val fagsakId: String,
    val fagsystemId: String,
    val klageBehandlendeEnhet: String,
    val previousSaksbehandler: PreviousSaksbehandler?,
)

data class PreviousSaksbehandler(
    val navIdent: String,
    val navn: String,
)