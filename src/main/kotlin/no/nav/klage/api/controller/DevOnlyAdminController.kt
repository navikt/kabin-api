package no.nav.klage.api.controller

import no.nav.klage.util.TokenUtil
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*
import java.util.*

@Profile("dev-gcp")
@RestController
class DevOnlyAdminController(
    private val tokenUtil: TokenUtil,
) {
    @Unprotected
    @GetMapping("/internal/mytoken")
    fun getToken(): String {
        return tokenUtil.getAccessTokenFrontendSent()
    }

    @Unprotected
    @GetMapping("/internal/dokarkivtoken")
    fun getDokakrivToken(): String {
        return tokenUtil.getSaksbehandlerAccessTokenWithDokArkivScope()
    }
}