package no.nav.klage.api.controller

import io.swagger.v3.oas.annotations.Hidden
import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.KlageFssProxyClient
import no.nav.klage.clients.KlankeSearchInput
import no.nav.klage.clients.SakFromKlanke
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.util.*
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*
import java.util.*

@Profile("dev-gcp")
@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
@Hidden
class DevOnlyController(
    private val klageFssProxyClient: KlageFssProxyClient,
    private val tokenUtil: TokenUtil,
) {

    @PostMapping("/klanke/search")
    fun searchKlanke(
        @RequestBody input: KlankeSearchInput
    ): List<SakFromKlanke> {
        return klageFssProxyClient.searchKlanke(input)
    }

    @PostMapping("/klanke/sak/{sakId}")
    fun getSakFromKlanke(
        @PathVariable sakId: String
    ): SakFromKlanke {
        return klageFssProxyClient.getSak(sakId)
    }

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