package no.nav.klage.api.controller.view

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class KabalmulighetView(
    override val id: UUID,
    override var currentFagsystemId: String,
    override val temaId: String,
    override val vedtakDate: LocalDate?,
    override val sakenGjelder: PartViewWithUtsendingskanal,
    override val fagsakId: String,
    override var originalFagsystemId: String,
    override val typeId: String,
    val sourceOfExistingBehandlinger: List<ExistingBehandling>,
    val ytelseId: String?,
    val hjemmelIdList: List<String>?,
    val klager: PartViewWithUtsendingskanal?,
    val fullmektig: PartViewWithUtsendingskanal?,
    val previousSaksbehandler: PreviousSaksbehandler?,
): MulighetView

data class ExistingBehandling(
    val id: UUID,
    val created: LocalDateTime,
    val completed: LocalDateTime?,
)

data class PreviousSaksbehandler(
    val navIdent: String,
    val navn: String,
)

data class KlagemulighetView(
    override val id: UUID,
    override val temaId: String,
    override val vedtakDate: LocalDate,
    override val sakenGjelder: PartViewWithUtsendingskanal,
    override val fagsakId: String,
    override val originalFagsystemId: String,
    override val currentFagsystemId: String,
    override val typeId: String,
    val klageBehandlendeEnhet: String,
): MulighetView

interface MulighetView {
    val id: UUID
    val temaId: String
    val vedtakDate: LocalDate?
    val sakenGjelder: PartViewWithUtsendingskanal
    val fagsakId: String
    val originalFagsystemId: String
    val currentFagsystemId: String
    val typeId: String
}

data class MuligheterView(
    val klagemuligheter: List<KlagemulighetView>,
    val ankemuligheter: List<KabalmulighetView>,
    val omgjoeringskravmuligheter: List<KabalmulighetView>,
    val muligheterFetched: LocalDateTime,
)