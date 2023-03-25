package no.nav.klage.clients

import no.nav.klage.api.controller.view.*
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.LocalDate
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

    fun searchKlanke(input: KlankeSearchInput): List<SakFromKlanke> {
        return klageFssProxyWebClient.post()
            .uri("/klanke/saker")
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getOnBehalfOfTokenWithKlageFSSProxyScope()}"
            )
            .bodyValue(input)
            .retrieve()
            .bodyToMono<List<SakFromKlanke>>()
            .block()
            ?: throw RuntimeException("Empty result")
    }
}

data class KlankeSearchInput(
    val fnr: String
)

data class SakFromKlanke(
    val sakId: String,
    val fagsakId: String,
    val tema: String,
    val utfall: String,
    val enhetsnummer: String,
    val vedtaksdato: LocalDate,
)