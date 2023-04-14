package no.nav.klage.service

import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.HandledInKabalInput
import no.nav.klage.clients.KabalApiClient
import no.nav.klage.clients.KlageFssProxyClient
import no.nav.klage.kodeverk.Fagsystem
import org.springframework.stereotype.Service
import java.util.*

@Service
class GenericApiService(
    private val kabalApiClient: KabalApiClient,
    private val fssProxyClient: KlageFssProxyClient
) {

    fun getCompletedKlagebehandlingerByIdnummer(idnummerInput: IdnummerInput): List<KabalApiClient.CompletedKlagebehandling> {
        return kabalApiClient.getCompletedKlagebehandlingerByIdnummer(idnummerInput)
    }

    fun getCompletedKlagebehandling(klagebehandlingId: UUID): KabalApiClient.CompletedKlagebehandling {
        return kabalApiClient.getCompletedKlagebehandling(klagebehandlingId)
    }

    fun createAnkeInKabal(input: CreateAnkeBasedOnKlagebehandling): KabalApiClient.CreatedBehandlingResponse {
        return kabalApiClient.createAnkeInKabal(input)
    }

    fun searchPart(searchPartInput: SearchPartInput): KabalApiClient.PartView {
        return kabalApiClient.searchPart(searchPartInput = searchPartInput)
    }

    fun getCreatedAnkeStatus(mottakId: UUID): KabalApiClient.CreatedBehandlingStatus {
        return kabalApiClient.getCreatedAnkeStatus(mottakId)
    }

    fun getUsedJournalpostIdListForPerson(fnr: String): List<String> {
        return kabalApiClient.getUsedJournalpostIdListForPerson(fnr = fnr)
    }

    fun createKlage(input: CreateKlageInput): KabalApiClient.CreatedBehandlingResponse {
        val sakFromKlanke = fssProxyClient.getSak(input.sakId)
        val createdBehandlingResponse = kabalApiClient.createKlageInKabal(
            input = KabalApiClient.CreateKlageBasedOnKabinInput(
                sakenGjelder = OversendtPartId(
                    type = OversendtPartIdType.PERSON,
                    value = sakFromKlanke.fnr
                ),
                klager = input.klager,
                fullmektig = input.fullmektig,
                fagsakId = sakFromKlanke.fagsakId,
                //TODO: Tilpass når vi får flere fagsystemer.
                fagsystemId = Fagsystem.IT01.id,
                hjemmelIdList = input.hjemmelIdList,
                forrigeBehandlendeEnhet = sakFromKlanke.enhetsnummer,
                klageJournalpostId = input.klageJournalpostId,
                brukersHenvendelseMottattNav = input.mottattNav,
                sakMottattKa = input.mottattKa,
                frist = input.frist,
                ytelseId = input.ytelseId,
                kildereferanse = input.sakId,
            )
        )

        fssProxyClient.setToHandledInKabal(
            sakFromKlanke.sakId, HandledInKabalInput(
                //TODO: Hvilket format?
                fristAsString = input.frist.toString()
            )
        )

        return createdBehandlingResponse
    }
}