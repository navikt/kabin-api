package no.nav.klage.clients.dokarkiv

import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

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
            dokArkivWebClient.put()
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
        } catch (e: Exception) {
            logger.error("Error updating journalpost $journalpostId:", e)
        }

        logger.debug("Document from journalpost $journalpostId updated with saksId ${input.sak.fagsakid}.")
    }
}