package no.nav.klage.clients.saf.rest

import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException


@Component
class SafRestClient(
    private val safWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getDokument(
        dokumentInfoId: String,
        journalpostId: String,
        variantFormat: String = "ARKIV"
    ): ArkivertDokument {
        return try {
            runWithTimingAndLogging {
                safWebClient.get()
                    .uri(
                        "/rest/hentdokument/{journalpostId}/{dokumentInfoId}/{variantFormat}",
                        journalpostId,
                        dokumentInfoId,
                        variantFormat
                    )
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithSafScope()}"
                    )
                    .retrieve()
                    .toEntity(ByteArray::class.java)
                    .map {
                        val type = it.headers.contentType
                        ArkivertDokument(
                            bytes = it.body ?: throw RuntimeException("no document data"),
                            contentType = type ?: throw RuntimeException("no content type")
                        )
                    }
                    .block() ?: throw RuntimeException("no document data returned")
            }
        } catch (badRequest: WebClientResponseException.BadRequest) {
            logger.warn("Got a 400 fetching dokument with journalpostId $journalpostId, dokumentInfoId $dokumentInfoId and variantFormat $variantFormat")
            throw badRequest
        } catch (unautorized: WebClientResponseException.Unauthorized) {
            logger.warn("Got a 401 fetching dokument with journalpostId $journalpostId, dokumentInfoId $dokumentInfoId and variantFormat $variantFormat")
            throw unautorized
        } catch (forbidden: WebClientResponseException.Forbidden) {
            logger.warn("Got a 403 fetching dokument with journalpostId $journalpostId, dokumentInfoId $dokumentInfoId and variantFormat $variantFormat")
            throw forbidden
        } catch (notFound: WebClientResponseException.NotFound) {
            logger.warn("Got a 404 fetching dokument with journalpostId $journalpostId, dokumentInfoId $dokumentInfoId and variantFormat $variantFormat")
            throw notFound
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


