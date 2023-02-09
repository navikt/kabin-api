package no.nav.klage.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import no.nav.klage.api.controller.view.DokumenterResponse
import no.nav.klage.clients.KabalApiClient
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.kodeverk.Tema
import no.nav.klage.service.DocumentService
import no.nav.klage.service.KabalApiService
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class CreateAnkeController(
    private val kabalApiService: KabalApiService,
    private val documentService: DocumentService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @PostMapping("/createanke", produces = ["application/json"])
    fun createAnke(@RequestBody input: CreateAnkeBasedOnKlagebehandling) {
        secureLogger.debug("createAnke called with: {}", input)
        kabalApiService.createAnkeInKabal(input)
    }

    @GetMapping("/{fnr}/ankemuligheter", produces = ["application/json"])
    fun getCompletedKlagebehandlingerByPartIdValue(@PathVariable fnr: String): List<KabalApiClient.CompletedKlagebehandling> {
        secureLogger.debug("getCompletedKlagebehandlingerByPartIdValue called for {}", fnr)
        return kabalApiService.getCompletedKlagebehandlingerByPartIdValue(fnr)
    }

    @Operation(
        summary = "Hent metadata om dokumenter for brukeren som saken gjelder"
    )
    @GetMapping("/{fnr}/arkivertedokumenter", produces = ["application/json"])
    fun fetchDokumenter(
        @PathVariable("fnr") fnr: String,
        @RequestParam(required = false, name = "antall", defaultValue = "10") pageSize: Int,
        @RequestParam(required = false, name = "forrigeSide") previousPageRef: String? = null,
        @RequestParam(required = false, name = "temaer") temaer: List<String>? = emptyList()
    ): DokumenterResponse {
        return documentService.fetchDokumentlisteForBruker(
            fnr = fnr,
            temaer = temaer?.map { Tema.of(it) } ?: emptyList(),
            pageSize = pageSize,
            previousPageRef = previousPageRef
        )
    }

}