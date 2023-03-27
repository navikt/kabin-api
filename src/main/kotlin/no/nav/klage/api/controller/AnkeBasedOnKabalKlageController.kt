package no.nav.klage.api.controller

import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.KabalApiClient
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.service.DokArkivService
import no.nav.klage.service.KabalApiService
import no.nav.klage.util.*
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class AnkeBasedOnKabalKlageController(
    private val kabalApiService: KabalApiService,
    private val dokArkivService: DokArkivService,
    private val tokenUtil: TokenUtil,
    private val validationUtil: ValidationUtil
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @PostMapping("/createanke", produces = ["application/json"])
    fun createAnke(@RequestBody input: CreateAnkeBasedOnKlagebehandling): KabalApiClient.CreatedAnkeResponse {
        logMethodDetails(
            methodName = ::createAnke.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )

        secureLogger.debug("createAnke called with: {}", input)

        validationUtil.validateCreateAnkeInput(input)

        val journalpostId = dokArkivService.handleJournalpost(
            journalpostId = input.ankeDocumentJournalpostId,
            klagebehandlingId = input.klagebehandlingId,
        )

        return kabalApiService.createAnkeInKabal(input.copy(ankeDocumentJournalpostId = journalpostId))
    }

    @PostMapping("/ankemuligheter", produces = ["application/json"])
    fun getCompletedKlagebehandlingerByIdnummer(@RequestBody input: IdnummerInput): List<KabalApiClient.CompletedKlagebehandling> {
        logMethodDetails(
            methodName = ::getCompletedKlagebehandlingerByIdnummer.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )

        return kabalApiService.getCompletedKlagebehandlingerByIdnummer(input)
    }

    @GetMapping("/anker/{mottakId}/status")
    fun createdAnkeStatus(
        @PathVariable mottakId: UUID,
    ): KabalApiClient.CreatedBehandlingStatus {
        logMethodDetails(
            methodName = ::createdAnkeStatus.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )
        return kabalApiService.getCreatedAnkeStatus(mottakId = mottakId)
    }
}