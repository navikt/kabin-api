package no.nav.klage.clients.pdl


import no.nav.klage.clients.pdl.grahql.HentIdenterResponse
import no.nav.klage.clients.pdl.grahql.hentAktoerIdQuery
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.klage.util.logErrorResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.lang.System.currentTimeMillis

@Component
class PdlClient(
    private val pdlWebClient: WebClient,
    private val tokenUtil: TokenUtil
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    fun <T> runWithTiming(block: () -> T): T {
        val start = currentTimeMillis()
        try {
            return block.invoke()
        } finally {
            val end = currentTimeMillis()
            logger.debug("Time it took to call pdl: ${end - start} millis")
        }
    }

    @Retryable
    fun hentAktoerIdent(fnr: String): String {
        return runWithTiming {
            pdlWebClient.post()
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithPdlScope()}")
                .bodyValue(hentAktoerIdQuery(fnr))
                .retrieve()
                .onStatus(HttpStatusCode::isError) { response ->
                    logErrorResponse(response, ::hentAktoerIdent.name, secureLogger)
                }
                .bodyToMono<HentIdenterResponse>()
                .block()?.data?.hentIdenter?.identer?.firstOrNull()?.ident ?: throw RuntimeException("Person not found")
        }
    }
}
