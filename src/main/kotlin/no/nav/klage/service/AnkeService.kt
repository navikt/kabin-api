package no.nav.klage.service

import no.nav.klage.api.controller.mapper.toView
import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.kabalapi.toView
import no.nav.klage.domain.CreateAnkeInput
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.Tema
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.Ytelse
import no.nav.klage.util.AnkemulighetSource
import no.nav.klage.util.ValidationUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.stereotype.Service
import java.util.*

@Service
class AnkeService(
    private val validationUtil: ValidationUtil,
    private val dokArkivService: DokArkivService,
    private val klageFssProxyService: KlageFssProxyService,
    private val kabalApiService: KabalApiService,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    fun createAnke(input: CreateAnkeInputView): CreatedBehandlingResponse {
        val processedInput = validationUtil.validateCreateAnkeInputView(input)
        val journalpostId = dokArkivService.handleJournalpostBasedOnAnkeInput(processedInput)
        val finalInput = processedInput.copy(ankeDocumentJournalpostId = journalpostId)

        return CreatedBehandlingResponse(
            mottakId = when (finalInput.ankemulighetSource) {
                AnkemulighetSource.INFOTRYGD -> createAnkeFromInfotrygdSak(input = finalInput)
                AnkemulighetSource.KABAL -> kabalApiService.createAnkeInKabalFromKlagebehandling(input = finalInput)
            }
        )
    }

    private fun createAnkeFromInfotrygdSak(input: CreateAnkeInput): UUID {
        val sakFromKlanke = klageFssProxyService.getSak(sakId = input.id)
        val frist = input.mottattKlageinstans.plusWeeks(input.fristInWeeks.toLong())
        val mottakId = kabalApiService.createAnkeInKabalFromCompleteInput(
            input = input,
            sakFromKlanke = sakFromKlanke,
            frist = frist
        )
        klageFssProxyService.setToHandledInKabal(
            sakId = sakFromKlanke.sakId,
            frist = frist,
        )
        return mottakId
    }

    fun getAnkemuligheter(input: IdnummerInput): List<Ankemulighet> {
        val ankemuligheterFromInfotrygd = getAnkemuligheterFromInfotrygd(input)
        val ankemuligheterFromKabal =
            kabalApiService.getAnkemuligheter(input)

        return ankemuligheterFromInfotrygd + ankemuligheterFromKabal
    }

    private fun getAnkemuligheterFromInfotrygd(input: IdnummerInput): List<Ankemulighet> {
        val resultsFromInfotrygd = klageFssProxyService.getAnkemuligheter(input = input)

        return resultsFromInfotrygd
            .filter {
                !kabalApiService.mulighetIsDuplicate(
                    fagsystem = Fagsystem.IT01,
                    kildereferanse = it.sakId,
                    type = Type.ANKE,
                )
            }
            .map {
                Ankemulighet(
                    id = it.sakId,
                    ytelseId = null,
                    hjemmelIdList = null,
                    temaId = Tema.fromNavn(it.tema).id,
                    vedtakDate = null,
                    sakenGjelder = kabalApiService.searchPartWithUtsendingskanal(
                        SearchPartWithUtsendingskanalInput(
                            identifikator = it.fnr,
                            sakenGjelderId = it.fnr,
                            //don't care which ytelse is picked, as long as Tema is correct. Could be prettier.
                            ytelseId = Ytelse.entries.find { y -> y.toTema().navn == it.tema }!!.id,
                        )
                    ).partViewWithUtsendingskanal(),
                    klager = null,
                    fullmektig = null,
                    fagsakId = it.fagsakId,
                    fagsystemId = Fagsystem.IT01.id,
                    previousSaksbehandler = null,
                    sourceId = AnkemulighetSource.INFOTRYGD.fagsystem.id,
                    typeId = Type.ANKE.id,
                    sourceOfExistingAnkebehandling = emptyList(),
                )
            }

    }

    fun getCreatedAnkeStatus(mottakId: UUID): CreatedAnkebehandlingStatusView {
        val response = kabalApiService.getCreatedAnkeStatus(mottakId = mottakId)

        return CreatedAnkebehandlingStatusView(
            typeId = response.typeId,
            ytelseId = response.ytelseId,
            vedtakDate = if (Fagsystem.of(response.fagsystemId) == Fagsystem.IT01) null else response.vedtakDate.toLocalDate(),
            sakenGjelder = response.sakenGjelder.partViewWithUtsendingskanal(),
            klager = response.klager.partViewWithUtsendingskanal(),
            fullmektig = response.fullmektig?.partViewWithUtsendingskanal(),
            mottattKlageinstans = response.mottattNav,
            frist = response.frist,
            fagsakId = response.fagsakId,
            fagsystemId = response.fagsystemId,
            journalpost = response.journalpost.toView(),
            tildeltSaksbehandler = response.tildeltSaksbehandler?.toView(),
            svarbrev = response.svarbrev?.toView(),
        )
    }
}