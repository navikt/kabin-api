package no.nav.klage.service

import no.nav.klage.api.controller.mapper.toReceiptView
import no.nav.klage.api.controller.view.CreateAnkeInputView
import no.nav.klage.api.controller.view.CreatedAnkebehandlingStatusView
import no.nav.klage.api.controller.view.CreatedBehandlingResponse
import no.nav.klage.api.controller.view.IdnummerInput
import no.nav.klage.clients.SakFromKlanke
import no.nav.klage.clients.kabalapi.AnkemulighetFromKabal
import no.nav.klage.clients.kabalapi.toView
import no.nav.klage.domain.CreateAnkeInput
import no.nav.klage.domain.entities.Mulighet
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.TimeUnitType
import no.nav.klage.util.MulighetSource
import no.nav.klage.util.ValidationUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.*

@Service
class AnkeService(
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

    fun createAnke(input: CreateAnkeInputView, ankemulighet: Mulighet): CreatedBehandlingResponse {
        val processedInput = validationUtil.validateCreateAnkeInputView(input)
        val journalpostId = dokArkivService.handleJournalpostBasedOnAnkeInput(
            input = processedInput,
            ankemulighet = ankemulighet
        )
        val finalInput = processedInput.copy(ankeDocumentJournalpostId = journalpostId)

        return CreatedBehandlingResponse(
            behandlingId = when (finalInput.mulighetSource) {
                MulighetSource.INFOTRYGD -> createAnkeFromInfotrygdSak(input = finalInput)
                MulighetSource.KABAL -> kabalApiService.createAnkeInKabalFromKlagebehandling(input = finalInput)
            }
        )
    }

    private fun createAnkeFromInfotrygdSak(input: CreateAnkeInput): UUID {
        val sakFromKlanke = klageFssProxyService.getSak(sakId = input.id)
        val frist = when(input.behandlingstidUnitType) {
            TimeUnitType.WEEKS -> input.mottattKlageinstans.plusWeeks(input.behandlingstidUnits.toLong())
            TimeUnitType.MONTHS -> input.mottattKlageinstans.plusMonths(input.behandlingstidUnits.toLong())
        }
        val behandlingId = kabalApiService.createAnkeInKabalFromCompleteInput(
            input = input,
            sakFromKlanke = sakFromKlanke,
            frist = frist
        )

        try {
            klageFssProxyService.setToHandledInKabal(
                sakId = sakFromKlanke.sakId,
                frist = frist,
            )
        } catch (e: Exception) {
            logger.error("Failed to set to handled in kabal", e)
        }

        try {
            input.oppgaveId?.let {
                logger.debug("Attempting oppgave update")
                oppgaveService.updateOppgave(
                    oppgaveId = it,
                    frist = frist,
                    tildeltSaksbehandlerIdent = input.saksbehandlerIdent,
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to update oppgave", e)
        }

        return behandlingId
    }

    fun getAnkemuligheterFromKabalAsMono(input: IdnummerInput): Mono<List<AnkemulighetFromKabal>> {
        return kabalApiService.getAnkemuligheterAsMono(input)
    }

    fun getAnkemuligheterFromInfotrygdAsMono(input: IdnummerInput): Mono<List<SakFromKlanke>> {
        return klageFssProxyService.getAnkemuligheterAsMono(input)
    }

    fun getCreatedAnkeStatus(behandlingId: UUID): CreatedAnkebehandlingStatusView {
        val response = kabalApiService.getCreatedAnkeStatus(behandlingId = behandlingId)

        return CreatedAnkebehandlingStatusView(
            typeId = response.typeId,
            ytelseId = response.ytelseId,
            vedtakDate = response.vedtakDate.toLocalDate(),
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
}