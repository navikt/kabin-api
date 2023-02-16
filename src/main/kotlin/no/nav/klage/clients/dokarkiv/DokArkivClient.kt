package no.nav.klage.clients.dokarkiv

import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component

class DokArkivClient(
    private val dokArkivWebClient: WebClient,
    private val tokenUtil: TokenUtil
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun updateDocumentTitleOnBehalfOf(journalpostId: String, input: UpdateJournalpostSaksIdRequest) {
        try {
            val output = dokArkivWebClient.put()
                .uri("/${journalpostId}")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithDokArkivScope()}"
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(input)
                .retrieve()
                .bodyToMono(JournalpostResponse::class.java)
                .block()
                ?: throw RuntimeException("Journalpost could not be updated.")
            logger.debug("Svar fra dokarkiv: $output")
        } catch (e: Exception) {
            logger.error("Error updating journalpost $journalpostId:", e)
            throw e
        }

        logger.debug("Document from journalpost $journalpostId updated with saksId ${input.sak.fagsakid}.")
    }

    fun finalizeJournalpostOnBehalfOf(journalpostId: String, journalfoerendeEnhet: String) {
        try {
            val output = dokArkivWebClient.patch()
                .uri("/${journalpostId}/ferdigstill")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithDokArkivScope()}"
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(FerdigstillJournalpostPayload(journalfoerendeEnhet))
                .retrieve()
                .bodyToMono<String>()
                .block()
                ?: throw RuntimeException("Journalpost could not be finalized")

            logger.debug("Finalized journalpost, response from dokarkiv: $output")
        } catch (e: Exception) {
            logger.error("Error finalizing journalpost $journalpostId:", e)
            throw e
        }

        logger.debug("Journalpost with id $journalpostId was succesfully finalized.")
    }

    data class FerdigstillJournalpostPayload(
        val journalfoerendeEnhet: String
    )
}