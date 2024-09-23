package no.nav.klage.service

import no.nav.klage.api.controller.mapper.toReceiptView
import no.nav.klage.api.controller.view.CreatedBehandlingResponse
import no.nav.klage.api.controller.view.CreatedKlagebehandlingStatusView
import no.nav.klage.api.controller.view.IdnummerInput
import no.nav.klage.clients.SakFromKlanke
import no.nav.klage.clients.kabalapi.toView
import no.nav.klage.domain.CreateBehandlingInput
import no.nav.klage.domain.entities.Mulighet
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.exceptions.IllegalInputException
import no.nav.klage.kodeverk.TimeUnitType
import no.nav.klage.util.MulighetSource
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

    fun createKlage(registrering: Registrering): CreatedBehandlingResponse {
        val mulighet = registrering.mulighetId?.let { mulighetId ->
            registrering.muligheter.find { it.id == mulighetId }
        } ?: throw IllegalInputException("Muligheten som registreringen refererer til finnes ikke.")

        validationUtil.validateRegistrering(registrering = registrering, mulighet = mulighet)
        val processedInput = registrering.toCreateBehandlingInput(mulighet)

        val journalpostId = dokArkivService.handleJournalpostBasedOnInfotrygdSak(
            journalpostId = processedInput.receivedDocumentJournalpostId,
            mulighet = mulighet,
            avsender = processedInput.avsender,
        )

        val finalInput = processedInput.copy(receivedDocumentJournalpostId = journalpostId)

        return CreatedBehandlingResponse(
            behandlingId = createKlageFromInfotrygdSak(input = finalInput)
        )
    }

    private fun createKlageFromInfotrygdSak(input: CreateBehandlingInput): UUID {
        //TODO, get from cache?
        val sakFromKlanke = klageFssProxyService.getSak(sakId = input.currentFagystemTechnicalId)
        val frist = when (input.behandlingstidUnitType) {
            TimeUnitType.WEEKS -> input.mottattKlageinstans.plusWeeks(input.behandlingstidUnits.toLong())
            TimeUnitType.MONTHS -> input.mottattKlageinstans.plusMonths(input.behandlingstidUnits.toLong())
        }
        val behandlingId = kabalApiService.createKlageInKabalFromCompleteInput(
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

    fun Registrering.toCreateBehandlingInput(mulighet: Mulighet): CreateBehandlingInput {
        return CreateBehandlingInput(
            currentFagystemTechnicalId = mulighet.currentFagystemTechnicalId,
            mulighetSource = MulighetSource.of(mulighet.currentFagsystem),
            mottattKlageinstans = mottattKlageinstans!!,
            mottattVedtaksinstans = mottattVedtaksinstans!!,
            behandlingstidUnits = behandlingstidUnits,
            behandlingstidUnitType = behandlingstidUnitType,
            klager = klager.toPartIdInput()!!,
            fullmektig = fullmektig.toPartIdInput(),
            receivedDocumentJournalpostId = journalpostId!!,
            ytelseId = ytelse!!.id,
            hjemmelIdList = hjemmelIdList,
            avsender = avsender.toPartIdInput(),
            saksbehandlerIdent = saksbehandlerIdent!!,
            svarbrevInput = toSvarbrevWithReceiverInput(this.getSvarbrevSettings()),
            oppgaveId = oppgaveId,
            typeId = type!!.id

        )
    }

    private fun Registrering.getSvarbrevSettings() = kabalApiService.getSvarbrevSettings(
        ytelseId = ytelse!!.id,
        typeId = type!!.id
    )
}