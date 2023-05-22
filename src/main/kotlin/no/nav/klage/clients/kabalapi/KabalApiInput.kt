package no.nav.klage.clients.kabalapi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.util.*

data class CreateAnkeBasedOnKlagebehandling(
    val klagebehandlingId: UUID,
    val mottattNav: LocalDate,
    val fristInWeeks: Int,
    val klager: OversendtPartId?,
    val fullmektig: OversendtPartId?,
    val ankeDocumentJournalpostId: String,
    val saksbehandlerIdent: String?,
)

data class IsDuplicateInput(
    val fagsystemId: String,
    val kildereferanse: String,
    val typeId: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateKlageBasedOnKabinInput(
    val sakenGjelder: OversendtPartId,
    val klager: OversendtPartId?,
    val fullmektig: OversendtPartId?,
    val fagsakId: String,
    val fagsystemId: String,
    val hjemmelIdList: List<String>,
    val forrigeBehandlendeEnhet: String,
    val klageJournalpostId: String,
    val brukersHenvendelseMottattNav: LocalDate,
    val sakMottattKa: LocalDate,
    val frist: LocalDate,
    val ytelseId: String,
    val kildereferanse: String,
    val saksbehandlerIdent: String?,
)