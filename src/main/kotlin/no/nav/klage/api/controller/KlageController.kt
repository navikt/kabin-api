package no.nav.klage.api.controller

import no.nav.klage.api.controller.view.*
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.service.KlageService
import no.nav.klage.util.*
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class KlageController(
    private val tokenUtil: TokenUtil,
    private val klageService: KlageService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @PostMapping("/createklage", produces = ["application/json"])
    fun createKlage(@RequestBody input: CreateKlageInputView): CreatedBehandlingResponse {
        logMethodDetails(
            methodName = ::createKlage.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        secureLogger.debug("createklage called with: {}", input)

        return klageService.createKlage(input = input)
    }

    @PostMapping("/klagemuligheter", produces = ["application/json"])
    fun getCompletedKlagebehandlingerInVedtaksinstansByIdnummer(@RequestBody input: IdnummerInput): List<Klagemulighet> {
        logMethodDetails(
            methodName = ::getCompletedKlagebehandlingerInVedtaksinstansByIdnummer.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return klageService.getKlagemuligheter(input)
    }

    @GetMapping("/klager/{mottakId}/status")
    fun createdKlageStatus(
        @PathVariable mottakId: UUID,
    ): CreatedKlagebehandlingStatusView {
        logMethodDetails(
            methodName = ::createdKlageStatus.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return klageService.getCreatedKlageStatus(mottakId = mottakId)
    }
}