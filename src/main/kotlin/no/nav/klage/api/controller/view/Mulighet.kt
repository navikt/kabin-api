package no.nav.klage.api.controller.view

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class Ankemulighet(
    val id: String,
    //where the "mulighet" comes from. Kabal or Infotrygd (or other).
    val sourceId: String,
    val sourceOfExistingAnkebehandling: List<ExistingAnkebehandling>,
    val ytelseId: String?,
    val hjemmelIdList: List<String>?,
    val temaId: String,
    val vedtakDate: LocalDate?,
    val sakenGjelder: PartViewWithUtsendingskanal,
    val klager: PartViewWithUtsendingskanal?,
    val fullmektig: PartViewWithUtsendingskanal?,
    val fagsakId: String,
    val fagsystemId: String,
    val previousSaksbehandler: PreviousSaksbehandler?,
    val typeId: String?,
)

data class ExistingAnkebehandling(
    val id: UUID,
    val created: LocalDateTime,
    val completed: LocalDateTime?,
)

data class PreviousSaksbehandler(
    val navIdent: String,
    val navn: String,
)

data class Klagemulighet(
    val id: String,
    val temaId: String,
    val vedtakDate: LocalDate,
    val sakenGjelder: PartViewWithUtsendingskanal,
    val fagsakId: String,
    val fagsystemId: String,
    val klageBehandlendeEnhet: String,
    val sourceId: String,
)