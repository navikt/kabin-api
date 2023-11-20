package no.nav.klage.clients.kabalapi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.util.*

data class CreateAnkeBasedOnKlagebehandlingInput(
    val sourceBehandlingId: UUID,
    val mottattNav: LocalDate,
    val frist: LocalDate,
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
data class CreateAnkeBasedOnKabinInput(
    val sakenGjelder: OversendtPartId,
    val klager: OversendtPartId?,
    val fullmektig: OversendtPartId?,
    val fagsakId: String,
    val fagsystemId: String,
    val hjemmelId: String,
    val forrigeBehandlendeEnhet: String,
    val ankeJournalpostId: String,
    val mottattNav: LocalDate,
    val frist: LocalDate,
    val ytelseId: String,
    val kildereferanse: String,
    val saksbehandlerIdent: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateKlageBasedOnKabinInput(
    val sakenGjelder: OversendtPartId,
    val klager: OversendtPartId?,
    val fullmektig: OversendtPartId?,
    val fagsakId: String,
    val fagsystemId: String,
    val hjemmelId: String,
    val forrigeBehandlendeEnhet: String,
    val klageJournalpostId: String,
    val brukersHenvendelseMottattNav: LocalDate,
    val sakMottattKa: LocalDate,
    val frist: LocalDate,
    val ytelseId: String,
    val kildereferanse: String,
    val saksbehandlerIdent: String?,
)