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
class OppgaveClientConfiguration(private val webClientBuilder: WebClient.Builder) {
    @Value("\${OPPGAVE_BASE_URL}")
    private lateinit var oppgaveBaseURL: String

    @Bean("oppgaveWebClient")
    fun oppgaveWebClient(): WebClient {
        return webClientBuilder
            .baseUrl(oppgaveBaseURL)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(ReactorClientHttpConnector(newConnection()))
            .build()
    }

}
