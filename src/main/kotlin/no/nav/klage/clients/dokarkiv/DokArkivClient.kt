package no.nav.klage.clients.dokarkiv

import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.beans.factory.annotation.Value
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

    @Value("\${spring.application.name}")
    lateinit var applicationName: String

    fun createNewJournalpostBasedOnExistingJournalpost(
        payload: CreateNewJournalpostBasedOnExistingJournalpostRequest,
        oldJournalpostId: String,
    ): CreateNewJournalpostBasedOnExistingJournalpostResponse {
        try {
            val journalpostResponse = dokArkivWebClient.put()
                .uri("/${oldJournalpostId}/knyttTilAnnenSak")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithDokArkivScope()}")
                .header("Nav-Consumer-Id", applicationName)
                .header("Nav-User-Id", tokenUtil.getCurrentIdent())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(CreateNewJournalpostBasedOnExistingJournalpostResponse::class.java)
                .block()
                ?: throw RuntimeException("Journalpost could not be created.")

            logger.debug("Journalpost successfully created in dokarkiv based on saksid ${payload.fagsakId}, resulting in id ${journalpostResponse.nyJournalpostId}.")

            return journalpostResponse
        } catch (e: Exception) {
            logger.error("Error creating journalpost in dokarkiv based on existing saksid:", e)
            throw e
        }
    }

    fun updateSakInJournalpost(journalpostId: String, input: UpdateSakInJournalpostRequest) {
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
                ?: throw RuntimeException("Journalpost fagsakid could not be updated.")
            logger.debug("Svar fra dokarkiv: {}", output)
        } catch (e: Exception) {
            logger.error("Error updating journalpost $journalpostId fagsakid:", e)
            throw e
        }

        logger.debug("Document from journalpost $journalpostId updated with saksId ${input.sak.fagsakid}.")
    }

    fun updateAvsenderMottakerInJournalpost(journalpostId: String, input: UpdateAvsenderMottakerInJournalpostRequest) {
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
                ?: throw RuntimeException("Journalpost AvsenderMottaker could not be updated.")
            logger.debug("Svar fra dokarkiv: {}", output)
        } catch (e: Exception) {
            logger.error("Error updating journalpost $journalpostId AvsenderMottaker:", e)
            throw e
        }
    }

    fun updateDocumentTitle(
        journalpostId: String,
        input: UpdateDocumentTitleJournalpostInput
    ) {
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
                ?: throw RuntimeException("Journalpost document title could not be updated.")
        } catch (e: Exception) {
            logger.error("Error updating journalpost $journalpostId document title:", e)
        }

        logger.debug("Document from journalpost $journalpostId with dokumentInfoId id ${input.dokumenter.first().dokumentInfoId} was successfully updated.")
    }

    fun finalizeJournalpost(journalpostId: String, journalfoerendeEnhet: String) {
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

        logger.debug("Journalpost with id $journalpostId was successfully finalized.")
    }

    data class FerdigstillJournalpostPayload(
        val journalfoerendeEnhet: String
    )
}