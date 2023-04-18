package no.nav.klage.service

import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.HandledInKabalInput
import no.nav.klage.clients.KabalApiClient
import no.nav.klage.clients.KlageFssProxyClient
import no.nav.klage.kodeverk.Fagsystem
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

    fun createAnkeInKabal(input: CreateAnkeBasedOnKlagebehandlingView): KabalApiClient.CreatedBehandlingResponse {
        return kabalApiClient.createAnkeInKabal(KabalApiClient.CreateAnkeBasedOnKlagebehandling(
            klagebehandlingId = input.klagebehandlingId,
            mottattNav = input.mottattKlageinstans,
            fristInWeeks = input.fristInWeeks,
            klager = input.klager.toOversendtPartId(),
            fullmektig = input.fullmektig.toOversendtPartId(),
            ankeDocumentJournalpostId = input.ankeDocumentJournalpostId,
        ))
    }

    private fun PartId?.toOversendtPartId(): KabalApiClient.OversendtPartId? {
        return if (this == null) {
            null
        } else {
            if (type == PartView.PartType.FNR) {
                KabalApiClient.OversendtPartId(
                    type = KabalApiClient.OversendtPartIdType.PERSON,
                    value = this.id
                )
            } else {
                KabalApiClient.OversendtPartId(
                    type = KabalApiClient.OversendtPartIdType.VIRKSOMHET,
                    value = this.id
                )
            }
        }
    }

    fun searchPart(searchPartInput: SearchPartInput): KabalApiClient.PartView {
        return kabalApiClient.searchPart(searchPartInput = searchPartInput)
    }

    fun getCreatedAnkeStatus(mottakId: UUID): KabalApiClient.CreatedAnkebehandlingStatus {
        return kabalApiClient.getCreatedAnkeStatus(mottakId)
    }

    fun getCreatedKlageStatus(mottakId: UUID): KabalApiClient.CreatedKlagebehandlingStatus {
        return kabalApiClient.getCreatedKlageStatus(mottakId)
    }

    fun getUsedJournalpostIdListForPerson(fnr: String): List<String> {
        return kabalApiClient.getUsedJournalpostIdListForPerson(fnr = fnr)
    }

    fun createKlage(input: CreateKlageInput): KabalApiClient.CreatedBehandlingResponse {
        val sakFromKlanke = fssProxyClient.getSak(input.sakId)
        val frist = LocalDate.now().plusWeeks(input.fristInWeeks.toLong())
        val createdBehandlingResponse = kabalApiClient.createKlageInKabal(
            input = KabalApiClient.CreateKlageBasedOnKabinInput(
                sakenGjelder = KabalApiClient.OversendtPartId(
                    type = KabalApiClient.OversendtPartIdType.PERSON,
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
                kildereferanse = input.sakId,
            )
        )

        fssProxyClient.setToHandledInKabal(
            sakFromKlanke.sakId, HandledInKabalInput(
                fristAsString = frist.format(DateTimeFormatter.BASIC_ISO_DATE)
            )
        )

        return createdBehandlingResponse
    }
}
