package no.nav.klage.service

import no.nav.klage.api.controller.view.*
import no.nav.klage.api.controller.view.PartView
import no.nav.klage.clients.HandledInKabalInput
import no.nav.klage.clients.KlageFssProxyClient
import no.nav.klage.clients.KlankeSearchInput
import no.nav.klage.clients.kabalapi.*
import no.nav.klage.kodeverk.*
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class GenericApiService(
    private val kabalApiClient: KabalApiClient,
    private val fssProxyClient: KlageFssProxyClient
) {
    fun getAnkemuligheter(input: IdnummerInput): List<Ankemulighet> {
        val ankemuligheterFromInfotrygd = fssProxyClient.searchKlanke(KlankeSearchInput(fnr = input.idnummer, sakstype = "ANKE"))
            .filter {
                !mulighetIsDuplicate(
                    fagsystem = Fagsystem.IT01,
                    kildereferanse = it.sakId,
                    type = Type.ANKE,
                )
            }
            .map {
                Ankemulighet(
                    behandlingId = null,
                    ytelseId = null,
                    utfallId = infotrygdKlageutfallToUtfall[it.utfall]!!.id,
                    temaId = Tema.fromNavn(it.tema).id,
                    vedtakDate = it.vedtaksdato,
                    sakenGjelder = searchPart(SearchPartInput(identifikator = it.fnr)).toView(),
                    klager = null,
                    fullmektig = null,
                    fagsakId = it.fagsakId,
                    //TODO: Tilpass når vi får flere fagsystemer.
                    fagsystemId = Fagsystem.IT01.id,
                    klageBehandlendeEnhet = it.enhetsnummer,
                    previousSaksbehandler = null,
                )
            }
        val ankemuligheterFromKabal = getCompletedKlagebehandlingerByIdnummer(input).map {
            Ankemulighet(
                behandlingId = it.behandlingId,
                ytelseId = it.ytelseId,
                utfallId = it.utfallId,
                temaId = Ytelse.of(it.ytelseId).toTema().id,
                vedtakDate = it.vedtakDate.toLocalDate(),
                sakenGjelder = it.sakenGjelder.toView(),
                klager = it.klager.toView(),
                fullmektig = it.fullmektig?.toView(),
                fagsakId = it.fagsakId,
                fagsystemId = it.fagsystemId,
                klageBehandlendeEnhet = it.klageBehandlendeEnhet,
                previousSaksbehandler = it.tildeltSaksbehandlerIdent?.let { it1 ->
                    it.tildeltSaksbehandlerNavn?.let { it2 ->
                        PreviousSaksbehandler(
                            navIdent = it1,
                            navn = it2,
                        )
                    }
                },
            )
        }

        return ankemuligheterFromInfotrygd + ankemuligheterFromKabal
    }



    fun getCompletedKlagebehandlingerByIdnummer(idnummerInput: IdnummerInput): List<CompletedKlagebehandling> {
        return kabalApiClient.getCompletedKlagebehandlingerByIdnummer(idnummerInput)
    }

    fun getCompletedKlagebehandling(klagebehandlingId: UUID): CompletedKlagebehandling {
        return kabalApiClient.getCompletedKlagebehandling(klagebehandlingId)
    }
    
    fun mulighetIsDuplicate(fagsystem: Fagsystem, kildereferanse: String, type: Type): Boolean {
        return kabalApiClient.checkDuplicateInKabal(
            input = IsDuplicateInput(fagsystemId = fagsystem.id, kildereferanse = kildereferanse, typeId = type.id)
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
