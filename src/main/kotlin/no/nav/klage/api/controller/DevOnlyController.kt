package no.nav.klage.api.controller

import io.swagger.v3.oas.annotations.Hidden
import no.nav.klage.api.controller.view.GosysOppgaveView
import no.nav.klage.clients.gosysoppgave.Gjelder
import no.nav.klage.clients.klanke.SakFromKlanke
import no.nav.klage.kodeverk.Tema
import no.nav.klage.service.GosysOppgaveService
import no.nav.klage.service.KlankeService
import no.nav.klage.util.TokenUtil
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@Profile("dev")
@RestController
@Hidden
class DevOnlyController(
    private val klankeService: KlankeService,
    private val tokenUtil: TokenUtil,
    private val gosysOppgaveService: GosysOppgaveService,
) {
    @GetMapping("/klanke/sak/{sakId}")
    fun getSakFromKlankeAppAccess(
        @PathVariable sakId: String,
    ): SakFromKlanke = klankeService.getSakAppAccess(sakId = sakId, saksbehandlerIdent = tokenUtil.getCurrentIdent())

    @GetMapping("/internal/mytoken")
    fun getToken(): String = tokenUtil.getAccessTokenFrontendSent()

    @GetMapping("/internal/dokarkivtoken")
    fun getDokarkivToken(): String = tokenUtil.getOnBehalfOfTokenWithDokArkivScope()

    @GetMapping("/internal/gosysoppgavetoken")
    fun getGosysOppgaveToken(): String = tokenUtil.getOnBehalfOfTokenWithGosysOppgaveScope()

    @GetMapping("/gosysoppgaver/{fnr}")
    fun searchGosysOppgaveForFnr(
        @PathVariable fnr: String,
    ): List<GosysOppgaveView> = gosysOppgaveService.getGosysOppgaveList(fnr = fnr, tema = null)

    @GetMapping("/gosysoppgaver/kodeverk/gjelder/{tema}")
    fun searchGjelderKodeverk(
        @PathVariable tema: String,
    ): List<Gjelder> = gosysOppgaveService.getGjelderKodeverkForTema(tema = Tema.fromNavn(tema))
}
