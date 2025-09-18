package no.nav.klage.service

import no.nav.klage.api.controller.view.CreatedBehandlingResponse
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.exceptions.IllegalInputException
import no.nav.klage.util.ValidationUtil
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service

@Service
class OmgjoeringskravService(
    private val validationUtil: ValidationUtil,
    private val dokArkivService: DokArkivService,
    private val kabalApiService: KabalApiService,
    private val gosysOppgaveService: GosysOppgaveService,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun createOmgjoeringskrav(registrering: Registrering): CreatedBehandlingResponse {
        val mulighet = registrering.mulighetId?.let { mulighetId ->
            registrering.muligheter.find { it.id == mulighetId }
        } ?: throw IllegalInputException("Muligheten som registreringen refererer til finnes ikke.")

        validationUtil.validateRegistrering(registrering = registrering, mulighet = mulighet)

        val journalpostId = dokArkivService.handleJournalpost(
            registrering = registrering,
        )

        if (registrering.mulighetIsBasedOnJournalpost) {
            val kabalResponse = CreatedBehandlingResponse(
                behandlingId = kabalApiService.createOmgjoeringskravBasedOnJournalpost(
                    journalpostId = journalpostId,
                    mulighet = mulighet,
                    registrering = registrering
                )
            )
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
            return kabalResponse
        } else {
            return CreatedBehandlingResponse(
                behandlingId = kabalApiService.createBehandlingFromKabalInput(
                    journalpostId = journalpostId,
                    mulighet = mulighet,
                    registrering = registrering
                )
            )
        }
    }
}