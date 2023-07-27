package no.nav.klage.service

import no.nav.klage.api.controller.mapper.toView
import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.kabalapi.toView
import no.nav.klage.domain.CreateKlageInput
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.Tema
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.infotrygdKlageutfallToUtfall
import no.nav.klage.util.ValidationUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.stereotype.Service
import java.util.*

@Service
class KlageService(
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

    fun createKlage(input: CreateKlageInputView): CreatedBehandlingResponse {
        val processedInput = validationUtil.validateCreateKlageInputView(input)
        val journalpostId = dokArkivService.handleJournalpostBasedOnInfotrygdSak(
            journalpostId = processedInput.klageJournalpostId,
            eksternBehandlingId = processedInput.eksternBehandlingId,
            avsender = input.avsender,
            type = Type.KLAGE
        )

        val finalInput = processedInput.copy(klageJournalpostId = journalpostId)

        return CreatedBehandlingResponse(
            mottakId = createKlageFromInfotrygdSak(input = finalInput)
        )
    }

    private fun createKlageFromInfotrygdSak(input: CreateKlageInput): UUID {
        val sakFromKlanke = klageFssProxyService.getSak(sakId = input.eksternBehandlingId)
        val frist = input.mottattKlageinstans.plusWeeks(input.fristInWeeks.toLong())
        val mottakId = kabalApiService.createKlageInKabalFromCompleteInput(
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

    fun getKlagemuligheter(input: IdnummerInput): List<Klagemulighet> {
        val resultsFromInfotrygd = klageFssProxyService.getKlagemuligheter(input = input)
        return resultsFromInfotrygd
            .filter {
                !kabalApiService.mulighetIsDuplicate(
                    fagsystem = Fagsystem.IT01,
                    kildereferanse = it.sakId,
                    type = Type.KLAGE,
                )
            }
            .map {
                Klagemulighet(
                    behandlingId = it.sakId,
                    temaId = Tema.fromNavn(it.tema).id,
                    utfallId = infotrygdKlageutfallToUtfall[it.utfall]!!.id,
                    vedtakDate = it.vedtaksdato,
                    fagsakId = it.fagsakId,
                    //TODO: Tilpass når vi får flere fagsystemer.
                    fagsystemId = Fagsystem.IT01.id,
                    klageBehandlendeEnhet = it.enhetsnummer,
                    sakenGjelder = kabalApiService.searchPart(SearchPartInput(identifikator = it.fnr)).toView()
                )
            }
    }

    fun getCreatedKlageStatus(mottakId: UUID): CreatedKlagebehandlingStatusView {
        val status = kabalApiService.getCreatedKlageStatus(mottakId = mottakId)

        //TODO works only for klager in Infotrygd
        val sakFromKlanke = klageFssProxyService.getSak(status.kildereferanse)

        return CreatedKlagebehandlingStatusView(
            typeId = status.typeId,
            ytelseId = status.ytelseId,
            utfallId = infotrygdKlageutfallToUtfall[sakFromKlanke.utfall]!!.id,
            vedtakDate = sakFromKlanke.vedtaksdato,
            sakenGjelder = status.sakenGjelder.toView(),
            klager = status.klager.toView(),
            fullmektig = status.fullmektig?.toView(),
            mottattVedtaksinstans = status.mottattVedtaksinstans,
            mottattKlageinstans = status.mottattKlageinstans,
            frist = status.frist,
            fagsakId = status.fagsakId,
            fagsystemId = status.fagsystemId,
            journalpost = status.journalpost.toView(),
            tildeltSaksbehandler = status.tildeltSaksbehandler?.toView(),
        )
    }
}