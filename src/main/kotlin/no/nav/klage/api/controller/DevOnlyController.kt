package no.nav.klage.api.controller

import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.KlageFssProxyClient
import no.nav.klage.clients.KlankeSearchInput
import no.nav.klage.clients.KlankeSearchOutput
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.util.*
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*
import java.util.*

@Profile("dev-gcp")
@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class DevOnlyController(
    private val klageFssProxyClient: KlageFssProxyClient
) {

    @PostMapping("/klanke/search")
    fun searchKlanke(
        @RequestBody input: KlankeSearchInput
    ): KlankeSearchOutput {
        return klageFssProxyClient.searchKlanke(input)
    }
}