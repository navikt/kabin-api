package no.nav.klage.domain

import no.nav.klage.api.controller.view.PartIdInput
import no.nav.klage.api.controller.view.SvarbrevWithReceiverInput
import no.nav.klage.kodeverk.TimeUnitType
import no.nav.klage.util.MulighetSource
import java.time.LocalDate

data class CreateAnkeInput(
    val id: String,
    val mulighetSource: MulighetSource,
    val mottattKlageinstans: LocalDate,
    val behandlingstidUnits: Int,
    val behandlingstidUnitType: TimeUnitType,
    val klager: PartIdInput,
    val fullmektig: PartIdInput?,
    val ankeDocumentJournalpostId: String,
    val ytelseId: String?,
    val hjemmelIdList: List<String>,
    val avsender: PartIdInput?,
    val saksbehandlerIdent: String?,
    val svarbrevInput: SvarbrevWithReceiverInput?,
    val oppgaveId: Long?
)

data class CreateKlageInput(
    val eksternBehandlingId: String,
    val mottattVedtaksinstans: LocalDate,
    val mottattKlageinstans: LocalDate,
    val behandlingstidUnits: Int,
    val behandlingstidUnitType: TimeUnitType,
    val klager: PartIdInput,
    val fullmektig: PartIdInput?,
    val klageJournalpostId: String,
    val ytelseId: String,
    val hjemmelIdList: List<String>,
    val avsender: PartIdInput?,
    val saksbehandlerIdent: String?,
    val oppgaveId: Long?,
    val svarbrevInput: SvarbrevWithReceiverInput?,
)
