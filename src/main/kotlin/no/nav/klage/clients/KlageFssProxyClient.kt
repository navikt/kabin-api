package no.nav.klage.clients

import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.LocalDate

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

    fun getSak(sakId: String): SakFromKlanke {
        return klageFssProxyWebClient.get()
            .uri { it.path("/klanke/saker/{sakId}").build(sakId) }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getOnBehalfOfTokenWithKlageFSSProxyScope()}"
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

data class KlankeSearchInput(
    val fnr: String,
    val sakstype: String,
)

data class SakFromKlanke(
    val sakId: String,
    val fagsakId: String,
    val tema: String,
    val utfall: String,
    val enhetsnummer: String,
    val vedtaksdato: LocalDate,
    val fnr: String,
    val sakstype: String,
)

data class HandledInKabalInput(
    val fristAsString: String
)

data class Access(
    val access: Boolean
)