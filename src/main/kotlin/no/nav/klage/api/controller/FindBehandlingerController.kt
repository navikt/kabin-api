package no.nav.klage.api.controller

import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.util.getLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.RestController

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class FindBehandlingerController(

) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }


}