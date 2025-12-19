package no.nav.klage.clients.gosysoppgave

import io.opentelemetry.api.trace.Span
import no.nav.klage.config.CacheWithJCacheConfiguration
import no.nav.klage.exceptions.GosysOppgaveClientException
import no.nav.klage.kodeverk.Tema
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getTeamLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class GosysOppgaveClient(
    private val gosysOppgaveWebClient: WebClient,
    private val tokenUtil: TokenUtil,
    @Value("\${spring.application.name}") private val applicationName: String,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val teamLogger = getTeamLogger()
    }

    @Retryable
    fun fetchJournalfoeringsoppgave(
        journalpostId: String,
    ): GosysOppgaveRecord? {
        val gosysOppgaveResponse =
            logTimingAndWebClientResponseException(GosysOppgaveClient::fetchJournalfoeringsoppgave.name) {
                gosysOppgaveWebClient.get()
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
                        "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithGosysOppgaveScope()}"
                    )
                    .header("X-Correlation-ID", Span.current().spanContext.traceId)
                    .header("Nav-Consumer-Id", applicationName)
                    .retrieve()
                    .bodyToMono<GosysOppgaveResponse>()
                    .block() ?: throw RuntimeException("Gosys-oppgaver could not be fetched")
            }

        if (gosysOppgaveResponse.oppgaver.size > 1) {
            throw GosysOppgaveClientException("Forventet ingen eller én journalfoeringsoppgave, men fant ${gosysOppgaveResponse.antallTreffTotalt}.")
        }
        return gosysOppgaveResponse.oppgaver.firstOrNull()
    }

    @Retryable
    fun fetchGosysOppgaverForAktoerIdAndTema(
        aktoerId: String,
        temaList: List<Tema>?
    ): List<GosysOppgaveRecord> {
        val gosysOppgaveResponse =
            logTimingAndWebClientResponseException(GosysOppgaveClient::fetchGosysOppgaverForAktoerIdAndTema.name) {
                gosysOppgaveWebClient.get()
                    .uri { uriBuilder ->
                        uriBuilder.pathSegment("oppgaver")
                        uriBuilder.queryParam("aktoerId", aktoerId)
                        uriBuilder.queryParam("statuskategori", Statuskategori.AAPEN)
                        temaList?.let { uriBuilder.queryParam("tema", temaList?.map { it.navn }) }
                        uriBuilder.queryParam("limit", 1000)
                        uriBuilder.queryParam("offset", 0)
                        uriBuilder.build()
                    }
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithGosysOppgaveScope()}"
                    )
                    .header("X-Correlation-ID", Span.current().spanContext.traceId)
                    .header("Nav-Consumer-Id", applicationName)
                    .retrieve()
                    .bodyToMono<GosysOppgaveResponse>()
                    .block() ?: throw RuntimeException("Gosys-oppgaver could not be fetched")
            }

        return gosysOppgaveResponse.oppgaver
    }

    @Retryable
    fun ferdigstillGosysOppgave(ferdigstillGosysOppgaveRequest: FerdigstillGosysOppgaveRequest): GosysOppgaveRecord {
        return logTimingAndWebClientResponseException(GosysOppgaveClient::ferdigstillGosysOppgave.name) {
            gosysOppgaveWebClient.patch()
                .uri { uriBuilder ->
                    uriBuilder.pathSegment("oppgaver", "{id}").build(ferdigstillGosysOppgaveRequest.oppgaveId)
                }
                .contentType(MediaType.APPLICATION_JSON)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithGosysOppgaveScope()}"
                )
                .header("X-Correlation-ID", Span.current().spanContext.traceId)
                .header("Nav-Consumer-Id", applicationName)
                .bodyValue(ferdigstillGosysOppgaveRequest)
                .retrieve()
                .bodyToMono<GosysOppgaveRecord>()
                .block() ?: throw RuntimeException("Kunne ikke ferdigstille Gosys-oppgaven.")
        }
    }

    @Retryable
    fun getGosysOppgave(gosysOppgaveId: Long): GosysOppgaveRecord {
        return logTimingAndWebClientResponseException(GosysOppgaveClient::getGosysOppgave.name) {
            gosysOppgaveWebClient.get()
                .uri { uriBuilder ->
                    uriBuilder.pathSegment("oppgaver", "{id}").build(gosysOppgaveId)
                }
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithGosysOppgaveScope()}"
                )
                .header("X-Correlation-ID", Span.current().spanContext.traceId)
                .header("Nav-Consumer-Id", applicationName)
                .retrieve()
                .bodyToMono<GosysOppgaveRecord>()
                .block() ?: throw RuntimeException("Gosys-oppgave could not be fetched")
        }
    }

    @Retryable
    fun updateGosysOppgave(gosysOppgaveId: Long, updateGosysOppgaveInput: UpdateGosysOppgaveInput): GosysOppgaveRecord {
        return logTimingAndWebClientResponseException(GosysOppgaveClient::updateGosysOppgave.name) {
            gosysOppgaveWebClient.patch()
                .uri { uriBuilder ->
                    uriBuilder.pathSegment("oppgaver", "{id}").build(gosysOppgaveId)
                }
                .bodyValue(updateGosysOppgaveInput)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithGosysOppgaveScope()}"
                )
                .header("X-Correlation-ID", Span.current().spanContext.traceId)
                .header("Nav-Consumer-Id", applicationName)
                .retrieve()
                .bodyToMono<GosysOppgaveRecord>()
                .block() ?: throw RuntimeException("Gosys-oppgave could not be updated")
        }
    }

    @Cacheable(CacheWithJCacheConfiguration.GJELDER_CACHE)
    fun getGjelderKodeverkForTema(tema: Tema): List<Gjelder> {
        val gjelderResponse =
            logTimingAndWebClientResponseException(GosysOppgaveClient::getGjelderKodeverkForTema.name) {
                gosysOppgaveWebClient.get()
                    .uri { uriBuilder ->
                        uriBuilder.pathSegment("kodeverk", "gjelder", "{tema}")
                        uriBuilder.build(tema.navn)
                    }
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithGosysOppgaveScope()}"
                    )
                    .header("X-Correlation-ID", Span.current().spanContext.traceId)
                    .header("Nav-Consumer-Id", applicationName)
                    .retrieve()
                    .bodyToMono<List<Gjelder>>()
                    .block() ?: throw RuntimeException("Could not fetch gjelder kodeverk for tema ${tema.navn}")
            }

        return gjelderResponse
    }

    @Cacheable(CacheWithJCacheConfiguration.OPPGAVETYPE_CACHE)
    fun getGosysOppgavetypeKodeverkForTema(tema: Tema): List<GosysOppgavetypeResponse> {
        val gosysOppgavetypeResponse =
            logTimingAndWebClientResponseException(GosysOppgaveClient::getGjelderKodeverkForTema.name) {
                gosysOppgaveWebClient.get()
                    .uri { uriBuilder ->
                        uriBuilder.pathSegment("kodeverk", "oppgavetype", "{tema}")
                        uriBuilder.build(tema.navn)
                    }
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithGosysOppgaveScope()}"
                    )
                    .header("X-Correlation-ID", Span.current().spanContext.traceId)
                    .header("Nav-Consumer-Id", applicationName)
                    .retrieve()
                    .bodyToMono<List<GosysOppgavetypeResponse>>()
                    .block() ?: throw RuntimeException("Could not fetch oppgavetype kodeverk for tema ${tema.navn}")
            }

        return gosysOppgavetypeResponse
    }

    private fun <T> logTimingAndWebClientResponseException(methodName: String, function: () -> T): T {
        val start: Long = System.currentTimeMillis()
        try {
            return function.invoke()
        } catch (ex: WebClientResponseException) {
            logger.warn("Caught WebClientResponseException, see team-logs for details")
            teamLogger.error(
                "Got a {} error calling Oppgave {} {} with message {}",
                ex.statusCode,
                ex.request?.method ?: "-",
                ex.request?.uri ?: "-",
                ex.responseBodyAsString
            )
            throw GosysOppgaveClientException("Caught WebClientResponseException", ex)
        } catch (rtex: RuntimeException) {
            logger.warn("Caught RuntimeException", rtex)
            throw GosysOppgaveClientException("Caught runtimeexception", rtex)
        } finally {
            val end: Long = System.currentTimeMillis()
            logger.info("Method {} took {} millis", methodName, (end - start))
        }
    }
}
