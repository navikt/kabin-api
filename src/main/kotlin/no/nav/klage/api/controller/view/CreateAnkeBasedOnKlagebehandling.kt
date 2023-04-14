package no.nav.klage.api.controller.view

import java.time.LocalDate
import java.util.*

//TODO rename and remover compatible dates later
data class CreateAnkeBasedOnKlagebehandlingIntern(
    val klagebehandlingId: UUID,
    // new name
    val mottattKlageinstans: LocalDate?,
    // old name
    val mottattNav: LocalDate?,
    val fristInWeeks: Int,
    val klager: OversendtPartId?,
    val fullmektig: OversendtPartId?,
    val ankeDocumentJournalpostId: String,
    val avsender: OversendtPartId?,
)

data class CreateAnkeBasedOnKlagebehandling(
    val klagebehandlingId: UUID,
    val mottattNav: LocalDate,
    val fristInWeeks: Int,
    val klager: OversendtPartId?,
    val fullmektig: OversendtPartId?,
    val ankeDocumentJournalpostId: String,
    val avsender: OversendtPartId?,
)