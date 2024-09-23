package no.nav.klage.service

import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.SakFromKlanke
import no.nav.klage.clients.kabalapi.*
import no.nav.klage.clients.kabalapi.PartView
import no.nav.klage.clients.kabalapi.PartViewWithUtsendingskanal
import no.nav.klage.domain.CreateBehandlingInput
import no.nav.klage.domain.CreateKlageInput
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.TimeUnitType
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.*

@Service
class KabalApiService(
    private val kabalApiClient: KabalApiClient,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun oppgaveIsDuplicate(oppgaveId: Long): Boolean {
        return kabalApiClient.checkOppgaveDuplicateInKabal(
            input = OppgaveIsDuplicateInput(oppgaveId = oppgaveId)
        )
    }

    fun searchPart(searchPartInput: SearchPartInput): PartView {
        return kabalApiClient.searchPart(searchPartInput = searchPartInput)
    }

    fun searchPartWithUtsendingskanal(searchPartInput: SearchPartWithUtsendingskanalInput): PartViewWithUtsendingskanal {
        return kabalApiClient.searchPartWithUtsendingskanal(searchPartInput = searchPartInput)
    }

    fun getAnkemuligheterAsMono(input: IdnummerInput): Mono<List<MulighetFromKabal>> {
        return kabalApiClient.getAnkemuligheterByIdnummer(input)
    }

    fun getOmgjoeringskravmuligheterAsMono(input: IdnummerInput): Mono<List<MulighetFromKabal>> {
        return kabalApiClient.getOmgjoeringskravmuligheterByIdnummer(input)
    }

    fun createAnkeInKabalFromCompleteInput(
        input: CreateBehandlingInput,
        sakFromKlanke: SakFromKlanke,
        frist: LocalDate
    ): UUID {
        return kabalApiClient.createAnkeFromCompleteInputInKabal(
            CreateAnkeBasedOnKabinInput(
                sakenGjelder = OversendtPartId(
                    type = OversendtPartIdType.PERSON,
                    value = sakFromKlanke.fnr
                ),
                klager = input.klager.toOversendtPartId(),
                fullmektig = input.fullmektig.toOversendtPartId(),
                fagsakId = sakFromKlanke.fagsakId,
                //TODO: Tilpass når vi får flere fagsystemer.
                fagsystemId = Fagsystem.IT01.id,
                hjemmelIdList = input.hjemmelIdList,
                forrigeBehandlendeEnhet = sakFromKlanke.enhetsnummer,
                ankeJournalpostId = input.receivedDocumentJournalpostId,
                mottattNav = input.mottattKlageinstans,
                frist = frist,
                ytelseId = input.ytelseId!!,
                kildereferanse = input.currentFagystemTechnicalId,
                saksbehandlerIdent = input.saksbehandlerIdent,
                svarbrevInput = input.svarbrevInput?.toKabalModel(),
                oppgaveId = input.oppgaveId,
            )
        ).behandlingId
    }

    fun createAnkeInKabalFromKlagebehandling(input: CreateBehandlingInput): UUID {
        return kabalApiClient.createBehandlingInKabal(
            CreateBehandlingBasedOnKabalInput(
                typeId = input.typeId,
                sourceBehandlingId = UUID.fromString(input.currentFagystemTechnicalId),
                mottattNav = input.mottattKlageinstans,
                frist = when(input.behandlingstidUnitType) {
                    TimeUnitType.WEEKS -> input.mottattKlageinstans.plusWeeks(input.behandlingstidUnits.toLong())
                    TimeUnitType.MONTHS -> input.mottattKlageinstans.plusMonths(input.behandlingstidUnits.toLong())
                },
                klager = input.klager.toOversendtPartId(),
                fullmektig = input.fullmektig.toOversendtPartId(),
                receivedDocumentJournalpostId = input.receivedDocumentJournalpostId,
                saksbehandlerIdent = input.saksbehandlerIdent,
                svarbrevInput = input.svarbrevInput?.toKabalModel(),
                hjemmelIdList = input.hjemmelIdList,
                oppgaveId = input.oppgaveId,
            )
        ).behandlingId
    }

    private fun SvarbrevWithReceiverInput?.toKabalModel(): SvarbrevInput? {
        return this?.let { svarbrevInput ->
            SvarbrevInput(
                title = svarbrevInput.title,
                receivers = svarbrevInput.receivers.map { receiver ->
                    SvarbrevInput.Receiver(
                        id = receiver.id,
                        handling = SvarbrevInput.Receiver.HandlingEnum.valueOf(receiver.handling!!.name),
                        overriddenAddress = receiver.overriddenAddress?.let { address ->
                            SvarbrevInput.Receiver.AddressInput(
                                adresselinje1 = address.adresselinje1,
                                adresselinje2 = address.adresselinje2,
                                adresselinje3 = address.adresselinje3,
                                landkode = address.landkode,
                                postnummer = address.postnummer,
                            )
                        }
                    )
                },
                fullmektigFritekst = svarbrevInput.fullmektigFritekst,
                customText = svarbrevInput.customText,
                varsletBehandlingstidUnits = svarbrevInput.varsletBehandlingstidUnits,
                varsletBehandlingstidUnitTypeId = svarbrevInput.varsletBehandlingstidUnitTypeId ?: svarbrevInput.varsletBehandlingstidUnitType!!.id,
            )
        }
    }

    fun getCreatedAnkeStatus(behandlingId: UUID): CreatedAnkebehandlingStatus {
        return kabalApiClient.getCreatedAnkeStatus(behandlingId = behandlingId)
    }

    fun getCreatedKlageStatus(behandlingId: UUID): CreatedKlagebehandlingStatus {
        return kabalApiClient.getCreatedKlageStatus(behandlingId = behandlingId)
    }

    fun createKlageInKabalFromCompleteInput(
        input: CreateBehandlingInput,
        sakFromKlanke: SakFromKlanke,
        frist: LocalDate
    ): UUID {
        return kabalApiClient.createKlageInKabal(
            input = CreateKlageBasedOnKabinInput(
                sakenGjelder = OversendtPartId(
                    type = OversendtPartIdType.PERSON,
                    value = sakFromKlanke.fnr
                ),
                klager = input.klager.toOversendtPartId(),
                fullmektig = input.fullmektig.toOversendtPartId(),
                fagsakId = sakFromKlanke.fagsakId,
                //TODO: Tilpass når vi får flere fagsystemer.
                fagsystemId = Fagsystem.IT01.id,
                hjemmelIdList = input.hjemmelIdList,
                forrigeBehandlendeEnhet = sakFromKlanke.enhetsnummer,
                klageJournalpostId = input.receivedDocumentJournalpostId,
                brukersHenvendelseMottattNav = input.mottattVedtaksinstans!!,
                sakMottattKa = input.mottattKlageinstans,
                frist = frist,
                ytelseId = input.ytelseId,
                kildereferanse = input.currentFagystemTechnicalId,
                saksbehandlerIdent = input.saksbehandlerIdent,
                oppgaveId = input.oppgaveId,
                svarbrevInput = input.svarbrevInput?.toKabalModel(),
            )
        ).behandlingId
    }

    fun getUsedJournalpostIdListForPerson(fnr: String): List<String> {
        return kabalApiClient.getUsedJournalpostIdListForPerson(fnr = fnr)
    }

    fun getCompletedBehandling(behandlingId: UUID): CompletedBehandling {
        return kabalApiClient.getCompletedBehandling(behandlingId)
    }

    fun getSvarbrevSettings(ytelseId: String, typeId: String): SvarbrevSettingsView {
        return kabalApiClient.getSvarbrevSettings(ytelseId = ytelseId, typeId = typeId)
    }

}