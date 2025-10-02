package no.nav.klage.clients.kabalapi

import java.time.LocalDate
import java.util.*

data class CreateBehandlingBasedOnKabalInput(
    val typeId: String,
    val sourceBehandlingId: UUID,
    val mottattNav: LocalDate,
    val frist: LocalDate,
    val klager: OversendtPartId?,
    val fullmektig: OversendtPartId?,
    val receivedDocumentJournalpostId: String,
    val saksbehandlerIdent: String?,
    val svarbrevInput: SvarbrevInput?,
    val hjemmelIdList: List<String>,
    val gosysOppgaveId: Long?,
)

data class BehandlingIsDuplicateInput(
    val fagsystemId: String,
    val kildereferanse: String,
    val typeId: String
)

data class GosysOppgaveIsDuplicateInput(
    val gosysOppgaveId: Long,
)

data class CreateAnkeBasedOnKabinInput(
    val sakenGjelder: OversendtPartId,
    val klager: OversendtPartId?,
    val fullmektig: OversendtPartId?,
    val fagsakId: String,
    val fagsystemId: String,
    val hjemmelIdList: List<String>,
    val forrigeBehandlendeEnhet: String,
    val ankeJournalpostId: String,
    val mottattNav: LocalDate,
    val frist: LocalDate,
    val ytelseId: String,
    val kildereferanse: String,
    val saksbehandlerIdent: String?,
    val svarbrevInput: SvarbrevInput?,
    val gosysOppgaveId: Long,
)

data class CreateBehandlingBasedOnJournalpostInput(
    val typeId: String,
    val sakenGjelder: OversendtPartId,
    val klager: OversendtPartId?,
    val fullmektig: OversendtPartId?,
    val fagsakId: String,
    val fagsystemId: String,
    val hjemmelIdList: List<String>,
    val forrigeBehandlendeEnhet: String,
    val receivedDocumentJournalpostId: String,
    val mottattNav: LocalDate,
    val frist: LocalDate,
    val ytelseId: String,
    val kildereferanse: String,
    val saksbehandlerIdent: String?,
    val svarbrevInput: SvarbrevInput?,
    val gosysOppgaveId: Long,
)

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
    val gosysOppgaveId: Long,
    val svarbrevInput: SvarbrevInput?,
)

data class SvarbrevInput(
    val title: String,
    val receivers: List<Receiver>,
    val fullmektigFritekst: String?,
    val customText: String?,
    val initialCustomText: String?,
    val varsletBehandlingstidUnits: Int,
    val varsletBehandlingstidUnitTypeId: String,

    ) {
    data class Receiver(
        val identifikator: String,
        val handling: HandlingEnum,
        val overriddenAddress: AddressInput?,
    ) {
        data class AddressInput(
            val adresselinje1: String?,
            val adresselinje2: String?,
            val adresselinje3: String?,
            val landkode: String,
            val postnummer: String?,
        )

        enum class HandlingEnum {
            AUTO,
            LOCAL_PRINT,
            CENTRAL_PRINT
        }
    }
}