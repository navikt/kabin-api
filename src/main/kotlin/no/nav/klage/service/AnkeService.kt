package no.nav.klage.service

import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.SakFromKlanke
import no.nav.klage.clients.kabalapi.MulighetFromKabal
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

        val journalpostId = dokArkivService.handleJournalpost(
            mulighet = mulighet,
            journalpostId = registrering.journalpostId!!,
            avsender = registrering.avsender.toPartIdInput()
        )

        return CreatedBehandlingResponse(
            behandlingId = when (MulighetSource.of(mulighet.currentFagsystem)) {
                MulighetSource.INFOTRYGD -> createAnkeFromInfotrygdSak(
                    journalpostId = journalpostId,
                    mulighet = mulighet,
                    registrering = registrering
                )

                MulighetSource.KABAL -> kabalApiService.createBehandlingInKabalFromKabalInput(
                    journalpostId = journalpostId,
                    mulighet = mulighet,
                    registrering = registrering
                )
            }
        )
    }

    private fun createAnkeFromInfotrygdSak(
        journalpostId: String,
        mulighet: Mulighet,
        registrering: Registrering
    ): UUID {
        val frist = when (registrering.behandlingstidUnitType) {
            TimeUnitType.WEEKS -> registrering.mottattKlageinstans!!.plusWeeks(registrering.behandlingstidUnits.toLong())
            TimeUnitType.MONTHS -> registrering.mottattKlageinstans!!.plusMonths(registrering.behandlingstidUnits.toLong())
        }
        val behandlingId = kabalApiService.createAnkeInKabalFromInfotrygdInput(
            registrering = registrering,
            mulighet = mulighet,
            frist = frist,
            journalpostId = journalpostId,
        )

        try {
            klageFssProxyService.setToHandledInKabal(
                sakId = mulighet.currentFagystemTechnicalId,
                frist = frist,
            )
        } catch (e: Exception) {
            logger.error("Failed to set to handled in kabal", e)
        }

        try {
            registrering.oppgaveId?.let {
                logger.debug("Attempting oppgave update")
                oppgaveService.updateOppgave(
                    oppgaveId = it,
                    frist = frist,
                    tildeltSaksbehandlerIdent = registrering.saksbehandlerIdent,
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
}