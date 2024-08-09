package no.nav.klage.api.controller.view

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class Ankemulighet(
    override val id: String,
    override var currentFagsystemId: String,
    override val temaId: String,
    override val vedtakDate: LocalDate?,
    override val sakenGjelder: PartViewWithUtsendingskanal,
    override val fagsakId: String,
    override var originalFagsystemId: String,
    //where the "mulighet" comes from. Kabal or Infotrygd (or other).
    val sourceId: String,
    val sourceOfExistingAnkebehandling: List<ExistingAnkebehandling>,
    val ytelseId: String?,
    val hjemmelIdList: List<String>?,
    val klager: PartViewWithUtsendingskanal?,
    val fullmektig: PartViewWithUtsendingskanal?,
    val fagsystemId: String,
    val previousSaksbehandler: PreviousSaksbehandler?,
    val typeId: String?,
): Mulighet

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
    override val id: String,
    override val temaId: String,
    override val vedtakDate: LocalDate,
    override val sakenGjelder: PartViewWithUtsendingskanal,
    override val fagsakId: String,
    override val originalFagsystemId: String,
    override val currentFagsystemId: String,
    val klageBehandlendeEnhet: String,
): Mulighet

interface Mulighet {
    val id: String
    val temaId: String
    val vedtakDate: LocalDate?
    val sakenGjelder: PartViewWithUtsendingskanal
    val fagsakId: String
    val originalFagsystemId: String
    val currentFagsystemId: String
}