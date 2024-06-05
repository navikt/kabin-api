package no.nav.klage.clients.oppgaveapi

import brave.Tracer
import no.nav.klage.kodeverk.Tema
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class OppgaveClient(
    private val oppgaveWebClient: WebClient,
    private val tracer: Tracer,
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
                    .header("X-Correlation-ID", tracer.currentSpan().context().traceIdString())
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
                    .header("X-Correlation-ID", tracer.currentSpan().context().traceIdString())
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
                .header("X-Correlation-ID", tracer.currentSpan().context().traceIdString())
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
                .header("X-Correlation-ID", tracer.currentSpan().context().traceIdString())
                .header("Nav-Consumer-Id", applicationName)
                .retrieve()
                .bodyToMono<OppgaveApiRecord>()
                .block() ?: throw OppgaveClientException("Oppgave could not be fetched")
        }
    }

    fun getGjelderKodeverkForTema(tema: Tema): GjelderResponse {
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
                    .header("X-Correlation-ID", tracer.currentSpan().context().traceIdString())
                    .header("Nav-Consumer-Id", applicationName)
                    .retrieve()
                    .bodyToMono<List<Gjelder>>()
                    .block() ?: throw OppgaveClientException("Could not fetch kodeverk for tema ${tema.navn}")
            }

        return GjelderResponse(gjelder = gjelderResponse)
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
