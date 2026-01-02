package no.nav.klage.clients.pdl


import no.nav.klage.clients.pdl.grahql.HentIdenterResponse
import no.nav.klage.clients.pdl.grahql.IdentGruppe
import no.nav.klage.clients.pdl.grahql.hentIdenterQuery
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.logErrorResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.resilience.annotation.Retryable
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
    fun hentIdent(ident: String, identGruppe: IdentGruppe): String {
        return runWithTiming {
            pdlWebClient.post()
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithPdlScope()}")
                .bodyValue(hentIdenterQuery(ident))
                .retrieve()
                .onStatus(HttpStatusCode::isError) { response ->
                    logErrorResponse(
                        response = response,
                        functionName = ::hentIdent.name,
                        classLogger = logger,
                    )
                }
                .bodyToMono<HentIdenterResponse>()
                .block()?.data?.hentIdenter?.identer?.find { it.gruppe == identGruppe }?.ident ?: throw RuntimeException("Person not found")
        }
    }
}