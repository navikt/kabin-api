package no.nav.klage.api.controller

import no.nav.klage.api.controller.view.CreatedKlagebehandlingStatusView
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.service.KlageService
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.klage.util.logMethodDetails
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
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

    @GetMapping("/klager/{behandlingId}/status")
    fun createdKlageStatus(
        @PathVariable behandlingId: UUID,
    ): CreatedKlagebehandlingStatusView {
        logMethodDetails(
            methodName = ::createdKlageStatus.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return klageService.getCreatedKlageStatus(behandlingId = behandlingId)
    }
}