package no.nav.klage.api.controller

import io.swagger.v3.oas.annotations.Hidden
import no.nav.klage.api.controller.view.GosysOppgaveView
import no.nav.klage.clients.KlageFssProxyClient
import no.nav.klage.clients.SakFromKlanke
import no.nav.klage.clients.gosysoppgave.Gjelder
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.kodeverk.Tema
import no.nav.klage.service.GosysOppgaveService
import no.nav.klage.util.TokenUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@Profile("dev")
@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
@Hidden
class DevOnlyController(
    private val klageFssProxyClient: KlageFssProxyClient,
    private val tokenUtil: TokenUtil,
    private val gosysOppgaveService: GosysOppgaveService,
) {

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
        return tokenUtil.getOnBehalfOfTokenWithDokArkivScope()
    }

    @Unprotected
    @GetMapping("/internal/gosysoppgavetoken")
    fun getGosysOppgaveToken(): String {
        return tokenUtil.getOnBehalfOfTokenWithGosysOppgaveScope()
    }

    @GetMapping("/gosysoppgaver/{fnr}")
    fun searchGosysOppgaveForFnr(
        @PathVariable fnr: String
    ): List<GosysOppgaveView> {
        return gosysOppgaveService.getGosysOppgaveList(fnr = fnr, tema = null)
    }

    @GetMapping("/gosysoppgaver/kodeverk/gjelder/{tema}")
    fun searchGjelderKodeverk(
        @PathVariable tema: String
    ): List<Gjelder> {
        return gosysOppgaveService.getGjelderKodeverkForTema(tema = Tema.fromNavn(tema))
    }
}