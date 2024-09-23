package no.nav.klage.service

import no.nav.klage.api.controller.mapper.toReceiptView
import no.nav.klage.api.controller.view.CreatedAnkebehandlingStatusView
import no.nav.klage.api.controller.view.IdnummerInput
import no.nav.klage.clients.kabalapi.MulighetFromKabal
import no.nav.klage.clients.kabalapi.toView
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.util.ValidationUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.*

@Service
class OmgjoeringskravService(
    private val validationUtil: ValidationUtil,
    private val dokArkivService: DokArkivService,
    private val klageFssProxyService: KlageFssProxyService,
    private val kabalApiService: KabalApiService,
    private val oppgaveService: OppgaveService,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

//    fun createAnke(input: CreateAnkeInputView, ankemulighet: Mulighet): CreatedBehandlingResponse {
//        val processedInput = validationUtil.validateCreateAnkeInputView(input)
//        val journalpostId = dokArkivService.handleJournalpostBasedOnAnkeInput(
//            input = processedInput,
//            ankemulighet = ankemulighet
//        )
//        val finalInput = processedInput.copy(ankeDocumentJournalpostId = journalpostId)
//
//        return CreatedBehandlingResponse(
//            behandlingId = when (finalInput.mulighetSource) {
//                MulighetSource.INFOTRYGD -> createAnkeFromInfotrygdSak(input = finalInput)
//                MulighetSource.KABAL -> kabalApiService.createAnkeInKabalFromKlagebehandling(input = finalInput)
//            }
//        )
//    }


    fun getCreatedAnkeStatus(behandlingId: UUID): CreatedAnkebehandlingStatusView {
        val response = kabalApiService.getCreatedAnkeStatus(behandlingId = behandlingId)

        return CreatedAnkebehandlingStatusView(
            typeId = response.typeId,
            ytelseId = response.ytelseId,
            vedtakDate = if (Fagsystem.of(response.fagsystemId) == Fagsystem.IT01) null else response.vedtakDate.toLocalDate(),
            sakenGjelder = response.sakenGjelder.partViewWithUtsendingskanal(),
            klager = response.klager.partViewWithUtsendingskanal(),
            fullmektig = response.fullmektig?.partViewWithUtsendingskanal(),
            mottattKlageinstans = response.mottattNav,
            frist = response.frist,
            varsletFrist = response.varsletFrist,
            varsletFristUnits = response.varsletFristUnits,
            varsletFristUnitTypeId = response.varsletFristUnitTypeId,
            fagsakId = response.fagsakId,
            fagsystemId = response.fagsystemId,
            journalpost = response.journalpost.toReceiptView(),
            tildeltSaksbehandler = response.tildeltSaksbehandler?.toView(),
            svarbrev = response.svarbrev?.toView(),
        )
    }

    fun getOmgjoeringskravmuligheterFromKabalAsMono(input: IdnummerInput): Mono<List<MulighetFromKabal>> {
        return kabalApiService.getOmgjoeringskravmuligheterAsMono(input)
    }
}