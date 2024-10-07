package no.nav.klage.service

import no.nav.klage.api.controller.view.CreatedBehandlingResponse
import no.nav.klage.api.controller.view.IdnummerInput
import no.nav.klage.clients.kabalapi.MulighetFromKabal
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.exceptions.IllegalInputException
import no.nav.klage.util.ValidationUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class OmgjoeringskravService(
    private val validationUtil: ValidationUtil,
    private val dokArkivService: DokArkivService,
    private val kabalApiService: KabalApiService,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    fun createOmgjoeringskrav(registrering: Registrering): CreatedBehandlingResponse {
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
            behandlingId = kabalApiService.createBehandlingInKabalFromKabalInput(
                journalpostId = journalpostId,
                mulighet = mulighet,
                registrering = registrering
            )
        )
    }


    fun getOmgjoeringskravmuligheterFromKabalAsMono(input: IdnummerInput): Mono<List<MulighetFromKabal>> {
        return kabalApiService.getOmgjoeringskravmuligheterAsMono(input)
    }
}