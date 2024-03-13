package no.nav.klage.api.controller.view

import no.nav.klage.clients.kabalapi.OversendtPartId
import no.nav.klage.clients.kabalapi.OversendtPartIdType
import java.time.LocalDate

data class IdnummerInput(val idnummer: String)

data class SearchPartInput(
    val identifikator: String
)

data class CalculateFristInput(
    val fromDate: LocalDate,
    val fristInWeeks: Int,
)

data class WillCreateNewJournalpostInput(
    val journalpostId: String,
    val fagsakId: String,
    val fagsystemId: String,
)

data class SearchUsedJournalpostIdInput(
    val fnr: String,
)

data class PartId(
    val type: PartView.PartType,
    val id: String,
)

fun PartId?.toOversendtPartId(): OversendtPartId? {
    return if (this == null) {
        null
    } else {
        if (type == PartView.PartType.FNR) {
            OversendtPartId(
                type = OversendtPartIdType.PERSON,
                value = this.id
            )
        } else {
            OversendtPartId(
                type = OversendtPartIdType.VIRKSOMHET,
                value = this.id
            )
        }
    }
}

