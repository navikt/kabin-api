package no.nav.klage.clients.saf.graphql

import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getTeamLogger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.LocalDateTime

@Component
class SafGraphQlClient(
    private val safWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val teamLogger = getTeamLogger()
    }

    fun getDokumentoversiktBruker(
        idnummer: String,
        tema: List<Tema>,
        pageSize: Int,
        previousPageRef: String? = null
    ): DokumentoversiktBruker {
        val start = System.currentTimeMillis()
        return runWithTimingAndLogging {
            safWebClient.post()
                .uri("graphql")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithSafScope()}"
                )
                .bodyValue(hentDokumentoversiktBrukerQuery(idnummer, tema, pageSize, previousPageRef))
                .retrieve()
                .bodyToMono<DokumentoversiktBrukerResponse>()
                .block()
                ?.let { logErrorsFromSaf(it, idnummer, pageSize, previousPageRef); it }
                ?.let { failOnErrors(it); it }
                ?.data!!.dokumentoversiktBruker.also {
                    logger.debug(
                        "DokumentoversiktBruker: antall: {}, ms: {}, dato/tid: {}",
                        it.sideInfo.totaltAntall,
                        System.currentTimeMillis() - start,
                        LocalDateTime.now()
                    )
                }
        }
    }

    fun getJournalpostAsSaksbehandler(journalpostId: String): Journalpost? {
        return runWithTimingAndLogging {
            val token = tokenUtil.getSaksbehandlerAccessTokenWithSafScope()
            getJournalpostWithToken(journalpostId, token)
        }
    }

    private fun getJournalpostWithToken(journalpostId: String, token: String) = safWebClient.post()
        .uri("graphql")
        .header(
            HttpHeaders.AUTHORIZATION,
            "Bearer $token"
        )
        .bodyValue(hentJournalpostQuery(journalpostId))
        .retrieve()
        .bodyToMono<JournalpostResponse>()
        .block()
        ?.let { logErrorsFromSaf(it, journalpostId); it }
        ?.let { failOnErrors(it); it }
        ?.data?.journalpost

    private fun failOnErrors(response: JournalpostResponse) {
        if (response.data == null || response.errors != null && response.errors.map { it.extensions.classification }
                .contains("ValidationError")) {
            throw RuntimeException("getJournalpost failed")
        }
    }

    private fun failOnErrors(response: DokumentoversiktBrukerResponse) {
        if (response.data == null || response.errors != null) {
            throw RuntimeException("getDokumentoversiktBruker failed")
        }
    }

    private fun logErrorsFromSaf(
        response: DokumentoversiktBrukerResponse,
        fnr: String,
        pageSize: Int,
        previousPageRef: String?
    ) {
        if (response.errors != null) {
            logger.error("Error from SAF, see team-logs")
            teamLogger.error("Error from SAF when making call with following parameters: fnr=$fnr, pagesize=$pageSize, previousPageRef=$previousPageRef. Error is ${response.errors}")
        }
    }

    private fun logErrorsFromSaf(
        response: JournalpostResponse,
        journalpostId: String
    ) {
        if (response.errors != null) {
            logger.error("Error from SAF when making call with following parameters: journalpostId=$journalpostId. See more in team-logs")
            teamLogger.error("Error from SAF when making call with following parameters: journalpostId=$journalpostId. Error is ${response.errors}")
        }
    }

    fun <T> runWithTimingAndLogging(block: () -> T): T {
        val start = System.currentTimeMillis()
        try {
            return block.invoke()
        } finally {
            val end = System.currentTimeMillis()
            logger.debug("Time it took to call saf: ${end - start} millis")
        }
    }
}