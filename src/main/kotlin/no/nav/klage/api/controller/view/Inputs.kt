package no.nav.klage.api.controller.view

import no.nav.klage.clients.kabalapi.OversendtPartId
import no.nav.klage.clients.kabalapi.OversendtPartIdType
import no.nav.klage.domain.entities.HandlingEnum
import no.nav.klage.kodeverk.TimeUnitType
import java.time.LocalDate
import java.util.*

data class IdnummerInput(val idnummer: String)

data class SearchPartInput(
    val identifikator: String
)

data class SearchPartWithUtsendingskanalInput(
    val identifikator: String,
    val sakenGjelderId: String,
    val ytelseId: String,
)

data class CalculateFristInput(
    val fromDate: LocalDate,
    val varsletBehandlingstidUnits: Int,
    val varsletBehandlingstidUnitType: TimeUnitType?,
    val varsletBehandlingstidUnitTypeId: String?
)

data class GetOppgaveListInput(
    val identifikator: String,
    val temaId: String?,
)

data class SearchUsedJournalpostIdInput(
    val fnr: String,
)

data class PartIdInput(
    val type: PartType,
    val id: String,
)

fun PartIdInput?.toOversendtPartId(): OversendtPartId? {
    return if (this == null) {
        null
    } else {
        if (type == PartType.FNR) {
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

//////////////////////// new inputs ////////////////////////

data class SakenGjelderValueInput(val sakenGjelderValue: String?)

data class JournalpostIdInput(val journalpostId: String)

data class TypeIdInput(val typeId: String?)

data class MulighetInput(
    val mulighetId: UUID,
)

data class MottattVedtaksinstansInput(val mottattVedtaksinstans: LocalDate)

data class MottattKlageinstansInput(val mottattKlageinstans: LocalDate)

data class BehandlingstidInput(val units: Int, val unitTypeId: String)

data class HjemmelIdListInput(val hjemmelIdList: List<String>)

data class YtelseIdInput(val ytelseId: String?)

data class SaksbehandlerIdentInput(val saksbehandlerIdent: String?)

data class OppgaveIdInput(val oppgaveId: Long?)

data class SendSvarbrevInput(val send: Boolean)

data class SvarbrevFullmektigFritekstInput(val fullmektigFritekst: String?)

data class SvarbrevCustomTextInput(val customText: String)

data class SvarbrevTitleInput(val title: String)

data class SvarbrevRecipientInput(
    val part: PartIdInput,
    val handling: HandlingEnum,
    val overriddenAddress: AddressInput?
)

data class ModifySvarbrevRecipientInput(
    val handling: HandlingEnum,
    val overriddenAddress: AddressInput?
)
data class AddressInput(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val landkode: String?,
    val postnummer: String?,
    val poststed: String?,
)

data class SvarbrevOverrideBehandlingstidInput(
    val overrideBehandlingstid: Boolean,
)

data class SvarbrevOverrideCustomTextInput(
    val overrideCustomText: Boolean,
)

data class FullmektigInput(
    val fullmektig: PartIdInput?,
)

data class AvsenderInput(
    val avsender: PartIdInput?,
)

data class KlagerInput(
    val klager: PartIdInput?,
)