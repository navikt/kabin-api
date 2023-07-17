package no.nav.klage.service

import no.nav.klage.api.controller.view.*
import no.nav.klage.api.controller.view.PartView
import no.nav.klage.clients.HandledInKabalInput
import no.nav.klage.clients.KlageFssProxyClient
import no.nav.klage.clients.KlankeSearchInput
import no.nav.klage.clients.kabalapi.*
import no.nav.klage.kodeverk.*
import no.nav.klage.util.AnkemulighetSource
import no.nav.klage.util.utfallWithoutAnkemulighet
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class GenericApiService(
    private val kabalApiClient: KabalApiClient,
    private val fssProxyClient: KlageFssProxyClient,
    @Value("\${FETCH_ANKEMULIGHETER_FROM_INFOTRYGD}")
    private val toggleFetchFromInfotrygd: Boolean
) {
    fun getAnkemuligheter(input: IdnummerInput): List<Ankemulighet> {
        val ankemuligheterFromInfotrygd =
            if (toggleFetchFromInfotrygd && fssProxyClient.checkAccess().access) {
            fssProxyClient.searchKlanke(KlankeSearchInput(fnr = input.idnummer, sakstype = "ANKE"))
                .filter {
                    val utfallInSak = infotrygdKlageutfallToUtfall[it.utfall]
                    utfallInSak !in utfallWithoutAnkemulighet
                }
                .filter {
                    !mulighetIsDuplicate(
                        fagsystem = Fagsystem.IT01,
                        kildereferanse = it.sakId,
                        type = Type.ANKE,
                    )
                }
                .map {
                    Ankemulighet(
                        id = it.sakId,
                        ytelseId = null,
                        hjemmelId = null,
                        utfallId = infotrygdKlageutfallToUtfall[it.utfall]!!.id,
                        temaId = Tema.fromNavn(it.tema).id,
                        vedtakDate = it.vedtaksdato,
                        sakenGjelder = searchPart(SearchPartInput(identifikator = it.fnr)).toView(),
                        klager = null,
                        fullmektig = null,
                        fagsakId = it.fagsakId,
                        //TODO: Tilpass når vi får flere fagsystemer.
                        fagsystemId = Fagsystem.IT01.id,
                        previousSaksbehandler = null,
                        sourceId = AnkemulighetSource.INFOTRYGD.fagsystem.id
                    )
                }
            } else emptyList()

        val ankemuligheterFromKabal = getCompletedKlagebehandlingerByIdnummer(input).map {
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
        return when (input.ankemulighetSource) {
            AnkemulighetSource.INFOTRYGD -> createAnkeInKabalFromCompleteInput(input)
            AnkemulighetSource.KABAL -> createAnkeInKabalFromKlagebehandling(input)
        }
    }

    private fun createAnkeInKabalFromCompleteInput(input: CreateAnkeInput): CreatedBehandlingResponse {
        val sakFromKlanke = fssProxyClient.getSak(input.id)
        val frist = input.mottattKlageinstans.plusWeeks(input.fristInWeeks.toLong())
        val createdBehandlingResponse = kabalApiClient.createAnkeFromCompleteInputInKabal(
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
        )

        fssProxyClient.setToHandledInKabal(
            sakFromKlanke.sakId, HandledInKabalInput(
                fristAsString = frist.format(DateTimeFormatter.BASIC_ISO_DATE)
            )
        )

        return createdBehandlingResponse
    }

    private fun createAnkeInKabalFromKlagebehandling(input: CreateAnkeInput): CreatedBehandlingResponse {
        return kabalApiClient.createAnkeInKabal(
            CreateAnkeBasedOnKlagebehandlingInput(
                klagebehandlingId = UUID.fromString(input.id),
                mottattNav = input.mottattKlageinstans,
                frist = input.mottattKlageinstans.plusWeeks(input.fristInWeeks.toLong()),
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
        val sakFromKlanke = fssProxyClient.getSak(input.eksternBehandlingId)
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
        )

        fssProxyClient.setToHandledInKabal(
            sakFromKlanke.sakId, HandledInKabalInput(
                fristAsString = frist.format(DateTimeFormatter.BASIC_ISO_DATE)
            )
        )

        return createdBehandlingResponse
    }
}
