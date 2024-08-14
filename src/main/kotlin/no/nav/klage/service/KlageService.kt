package no.nav.klage.service

import no.nav.klage.api.controller.mapper.toReceiptView
import no.nav.klage.api.controller.view.CreateKlageInputView
import no.nav.klage.api.controller.view.CreatedBehandlingResponse
import no.nav.klage.api.controller.view.CreatedKlagebehandlingStatusView
import no.nav.klage.api.controller.view.IdnummerInput
import no.nav.klage.clients.SakFromKlanke
import no.nav.klage.clients.kabalapi.toView
import no.nav.klage.domain.CreateKlageInput
import no.nav.klage.domain.entities.Mulighet
import no.nav.klage.kodeverk.TimeUnitType
import no.nav.klage.kodeverk.Type
import no.nav.klage.util.ValidationUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.*

@Service
class KlageService(
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

    fun createKlage(input: CreateKlageInputView, klagemulighet: Mulighet): CreatedBehandlingResponse {
        val processedInput = validationUtil.validateCreateKlageInputView(input)
        val journalpostId = dokArkivService.handleJournalpostBasedOnInfotrygdSak(
            journalpostId = processedInput.klageJournalpostId,
            mulighet = klagemulighet,
            avsender = input.avsender,
            type = Type.KLAGE,
        )

        val finalInput = processedInput.copy(klageJournalpostId = journalpostId)

        return CreatedBehandlingResponse(
            behandlingId = createKlageFromInfotrygdSak(input = finalInput)
        )
    }

    private fun createKlageFromInfotrygdSak(input: CreateKlageInput): UUID {
        //TODO, get from cache?
        val sakFromKlanke = klageFssProxyService.getSak(sakId = input.eksternBehandlingId)
        val frist = when(input.behandlingstidUnitType) {
            TimeUnitType.WEEKS -> input.mottattKlageinstans.plusWeeks(input.behandlingstidUnits.toLong())
            TimeUnitType.MONTHS -> input.mottattKlageinstans.plusMonths(input.behandlingstidUnits.toLong())
        }
        val behandlingId = kabalApiService.createKlageInKabalFromCompleteInput(
            input = input,
            sakFromKlanke = sakFromKlanke,
            frist = frist
        )

        klageFssProxyService.setToHandledInKabal(
            sakId = sakFromKlanke.sakId,
            frist = frist,
        )

        input.oppgaveId?.let {
            logger.debug("Attempting oppgave update")
            oppgaveService.updateOppgave(
                oppgaveId = it,
                frist = frist,
                tildeltSaksbehandlerIdent = input.saksbehandlerIdent,
            )
        }

        return behandlingId
    }

    fun getKlagemuligheterFromInfotrygdAsMono(input: IdnummerInput): Mono<List<SakFromKlanke>> {
        return klageFssProxyService.getKlagemuligheterAsMono(input = input)
    }

    fun getKlageTilbakebetalingMuligheterFromInfotrygdAsMono(input: IdnummerInput): Mono<List<SakFromKlanke>> {
        return klageFssProxyService.getKlageTilbakebetalingMuligheterAsMono(input = input)
    }

    fun getCreatedKlageStatus(behandlingId: UUID): CreatedKlagebehandlingStatusView {
        val status = kabalApiService.getCreatedKlageStatus(behandlingId = behandlingId)

        //TODO works only for klager in Infotrygd
        val sakFromKlanke = klageFssProxyService.getSak(status.kildereferanse)

        return CreatedKlagebehandlingStatusView(
            typeId = status.typeId,
            ytelseId = status.ytelseId,
            vedtakDate = sakFromKlanke.vedtaksdato,
            sakenGjelder = status.sakenGjelder.partViewWithUtsendingskanal(),
            klager = status.klager.partViewWithUtsendingskanal(),
            fullmektig = status.fullmektig?.partViewWithUtsendingskanal(),
            mottattVedtaksinstans = status.mottattVedtaksinstans,
            mottattKlageinstans = status.mottattKlageinstans,
            frist = status.frist,
            varsletFrist = status.varsletFrist,
            varsletFristUnits = status.varsletFristUnits,
            varsletFristUnitTypeId = status.varsletFristUnitTypeId,
            fagsakId = status.fagsakId,
            fagsystemId = status.fagsystemId,
            journalpost = status.journalpost.toReceiptView(),
            tildeltSaksbehandler = status.tildeltSaksbehandler?.toView(),
            svarbrev = status.svarbrev?.toView(),
        )
    }
}