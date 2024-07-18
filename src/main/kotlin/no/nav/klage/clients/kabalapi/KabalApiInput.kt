package no.nav.klage.clients.kabalapi

import no.nav.klage.domain.BehandlingstidUnitType
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
    val svarbrevInput: SvarbrevInput?,
    val hjemmelIdList: List<String>,
    val oppgaveId: Long?,
)

data class BehandlingIsDuplicateInput(
    val fagsystemId: String,
    val kildereferanse: String,
    val typeId: String
)

data class OppgaveIsDuplicateInput(
    val oppgaveId: Long,
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
    val oppgaveId: Long?,
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
    val oppgaveId: Long?,
    val svarbrevInput: SvarbrevInput?,
)

data class SvarbrevInput(
    val title: String,
    val receivers: List<Receiver>,
    val fullmektigFritekst: String?,
    val customText: String?,
    val varsletBehandlingstidUnits: Int,
    val varsletBehandlingstidUnitType: BehandlingstidUnitType,

    ) {
    data class Receiver(
        val id: String,
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