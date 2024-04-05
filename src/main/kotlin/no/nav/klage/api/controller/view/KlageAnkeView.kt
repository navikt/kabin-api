package no.nav.klage.api.controller.view

import java.time.LocalDate
import java.util.*

data class CreateAnkeInputView(
    val mottattKlageinstans: LocalDate?,
    val fristInWeeks: Int?,
    val klager: PartId?,
    val fullmektig: PartId?,
    val journalpostId: String?,
    val ytelseId: String?,
    val hjemmelIdList: List<String>?,
    val avsender: PartId?,
    val saksbehandlerIdent: String?,
    val svarbrevInput: SvarbrevWithReceiverInput?,
    val vedtak: Vedtak?,
)

data class CreatedBehandlingResponse(
    val behandlingId: UUID,
)

data class CreateKlageInputView(
    val mottattVedtaksinstans: LocalDate?,
    val mottattKlageinstans: LocalDate?,
    val fristInWeeks: Int?,
    val klager: PartId?,
    val fullmektig: PartId?,
    val journalpostId: String?,
    val ytelseId: String?,
    val hjemmelIdList: List<String>?,
    val avsender: PartId?,
    val saksbehandlerIdent: String?,
    val vedtak: Vedtak?,
)

data class Vedtak(
    val id: String,
    val sakenGjelder: PartId,
    val sourceId: String,
)