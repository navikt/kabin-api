package no.nav.klage.clients.kabalapi

import io.mockk.every
import io.mockk.mockk
import no.nav.klage.service.KabalApiService
import no.nav.klage.util.TokenUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.http.client.reactive.MockClientHttpRequest
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.io.IOException

class KabalApiClientTest {
    @Test
    fun `sends saksnummer as JSON to duplicate endpoint with OBO token`() {
        val client =
            getClient(response = jsonResponse("false")) { request ->
                assertThat(request.method()).isEqualTo(HttpMethod.POST)
                assertThat(request.url().toString()).isEqualTo("http://kabal-api/api/internal/trygderettensaksnummerisduplicate")
                assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-token")

                val httpRequest = MockClientHttpRequest(request.method(), request.url())
                request.writeTo(httpRequest, ExchangeStrategies.withDefaults()).block()
                assertThat(httpRequest.headers.contentType).isEqualTo(MediaType.APPLICATION_JSON)
                val mapper = jacksonObjectMapper()
                assertThat(mapper.readTree(httpRequest.bodyAsString.block())).isEqualTo(
                    mapper.readTree("""{"trygderettenSaksnummer":"2027123"}"""),
                )
            }

        assertThat(KabalApiService(client).trygderettenSaksnummerIsDuplicate(trygderettenSaksnummer = "2027123")).isFalse()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `returns Boolean response unchanged`(duplicate: Boolean) {
        val client = getClient(response = jsonResponse(duplicate.toString()))

        assertThat(client.checkTrygderettenSaksnummerDuplicate(input = getInput())).isEqualTo(duplicate)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "null", """{"duplicate":false}""", "not-json"])
    fun `rejects empty null or malformed response`(body: String) {
        val client = getClient(response = jsonResponse(body))

        assertThatThrownBy {
            client.checkTrygderettenSaksnummerDuplicate(input = getInput())
        }.isInstanceOf(RuntimeException::class.java)
    }

    @ParameterizedTest
    @EnumSource(
        value = HttpStatus::class,
        names = ["BAD_REQUEST", "UNAUTHORIZED", "INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE"],
    )
    fun `propagates Kabal HTTP errors`(status: HttpStatus) {
        val client = getClient(response = Mono.just(ClientResponse.create(status).build()))

        assertThatThrownBy {
            client.checkTrygderettenSaksnummerDuplicate(input = getInput())
        }.isInstanceOf(WebClientResponseException::class.java)
            .extracting("statusCode")
            .isEqualTo(status)
    }

    @Test
    fun `propagates transport failure`() {
        val client = getClient(response = Mono.error(IOException("Kabal unavailable")))

        assertThatThrownBy {
            client.checkTrygderettenSaksnummerDuplicate(input = getInput())
        }.isInstanceOf(RuntimeException::class.java)
            .hasRootCauseMessage("Kabal unavailable")
    }

    private fun getInput() = TrygderettenSaksnummerIsDuplicateInput(trygderettenSaksnummer = "2027123")

    private fun jsonResponse(body: String): Mono<ClientResponse> =
        Mono.just(
            ClientResponse
                .create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build(),
        )

    private fun getClient(
        response: Mono<ClientResponse>,
        onRequest: (ClientRequest) -> Unit = {},
    ): KabalApiClient {
        val tokenUtil = mockk<TokenUtil>()
        every { tokenUtil.getOnBehalfOfTokenWithKabalApiScope() } returns "test-token"
        val webClient =
            WebClient
                .builder()
                .baseUrl("http://kabal-api")
                .exchangeFunction { request ->
                    onRequest(request)
                    response
                }.build()
        return KabalApiClient(kabalApiWebClient = webClient, tokenUtil = tokenUtil)
    }
}
