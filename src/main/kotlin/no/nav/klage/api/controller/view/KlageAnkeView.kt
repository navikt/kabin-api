package no.nav.klage.api.controller.view

import no.nav.klage.kodeverk.TimeUnitType
import java.time.LocalDate
import java.util.*

data class CreateAnkeInputView(
    val mottattKlageinstans: LocalDate?,
    val behandlingstidUnits: Int?,
    val behandlingstidUnitType: TimeUnitType?,
    val behandlingstidUnitTypeId: String?,
    val klager: PartId?,
    val fullmektig: PartId?,
    val journalpostId: String?,
    val ytelseId: String?,
    val hjemmelIdList: List<String>,
    val avsender: PartId?,
    val saksbehandlerIdent: String?,
    val svarbrevInput: SvarbrevWithReceiverInput?,
    val vedtak: Vedtak?,
    val oppgaveId: Long?,
)

data class CreatedBehandlingResponse(
    val behandlingId: UUID,
)

data class CreateKlageInputView(
    val mottattVedtaksinstans: LocalDate?,
    val mottattKlageinstans: LocalDate?,
    val behandlingstidUnits: Int?,
    val behandlingstidUnitType: TimeUnitType?,
    val behandlingstidUnitTypeId: String?,
    val klager: PartId?,
    val fullmektig: PartId?,
    val journalpostId: String?,
    val ytelseId: String?,
    val hjemmelIdList: List<String>,
    val avsender: PartId?,
    val saksbehandlerIdent: String?,
    val vedtak: Vedtak?,
    val oppgaveId: Long?,
    val svarbrevInput: SvarbrevWithReceiverInput?,
)

data class Vedtak(
    val id: String,
    val sourceId: String,
)