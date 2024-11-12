package no.nav.klage.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient.newConnection

@Configuration
class GosysOppgaveClientConfiguration(private val webClientBuilder: WebClient.Builder) {
    @Value("\${GOSYS_OPPGAVE_BASE_URL}")
    private lateinit var gosysOppgaveBaseURL: String

    @Bean("gosysOppgaveWebClient")
    fun gosysOppgaveWebClient(): WebClient {
        return webClientBuilder
            .baseUrl(gosysOppgaveBaseURL)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(ReactorClientHttpConnector(newConnection()))
            .build()
    }

}
