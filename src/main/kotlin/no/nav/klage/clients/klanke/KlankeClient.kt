package no.nav.klage.clients.klanke

import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Component
class KlankeClient(
    private val klankeWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun searchKlanke(input: KlankeSearchInput): Mono<List<SakFromKlanke>> {
        return klankeWebClient.post()
            .uri("/rest/saker")
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getOnBehalfOfTokenWithKlankeScope()}"
            )
            .bodyValue(input)
            .retrieve()
            .bodyToMono<List<SakFromKlanke>>()
    }

    fun getSakAppAccess(sakId: String, saksbehandlerIdent: String): SakFromKlanke {
        return klankeWebClient.post()
            .uri { it.path("/rest/saker/{sakId}").build(sakId) }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getMaskinTilMaskinTokenWithKlankeScope()}"
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
        return klankeWebClient.get()
            .uri { it.path("/rest/access").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getOnBehalfOfTokenWithKlankeScope()}"
            )
            .retrieve()
            .bodyToMono<Access>()
            .block()
            ?: throw RuntimeException("Empty result")
    }

    fun setToHandledInKabal(sakId: String, input: HandledInKabalInput) {
        klankeWebClient.post()
            .uri { it.path("/rest/saker/{sakId}/handledinkabal").build(sakId) }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getOnBehalfOfTokenWithKlankeScope()}"
            )
            .bodyValue(input)
            .retrieve()
            .bodyToMono<Unit>()
            .block()
    }
}
