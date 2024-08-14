package no.nav.klage.api.controller

import no.nav.klage.api.controller.view.CreatedAnkebehandlingStatusView
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.service.AnkeService
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
class AnkeController(
    private val tokenUtil: TokenUtil,
    private val ankeService: AnkeService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @GetMapping("/anker/{behandlingId}/status")
    fun createdAnkeStatus(
        @PathVariable behandlingId: UUID,
    ): CreatedAnkebehandlingStatusView {
        logMethodDetails(
            methodName = ::createdAnkeStatus.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return ankeService.getCreatedAnkeStatus(behandlingId)
    }
}