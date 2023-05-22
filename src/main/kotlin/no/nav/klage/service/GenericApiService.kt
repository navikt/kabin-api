package no.nav.klage.service

import no.nav.klage.api.controller.view.*
import no.nav.klage.api.controller.view.PartView
import no.nav.klage.clients.HandledInKabalInput
import no.nav.klage.clients.KlageFssProxyClient
import no.nav.klage.clients.kabalapi.*
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.Type
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class GenericApiService(
    private val kabalApiClient: KabalApiClient,
    private val fssProxyClient: KlageFssProxyClient
) {

    fun getCompletedKlagebehandlingerByIdnummer(idnummerInput: IdnummerInput): List<CompletedKlagebehandling> {
        return kabalApiClient.getCompletedKlagebehandlingerByIdnummer(idnummerInput)
    }

    fun getCompletedKlagebehandling(klagebehandlingId: UUID): CompletedKlagebehandling {
        return kabalApiClient.getCompletedKlagebehandling(klagebehandlingId)
    }
    
    fun klagemulighetIsDuplicate(fagsystem: Fagsystem, kildereferanse: String): Boolean {
        return kabalApiClient.checkDuplicateInKabal(
            input = IsDuplicateInput(fagsystemId = fagsystem.id, kildereferanse = kildereferanse, typeId = Type.KLAGE.id)
        )
    }

    fun createAnkeInKabal(input: CreateAnkeInput): CreatedBehandlingResponse {
        return kabalApiClient.createAnkeInKabal(
            CreateAnkeBasedOnKlagebehandling(
                klagebehandlingId = input.klagebehandlingId,
                mottattNav = input.mottattKlageinstans,
                fristInWeeks = input.fristInWeeks,
                klager = input.klager.toOversendtPartId(),
                fullmektig = input.fullmektig.toOversendtPartId(),
                ankeDocumentJournalpostId = input.ankeDocumentJournalpostId,
                saksbehandlerIdent = input.saksbehandlerIdent,
            )
        )
    }

    private fun PartId?.toOversendtPartId(): OversendtPartId? {
        return if (this == null) {
            null
        } else {
            if (type == PartView.PartType.FNR) {
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

    fun searchPart(searchPartInput: SearchPartInput): no.nav.klage.clients.kabalapi.PartView {
        return kabalApiClient.searchPart(searchPartInput = searchPartInput)
    }

    fun getCreatedAnkeStatus(mottakId: UUID): CreatedAnkebehandlingStatus {
        return kabalApiClient.getCreatedAnkeStatus(mottakId)
    }

    fun getCreatedKlageStatus(mottakId: UUID): CreatedKlagebehandlingStatus {
        return kabalApiClient.getCreatedKlageStatus(mottakId)
    }

    fun getUsedJournalpostIdListForPerson(fnr: String): List<String> {
        return kabalApiClient.getUsedJournalpostIdListForPerson(fnr = fnr)
    }

    fun createKlage(input: CreateKlageInput): CreatedBehandlingResponse {
        val sakFromKlanke = fssProxyClient.getSak(input.sakId)
        val frist = input.mottattKlageinstans.plusWeeks(input.fristInWeeks.toLong())
        val createdBehandlingResponse = kabalApiClient.createKlageInKabal(
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
                kildereferanse = input.sakId,
                saksbehandlerIdent = input.saksbehandlerIdent
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
