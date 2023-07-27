package no.nav.klage.api.controller.view

import java.time.LocalDate
import java.util.*

data class CreateAnkeInputView(
    val id: String?,
    val sourceId: String,
    val mottattKlageinstans: LocalDate?,
    val fristInWeeks: Int?,
    val klager: PartId?,
    val fullmektig: PartId?,
    val journalpostId: String?,
    val ytelseId: String?,
    val hjemmelId: String?,
    val avsender: PartId?,
    val saksbehandlerIdent: String?
)

data class CreatedBehandlingResponse(
    val mottakId: UUID,
)

data class CreateKlageInputView(
    val id: String?,
    val mottattVedtaksinstans: LocalDate?,
    val mottattKlageinstans: LocalDate?,
    val fristInWeeks: Int?,
    val klager: PartId?,
    val fullmektig: PartId?,
    val journalpostId: String?,
    val ytelseId: String?,
    val hjemmelId: String?,
    val avsender: PartId?,
    val saksbehandlerIdent: String?
)