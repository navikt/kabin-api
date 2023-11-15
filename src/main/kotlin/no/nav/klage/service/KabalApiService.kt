package no.nav.klage.service

import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.SakFromKlanke
import no.nav.klage.clients.kabalapi.*
import no.nav.klage.domain.CreateAnkeInput
import no.nav.klage.domain.CreateKlageInput
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.Ytelse
import no.nav.klage.util.AnkemulighetSource
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class KabalApiService(
    private val kabalApiClient: KabalApiClient,
) {
    fun mulighetIsDuplicate(fagsystem: Fagsystem, kildereferanse: String, type: Type): Boolean {
        return kabalApiClient.checkDuplicateInKabal(
            input = IsDuplicateInput(fagsystemId = fagsystem.id, kildereferanse = kildereferanse, typeId = type.id)
        )
    }

    fun searchPart(searchPartInput: SearchPartInput): no.nav.klage.clients.kabalapi.PartView {
        return kabalApiClient.searchPart(searchPartInput = searchPartInput)
    }

    fun getAnkemuligheter(input: IdnummerInput): List<Ankemulighet> {
        return kabalApiClient.getAnkemuligheterByIdnummer(input).map {
            Ankemulighet(
                id = it.behandlingId.toString(),
                ytelseId = it.ytelseId,
                hjemmelId = it.hjemmelId,
                utfallId = it.utfallId,
                temaId = Ytelse.of(it.ytelseId).toTema().id,
                vedtakDate = it.vedtakDate.toLocalDate(),
                sakenGjelder = it.sakenGjelder.toView(),
                klager = it.klager.toView(),
                fullmektig = it.fullmektig?.toView(),
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
                sourceId = AnkemulighetSource.KABAL.fagsystem.id,
                typeId = it.typeId,
                sourceOfAnkebehandlingWithId = it.sourceOfAnkebehandlingWithId,
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
                hjemmelId = input.hjemmelId!!,
                forrigeBehandlendeEnhet = sakFromKlanke.enhetsnummer,
                ankeJournalpostId = input.ankeDocumentJournalpostId,
                mottattNav = input.mottattKlageinstans,
                frist = frist,
                ytelseId = input.ytelseId!!,
                kildereferanse = input.id,
                saksbehandlerIdent = input.saksbehandlerIdent,
            )
        ).mottakId
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
            )
        ).mottakId
    }

    fun getCreatedAnkeStatus(mottakId: UUID): CreatedAnkebehandlingStatus {
        return kabalApiClient.getCreatedAnkeStatus(mottakId = mottakId)
    }

    fun getCreatedKlageStatus(mottakId: UUID): CreatedKlagebehandlingStatus {
        return kabalApiClient.getCreatedKlageStatus(mottakId = mottakId)
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
                hjemmelId = input.hjemmelId,
                forrigeBehandlendeEnhet = sakFromKlanke.enhetsnummer,
                klageJournalpostId = input.klageJournalpostId,
                brukersHenvendelseMottattNav = input.mottattVedtaksinstans,
                sakMottattKa = input.mottattKlageinstans,
                frist = frist,
                ytelseId = input.ytelseId,
                kildereferanse = input.eksternBehandlingId,
                saksbehandlerIdent = input.saksbehandlerIdent,
            )
        ).mottakId
    }

    fun getUsedJournalpostIdListForPerson(fnr: String): List<String> {
        return kabalApiClient.getUsedJournalpostIdListForPerson(fnr = fnr)
    }

    fun getCompletedBehandling(behandlingId: UUID): CompletedBehandling {
        return kabalApiClient.getCompletedBehandling(behandlingId)
    }
}