package no.nav.klage.api.controller

import no.nav.klage.clients.KabalApiClient
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.service.KabalApiService
import no.nav.klage.util.getLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class CreateAnkeController(
    private val kabalApiService: KabalApiService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @PostMapping("/internal/createanke", produces = ["application/json"])
    fun createAnke(@RequestBody input: CreateAnkeBasedOnKlagebehandling) {
        logger.debug("createAnke called with: {}", input)
        kabalApiService.createAnkeInKabal(input)
    }

    @GetMapping("/internal/{fnr}/ankemuligheter", produces = ["application/json"])
    fun getCompletedKlagebehandlingerByPartIdValue(@PathVariable fnr: String): List<KabalApiClient.CompletedKlagebehandling> {
        logger.debug("getCompletedKlagebehandlingerByPartIdValue called for {}", fnr)
        return kabalApiService.getCompletedKlagebehandlingerByPartIdValue(fnr)
    }

}