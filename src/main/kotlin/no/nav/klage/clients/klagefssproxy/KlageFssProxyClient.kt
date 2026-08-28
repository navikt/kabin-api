package no.nav.klage.clients.klagefssproxy

import no.nav.klage.clients.klanke.Access
import no.nav.klage.clients.klanke.GetSakAppAccessInput
import no.nav.klage.clients.klanke.HandledInKabalInput
import no.nav.klage.clients.klanke.KlankeSearchInput
import no.nav.klage.clients.klanke.SakFromKlanke
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Component
class KlageFssProxyClient(
    private val klageFssProxyWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun searchKlanke(
        input: KlankeSearchInput,
        token: String,
    ): Mono<List<SakFromKlanke>> =
        klageFssProxyWebClient
            .post()
            .uri("/klanke/saker")
            .header(
                HttpHeaders.AUTHORIZATION,
                token,
            ).bodyValue(input)
            .retrieve()
            .bodyToMono<List<SakFromKlanke>>()

    fun getSakAppAccess(
        sakId: String,
        saksbehandlerIdent: String,
    ): SakFromKlanke =
        klageFssProxyWebClient
            .post()
            .uri { it.path("/klanke/saker/{sakId}").build(sakId) }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getMaskinTilMaskinTokenWithKlageFSSProxyScope()}",
            ).bodyValue(
                GetSakAppAccessInput(
                    saksbehandlerIdent = saksbehandlerIdent,
                ),
            ).retrieve()
            .bodyToMono<SakFromKlanke>()
            .block()
            ?: throw RuntimeException("Empty result")

    fun checkAccess(): Access =
        klageFssProxyWebClient
            .get()
            .uri { it.path("/klanke/access").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getOnBehalfOfTokenWithKlageFSSProxyScope()}",
            ).retrieve()
            .bodyToMono<Access>()
            .block()
            ?: throw RuntimeException("Empty result")

    fun setToHandledInKabal(
        sakId: String,
        input: HandledInKabalInput,
    ) {
        klageFssProxyWebClient
            .post()
            .uri { it.path("/klanke/saker/{sakId}/handledinkabal").build(sakId) }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getOnBehalfOfTokenWithKlageFSSProxyScope()}",
            ).bodyValue(input)
            .retrieve()
            .bodyToMono<Unit>()
            .block()
    }
}
