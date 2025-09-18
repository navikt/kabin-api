package no.nav.klage.service

import no.nav.klage.api.controller.view.CreatedBehandlingResponse
import no.nav.klage.domain.entities.Mulighet
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.exceptions.IllegalInputException
import no.nav.klage.kodeverk.TimeUnitType
import no.nav.klage.util.ValidationUtil
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service
import java.util.*

@Service
class KlageService(
    private val validationUtil: ValidationUtil,
    private val dokArkivService: DokArkivService,
    private val klageFssProxyService: KlageFssProxyService,
    private val kabalApiService: KabalApiService,
    private val gosysOppgaveService: GosysOppgaveService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun createKlage(registrering: Registrering): CreatedBehandlingResponse {
        val mulighet = registrering.mulighetId?.let { mulighetId ->
            registrering.muligheter.find { it.id == mulighetId }
        } ?: throw IllegalInputException("Muligheten som registreringen refererer til finnes ikke.")

        validationUtil.validateRegistrering(registrering = registrering, mulighet = mulighet)

        val journalpostId = dokArkivService.handleJournalpost(
            registrering = registrering,
        )

        return CreatedBehandlingResponse(
            behandlingId = createKlageFromInfotrygdSak(
                journalpostId = journalpostId,
                mulighet = mulighet,
                registrering = registrering
            ),
        )
    }

    private fun createKlageFromInfotrygdSak(
        journalpostId: String,
        mulighet: Mulighet,
        registrering: Registrering
    ): UUID {
        val frist = when (registrering.behandlingstidUnitType) {
            TimeUnitType.WEEKS -> registrering.mottattKlageinstans!!.plusWeeks(registrering.behandlingstidUnits.toLong())
            TimeUnitType.MONTHS -> registrering.mottattKlageinstans!!.plusMonths(registrering.behandlingstidUnits.toLong())
        }
        val behandlingId = kabalApiService.createKlageFromInfotrygdInput(
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
            //Gosys-oppgave is ensured in validation step.
            logger.debug("Attempting Gosys-oppgave update")
            gosysOppgaveService.updateGosysOppgave(
                gosysOppgaveId = registrering.gosysOppgaveId!!,
                tildeltSaksbehandlerIdent = registrering.saksbehandlerIdent,
            )
        } catch (e: Exception) {
            logger.error("Failed to update Gosys-oppgave", e)
        }

        return behandlingId
    }
}