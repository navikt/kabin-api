package no.nav.klage.clients

import no.nav.klage.api.controller.view.*
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.*

@Component
class KlageFssProxyClient(
    private val klageFssProxyWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun searchKlanke(input: KlankeSearchInput): KlankeSearchOutput {
        return klageFssProxyWebClient.post()
            .uri("/klanke/search")
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getOnBehalfOfTokenWithKlageFSSProxyScope()}"
            )
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(input)
            .retrieve()
            .bodyToMono<KlankeSearchOutput>()
            .block()
            ?: throw RuntimeException("Empty result")
    }
}

data class KlankeSearchInput(
    val fnr: String
)

data class KlankeSearchOutput(
    val klankeSearchHits: Set<KlankeSearchHit>
)

data class KlankeSearchHit(
    val sakId: String
)