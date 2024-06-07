package no.nav.klage.service

import no.nav.klage.api.controller.view.*
import no.nav.klage.api.controller.view.ExistingAnkebehandling
import no.nav.klage.clients.SakFromKlanke
import no.nav.klage.clients.kabalapi.*
import no.nav.klage.clients.kabalapi.PartView
import no.nav.klage.clients.kabalapi.PartViewWithUtsendingskanal
import no.nav.klage.clients.kabalapi.SvarbrevInput
import no.nav.klage.domain.CreateAnkeInput
import no.nav.klage.domain.CreateKlageInput
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.Ytelse
import no.nav.klage.util.MulighetSource
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class KabalApiService(
    private val kabalApiClient: KabalApiClient,
    private val oppgaveService: OppgaveService,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun mulighetIsDuplicate(fagsystem: Fagsystem, kildereferanse: String, type: Type): Boolean {
        return kabalApiClient.checkBehandlingDuplicateInKabal(
            input = BehandlingIsDuplicateInput(fagsystemId = fagsystem.id, kildereferanse = kildereferanse, typeId = type.id)
        )
    }

    fun oppgaveIsDuplicate(oppgaveId: Long,): Boolean {
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

    fun getAnkemuligheter(input: IdnummerInput): List<Ankemulighet> {
        return kabalApiClient.getAnkemuligheterByIdnummer(input).map {
            Ankemulighet(
                id = it.behandlingId.toString(),
                ytelseId = it.ytelseId,
                hjemmelIdList = it.hjemmelIdList,
                temaId = Ytelse.of(it.ytelseId).toTema().id,
                vedtakDate = it.vedtakDate.toLocalDate(),
                sakenGjelder = it.sakenGjelder.partViewWithUtsendingskanal(),
                klager = it.klager.partViewWithUtsendingskanal(),
                fullmektig = it.fullmektig?.partViewWithUtsendingskanal(),
                fagsakId = it.fagsakId,
                fagsystemId = it.fagsystemId,
                previousSaksbehandler = it.tildeltSaksbehandlerIdent?.let { it1 ->
                    it.tildeltSaksbehandlerNavn?.let { it2 ->
                        PreviousSaksbehandler(
                            navIdent = it1,
                            navn = it2,
                        )
                    }
                },
                sourceId = MulighetSource.KABAL.fagsystem.id,
                typeId = it.typeId,
                sourceOfExistingAnkebehandling = it.sourceOfExistingAnkebehandling.map { existingAnkebehandling ->
                    ExistingAnkebehandling(
                        id = existingAnkebehandling.id,
                        created = existingAnkebehandling.created,
                        completed = existingAnkebehandling.completed,
                    )
                },
            )
        }
    }

    fun createAnkeInKabalFromCompleteInput(input: CreateAnkeInput, sakFromKlanke: SakFromKlanke, frist: LocalDate): UUID {
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
                ankeJournalpostId = input.ankeDocumentJournalpostId,
                mottattNav = input.mottattKlageinstans,
                frist = frist,
                ytelseId = input.ytelseId!!,
                kildereferanse = input.id,
                saksbehandlerIdent = input.saksbehandlerIdent,
                svarbrevInput = input.svarbrevInput?.toKabalModel(),
                oppgaveId = input.oppgaveId,
            )
        ).behandlingId
    }

    fun createAnkeInKabalFromKlagebehandling(input: CreateAnkeInput): UUID {
        return kabalApiClient.createAnkeInKabal(
            CreateAnkeBasedOnKlagebehandlingInput(
                sourceBehandlingId = UUID.fromString(input.id),
                mottattNav = input.mottattKlageinstans,
                frist = input.mottattKlageinstans.plusWeeks(input.fristInWeeks.toLong()),
                klager = input.klager.toOversendtPartId(),
                fullmektig = input.fullmektig.toOversendtPartId(),
                ankeDocumentJournalpostId = input.ankeDocumentJournalpostId,
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
                        handling = SvarbrevInput.Receiver.HandlingEnum.valueOf(receiver.handling.name),
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
        input: CreateKlageInput,
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
                klageJournalpostId = input.klageJournalpostId,
                brukersHenvendelseMottattNav = input.mottattVedtaksinstans,
                sakMottattKa = input.mottattKlageinstans,
                frist = frist,
                ytelseId = input.ytelseId,
                kildereferanse = input.eksternBehandlingId,
                saksbehandlerIdent = input.saksbehandlerIdent,
                oppgaveId = input.oppgaveId,
            )
        ).behandlingId
    }

    fun getUsedJournalpostIdListForPerson(fnr: String): List<String> {
        return kabalApiClient.getUsedJournalpostIdListForPerson(fnr = fnr)
    }

    fun getCompletedBehandling(behandlingId: UUID): CompletedBehandling {
        return kabalApiClient.getCompletedBehandling(behandlingId)
    }
}