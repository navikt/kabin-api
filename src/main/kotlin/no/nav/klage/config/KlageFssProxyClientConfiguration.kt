package no.nav.klage.config

import no.nav.klage.util.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient


@Configuration
class KlageFssProxyClientConfiguration(
    private val webClientBuilder: WebClient.Builder
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Value("\${KLAGE_FSS_PROXY_URL}")
    private lateinit var klageFssProxyUrl: String

    @Bean
    fun klageFssProxyWebClient(): WebClient {
        return webClientBuilder
            .baseUrl(klageFssProxyUrl)
            .build()
    }
}