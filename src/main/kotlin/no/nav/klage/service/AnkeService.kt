package no.nav.klage.service

import no.nav.klage.api.controller.mapper.toReceiptView
import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.SakFromKlanke
import no.nav.klage.clients.kabalapi.MulighetFromKabal
import no.nav.klage.clients.kabalapi.toView
import no.nav.klage.domain.CreateBehandlingInput
import no.nav.klage.domain.entities.Mulighet
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.exceptions.IllegalInputException
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

    fun createAnke(registrering: Registrering): CreatedBehandlingResponse {
        val mulighet = registrering.mulighetId?.let { mulighetId ->
            registrering.muligheter.find { it.id == mulighetId }
        } ?: throw IllegalInputException("Muligheten som registreringen refererer til finnes ikke.")

        validationUtil.validateRegistrering(registrering = registrering, mulighet = mulighet)
        val processedInput = registrering.toCreateBehandlingInput(mulighet)

        val journalpostId = dokArkivService.handleJournalpostBasedOnAnkeInput(
            input = processedInput,
            ankemulighet = mulighet
        )
        val finalInput = processedInput.copy(receivedDocumentJournalpostId = journalpostId)

        return CreatedBehandlingResponse(
            behandlingId = when (finalInput.mulighetSource) {
                MulighetSource.INFOTRYGD -> createAnkeFromInfotrygdSak(input = finalInput)
                MulighetSource.KABAL -> kabalApiService.createAnkeInKabalFromKlagebehandling(input = finalInput)
            }
        )
    }

    private fun createAnkeFromInfotrygdSak(input: CreateBehandlingInput): UUID {
        val sakFromKlanke = klageFssProxyService.getSak(sakId = input.currentFagystemTechnicalId)
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

    fun getAnkemuligheterFromKabalAsMono(input: IdnummerInput): Mono<List<MulighetFromKabal>> {
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



    fun Registrering.toCreateBehandlingInput(mulighet: Mulighet): CreateBehandlingInput {
        return CreateBehandlingInput(
            currentFagystemTechnicalId = mulighet.currentFagystemTechnicalId,
            mulighetSource = MulighetSource.of(mulighet.currentFagsystem),
            mottattKlageinstans = mottattKlageinstans!!,
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
            typeId = type!!.id,
            mottattVedtaksinstans = null,
        )
    }

    private fun Registrering.getSvarbrevSettings() = kabalApiService.getSvarbrevSettings(
        ytelseId = ytelse!!.id,
        typeId = type!!.id
    )
}