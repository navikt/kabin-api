package no.nav.klage.config

import io.netty.channel.ChannelOption
import no.nav.klage.util.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Configuration
class FileApiClientConfiguration(
    private val webClientBuilder: WebClient.Builder
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)

        /**
         * Confirming an upload triggers a synchronous virus scan or conversion of files up to 512 MB
         * in kabal-file-api, which can take a while, so this client gets a generous response timeout.
         *
         * Anything that waits for a request that is talking to kabal-file-api has to outlast this.
         */
        val RESPONSE_TIMEOUT: Duration = Duration.ofSeconds(360)
    }

    @Value($$"${KABAL_FILE_API_BASE_URL}")
    private lateinit var fileApiURL: String

    @Bean
    fun fileWebClient(): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
            .responseTimeout(RESPONSE_TIMEOUT)

        return webClientBuilder
            .clone()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .baseUrl(fileApiURL)
            .build()
    }
}
