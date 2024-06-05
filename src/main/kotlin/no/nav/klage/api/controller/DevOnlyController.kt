package no.nav.klage.api.controller

import io.swagger.v3.oas.annotations.Hidden
import no.nav.klage.clients.KlageFssProxyClient
import no.nav.klage.clients.KlankeSearchInput
import no.nav.klage.clients.SakFromKlanke
import no.nav.klage.clients.oppgaveapi.GjelderResponse
import no.nav.klage.clients.oppgaveapi.OppgaveApiRecord
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.kodeverk.Tema
import no.nav.klage.service.OppgaveService
import no.nav.klage.util.*
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*

@Profile("dev-gcp")
@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
@Hidden
class DevOnlyController(
    private val klageFssProxyClient: KlageFssProxyClient,
    private val tokenUtil: TokenUtil,
    private val oppgaveService: OppgaveService,
) {

    @PostMapping("/klanke/search")
    fun searchKlanke(
        @RequestBody input: KlankeSearchInput
    ): List<SakFromKlanke> {
        return klageFssProxyClient.searchKlanke(input)
    }

    @GetMapping("/klanke/sak/{sakId}")
    fun getSakFromKlankeAppAccess(
        @PathVariable sakId: String
    ): SakFromKlanke {
        return klageFssProxyClient.getSakAppAccess(sakId = sakId, saksbehandlerIdent = tokenUtil.getCurrentIdent())
    }

    @Unprotected
    @GetMapping("/internal/mytoken")
    fun getToken(): String {
        return tokenUtil.getAccessTokenFrontendSent()
    }

    @Unprotected
    @GetMapping("/internal/dokarkivtoken")
    fun getDokarkivToken(): String {
        return tokenUtil.getSaksbehandlerAccessTokenWithDokArkivScope()
    }

    @GetMapping("/oppgaver/{fnr}")
    fun searchOppgaveForFnr(
        @PathVariable fnr: String
    ): List<OppgaveApiRecord> {
        return oppgaveService.getOppgaveList(fnr = fnr, tema = null)
    }

    @GetMapping("/oppgaver/kodeverk/gjelder/{tema}")
    fun searchGjelderKodeverk(
        @PathVariable tema: String
    ): GjelderResponse {
        return oppgaveService.getGjelderKodeverkForTema(tema = Tema.fromNavn(tema))
    }
}