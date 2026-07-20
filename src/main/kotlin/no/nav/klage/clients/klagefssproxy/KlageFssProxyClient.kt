package no.nav.klage.clients.klagefssproxy

import no.nav.klage.clients.klanke.*
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

    fun searchKlanke(input: KlankeSearchInput, token: String): Mono<List<SakFromKlanke>> {
        return klageFssProxyWebClient.post()
            .uri("/klanke/saker")
            .header(
                HttpHeaders.AUTHORIZATION,
                token,
            )
            .bodyValue(input)
            .retrieve()
            .bodyToMono<List<SakFromKlanke>>()
    }

    fun getSakAppAccess(sakId: String, saksbehandlerIdent: String): SakFromKlanke {
        return klageFssProxyWebClient.post()
            .uri { it.path("/klanke/saker/{sakId}").build(sakId) }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getMaskinTilMaskinTokenWithKlageFSSProxyScope()}"
            )
            .bodyValue(
                GetSakAppAccessInput(
                    saksbehandlerIdent = saksbehandlerIdent
                )
            )
            .retrieve()
            .bodyToMono<SakFromKlanke>()
            .block()
            ?: throw RuntimeException("Empty result")
    }

    fun checkAccess(): Access {
        return klageFssProxyWebClient.get()
            .uri { it.path("/klanke/access").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getOnBehalfOfTokenWithKlageFSSProxyScope()}"
            )
            .retrieve()
            .bodyToMono<Access>()
            .block()
            ?: throw RuntimeException("Empty result")
    }

    fun setToHandledInKabal(sakId: String, input: HandledInKabalInput) {
        klageFssProxyWebClient.post()
            .uri { it.path("/klanke/saker/{sakId}/handledinkabal").build(sakId) }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getOnBehalfOfTokenWithKlageFSSProxyScope()}"
            )
            .bodyValue(input)
            .retrieve()
            .bodyToMono<Unit>()
            .block()
    }
}
