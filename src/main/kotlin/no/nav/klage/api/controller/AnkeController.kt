package no.nav.klage.api.controller

import no.nav.klage.api.controller.view.*
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.service.AnkeService
import no.nav.klage.util.*
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class AnkeController(
    private val tokenUtil: TokenUtil,
    private val ankeService: AnkeService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @PostMapping("/createanke", produces = ["application/json"])
    fun createAnke(@RequestBody input: CreateAnkeInputView): CreatedBehandlingResponse {
        logMethodDetails(
            methodName = ::createAnke.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        secureLogger.debug("createAnke called with: {}", input)
        return ankeService.createAnke(input)
    }

    @PostMapping("/ankemuligheter", produces = ["application/json"])
    fun getAnkemuligheterByIdnummer(@RequestBody input: IdnummerInput): List<Ankemulighet> {
        logMethodDetails(
            methodName = ::getAnkemuligheterByIdnummer.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return ankeService.getAnkemuligheter(input = input)
    }

    @GetMapping("/anker/{mottakId}/status")
    fun createdAnkeStatus(
        @PathVariable mottakId: UUID,
    ): CreatedAnkebehandlingStatusView {
        logMethodDetails(
            methodName = ::createdAnkeStatus.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return ankeService.getCreatedAnkeStatus(mottakId)
    }
}