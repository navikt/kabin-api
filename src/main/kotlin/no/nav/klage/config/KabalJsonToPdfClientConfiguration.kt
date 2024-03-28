package no.nav.klage.config

import no.nav.klage.util.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class KabalJsonToPdfClientConfiguration(
    private val webClientBuilder: WebClient.Builder
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Value("\${KABAL_JSON_TO_PDF_BASE_URL}")
    private lateinit var kabalJsonToPdfBaseUrl: String

    @Bean
    fun kabalJsonToPdfWebClient(): WebClient {
        return webClientBuilder
            .baseUrl(kabalJsonToPdfBaseUrl)
            .build()
    }
}
