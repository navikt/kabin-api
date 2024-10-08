package no.nav.klage.clients.oppgaveapi

import io.opentelemetry.api.trace.Span
import no.nav.klage.config.CacheWithJCacheConfiguration
import no.nav.klage.kodeverk.Tema
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class OppgaveClient(
    private val oppgaveWebClient: WebClient,
    private val tokenUtil: TokenUtil,
    @Value("\${spring.application.name}") private val applicationName: String,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val securelogger = getSecureLogger()
    }

    fun fetchJournalfoeringsoppgave(
        journalpostId: String,
    ): OppgaveApiRecord? {
        val oppgaveResponse =
            logTimingAndWebClientResponseException(OppgaveClient::fetchJournalfoeringsoppgave.name) {
                oppgaveWebClient.get()
                    .uri { uriBuilder ->
                        uriBuilder.pathSegment("oppgaver")
                        uriBuilder.queryParam("statuskategori", Statuskategori.AAPEN)
                        uriBuilder.queryParam("oppgavetype", "JFR")
                        uriBuilder.queryParam("journalpostId", journalpostId)
                        uriBuilder.queryParam("limit", 1)
                        uriBuilder.queryParam("offset", 0)
                        uriBuilder.build()
                    }
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithOppgaveScope()}"
                    )
                    .header("X-Correlation-ID", Span.current().spanContext.traceId)
                    .header("Nav-Consumer-Id", applicationName)
                    .retrieve()
                    .bodyToMono<OppgaveResponse>()
                    .block() ?: throw OppgaveClientException("Oppgaver could not be fetched")
            }

        if (oppgaveResponse.oppgaver.size > 1) {
            throw OppgaveClientException("Forventet ingen eller én journalfoeringsoppgave, men fant ${oppgaveResponse.antallTreffTotalt}.")
        }
        return oppgaveResponse.oppgaver.firstOrNull()
    }

    fun fetchOppgaveForAktoerIdAndTema(
        aktoerId: String,
        tema: Tema?
    ): List<OppgaveApiRecord> {
        val oppgaveResponse =
            logTimingAndWebClientResponseException(OppgaveClient::fetchOppgaveForAktoerIdAndTema.name) {
                oppgaveWebClient.get()
                    .uri { uriBuilder ->
                        uriBuilder.pathSegment("oppgaver")
                        uriBuilder.queryParam("aktoerId", aktoerId)
                        uriBuilder.queryParam("statuskategori", Statuskategori.AAPEN)
                        tema?.let { uriBuilder.queryParam("tema", it.navn) }
                        uriBuilder.queryParam("limit", 1000)
                        uriBuilder.queryParam("offset", 0)
                        uriBuilder.build()
                    }
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithOppgaveScope()}"
                    )
                    .header("X-Correlation-ID", Span.current().spanContext.traceId)
                    .header("Nav-Consumer-Id", applicationName)
                    .retrieve()
                    .bodyToMono<OppgaveResponse>()
                    .block() ?: throw OppgaveClientException("Oppgaver could not be fetched")
            }

        return oppgaveResponse.oppgaver
    }

    fun ferdigstillOppgave(ferdigstillOppgaveRequest: FerdigstillOppgaveRequest): OppgaveApiRecord {
        return logTimingAndWebClientResponseException(OppgaveClient::ferdigstillOppgave.name) {
            oppgaveWebClient.patch()
                .uri { uriBuilder ->
                    uriBuilder.pathSegment("oppgaver", "{id}").build(ferdigstillOppgaveRequest.oppgaveId)
                }
                .contentType(MediaType.APPLICATION_JSON)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithOppgaveScope()}"
                )
                .header("X-Correlation-ID", Span.current().spanContext.traceId)
                .header("Nav-Consumer-Id", applicationName)
                .bodyValue(ferdigstillOppgaveRequest)
                .retrieve()
                .bodyToMono<OppgaveApiRecord>()
                .block() ?: throw OppgaveClientException("Kunne ikke ferdigstille oppgaven.")
        }
    }

    fun getOppgave(oppgaveId: Long): OppgaveApiRecord {
        return logTimingAndWebClientResponseException(OppgaveClient::getOppgave.name) {
            oppgaveWebClient.get()
                .uri { uriBuilder ->
                    uriBuilder.pathSegment("oppgaver", "{id}").build(oppgaveId)
                }
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithOppgaveScope()}"
                )
                .header("X-Correlation-ID", Span.current().spanContext.traceId)
                .header("Nav-Consumer-Id", applicationName)
                .retrieve()
                .bodyToMono<OppgaveApiRecord>()
                .block() ?: throw OppgaveClientException("Oppgave could not be fetched")
        }
    }

    fun updateOppgave(oppgaveId: Long, updateOppgaveInput: UpdateOppgaveInput): OppgaveApiRecord {
        return logTimingAndWebClientResponseException(OppgaveClient::updateOppgave.name) {
            oppgaveWebClient.patch()
                .uri { uriBuilder ->
                    uriBuilder.pathSegment("oppgaver", "{id}").build(oppgaveId)
                }
                .bodyValue(updateOppgaveInput)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithOppgaveScope()}"
                )
                .header("X-Correlation-ID", Span.current().spanContext.traceId)
                .header("Nav-Consumer-Id", applicationName)
                .retrieve()
                .bodyToMono<OppgaveApiRecord>()
                .block() ?: throw OppgaveClientException("Oppgave could not be updated")
        }
    }

    @Cacheable(CacheWithJCacheConfiguration.GJELDER_CACHE)
    fun getGjelderKodeverkForTema(tema: Tema): List<Gjelder> {
        val gjelderResponse =
            logTimingAndWebClientResponseException(OppgaveClient::getGjelderKodeverkForTema.name) {
                oppgaveWebClient.get()
                    .uri { uriBuilder ->
                        uriBuilder.pathSegment("kodeverk", "gjelder", "{tema}")
                        uriBuilder.build(tema.navn)
                    }
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithOppgaveScope()}"
                    )
                    .header("X-Correlation-ID", Span.current().spanContext.traceId)
                    .header("Nav-Consumer-Id", applicationName)
                    .retrieve()
                    .bodyToMono<List<Gjelder>>()
                    .block() ?: throw OppgaveClientException("Could not fetch gjelder kodeverk for tema ${tema.navn}")
            }

        return gjelderResponse
    }

    @Cacheable(CacheWithJCacheConfiguration.OPPGAVETYPE_CACHE)
    fun getOppgavetypeKodeverkForTema(tema: Tema): List<OppgavetypeResponse> {
        val oppgavetypeResponse =
            logTimingAndWebClientResponseException(OppgaveClient::getGjelderKodeverkForTema.name) {
                oppgaveWebClient.get()
                    .uri { uriBuilder ->
                        uriBuilder.pathSegment("kodeverk", "oppgavetype", "{tema}")
                        uriBuilder.build(tema.navn)
                    }
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithOppgaveScope()}"
                    )
                    .header("X-Correlation-ID", Span.current().spanContext.traceId)
                    .header("Nav-Consumer-Id", applicationName)
                    .retrieve()
                    .bodyToMono<List<OppgavetypeResponse>>()
                    .block() ?: throw OppgaveClientException("Could not fetch oppgavetype kodeverk for tema ${tema.navn}")
            }

        return oppgavetypeResponse
    }

    private fun <T> logTimingAndWebClientResponseException(methodName: String, function: () -> T): T {
        val start: Long = System.currentTimeMillis()
        try {
            return function.invoke()
        } catch (ex: WebClientResponseException) {
            logger.warn("Caught WebClientResponseException, see securelogs for details")
            securelogger.error(
                "Got a {} error calling Oppgave {} {} with message {}",
                ex.statusCode,
                ex.request?.method ?: "-",
                ex.request?.uri ?: "-",
                ex.responseBodyAsString
            )
            throw OppgaveClientException("Caught WebClientResponseException", ex)
        } catch (rtex: RuntimeException) {
            logger.warn("Caught RuntimeException", rtex)
            throw OppgaveClientException("Caught runtimeexception", rtex)
        } finally {
            val end: Long = System.currentTimeMillis()
            logger.info("Method {} took {} millis", methodName, (end - start))
        }
    }
}
