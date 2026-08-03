package no.nav.klage.clients.fileapi

import no.nav.klage.exceptions.AttachmentCouldNotBeConvertedException
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class FileApiClient(
    private val fileWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun createUploadPolicy(contentType: String): UploadPostPolicyResponse {
        logger.debug("Requesting signed upload policy from kabal-file-api for content type {}", contentType)

        val token = tokenUtil.getOnBehalfOfTokenWithKabalFileApiScope()

        return fileWebClient
            .post()
            .uri("/document/uploadpolicy")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .bodyValue(UploadUrlRequest(contentType = contentType))
            .retrieve()
            .onStatus(HttpStatusCode::isError) { clientResponse ->
                clientResponse.bodyToMono(String::class.java).map { body ->
                    logger.error("Error requesting signed upload policy from kabal-file-api: $body")
                    RuntimeException("Error requesting signed upload policy from kabal-file-api")
                }
            }
            .bodyToMono<UploadPostPolicyResponse>()
            .block() ?: throw RuntimeException("No response from kabal-file-api on upload policy request")
    }

    fun getDocumentMetadata(id: String): DocumentMetadataResponse {
        logger.debug("Fetching document metadata for id {} from kabal-file-api", id)

        val token = tokenUtil.getOnBehalfOfTokenWithKabalFileApiScope()

        return fileWebClient
            .get()
            .uri { it.path("/document/{id}/metadata").build(id) }
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .onStatus(HttpStatusCode::isError) { clientResponse ->
                clientResponse.bodyToMono(String::class.java).map { body ->
                    logger.error("Error fetching document metadata from kabal-file-api: $body")
                    RuntimeException("Error fetching document metadata from kabal-file-api")
                }
            }
            .bodyToMono<DocumentMetadataResponse>()
            .block() ?: throw RuntimeException("No response from kabal-file-api on metadata request")
    }

    fun processDocument(id: String): ProcessResultResponse {
        logger.debug("Requesting processing (scan + convert) for document with id {} from kabal-file-api", id)

        val token = tokenUtil.getOnBehalfOfTokenWithKabalFileApiScope()

        return fileWebClient
            .post()
            .uri { it.path("/document/{id}/process").build(id) }
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .onStatus({ it.value() == 422 }) { clientResponse ->
                clientResponse.bodyToMono(String::class.java).map { body ->
                    logger.warn("kabal-file-api could not convert document to PDF: $body")
                    AttachmentCouldNotBeConvertedException()
                }
            }
            .onStatus(HttpStatusCode::isError) { clientResponse ->
                clientResponse.bodyToMono(String::class.java).map { body ->
                    logger.error("Error processing document in kabal-file-api: $body")
                    RuntimeException("Error processing document in kabal-file-api")
                }
            }
            .bodyToMono<ProcessResultResponse>()
            .block() ?: throw RuntimeException("No response from kabal-file-api on process request")
    }

    fun getDocumentViewUrl(id: String, headers: Map<String, String>): String {
        logger.debug("Requesting view (signed GET) URL for document with id {} from kabal-file-api", id)

        val token = tokenUtil.getOnBehalfOfTokenWithKabalFileApiScope()

        return fileWebClient
            .post()
            .uri { it.path("/document/{id}/signedurl").build(id) }
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .bodyValue(SignedUrlRequest(headers = headers))
            .retrieve()
            .onStatus(HttpStatusCode::isError) { clientResponse ->
                clientResponse.bodyToMono(String::class.java).map { body ->
                    logger.error("Error requesting view URL from kabal-file-api: $body")
                    RuntimeException("Error requesting view URL from kabal-file-api")
                }
            }
            .bodyToMono<String>()
            .block() ?: throw RuntimeException("No response from kabal-file-api on view URL request")
    }

    fun deleteDocument(id: String) {
        logger.debug("Deleting document with id {} from kabal-file-api", id)

        val token = tokenUtil.getOnBehalfOfTokenWithKabalFileApiScope()

        try {
            fileWebClient
                .delete()
                .uri { it.path("/document/{id}").build(id) }
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .retrieve()
                .onStatus(HttpStatusCode::isError) { clientResponse ->
                    clientResponse.bodyToMono(String::class.java).map { body ->
                        logger.error("Error deleting document from kabal-file-api: $body")
                        RuntimeException("Error deleting document from kabal-file-api")
                    }
                }
                .bodyToMono<Boolean>()
                .block()
        } catch (e: Exception) {
            logger.error("Could not delete document ($id) from kabal-file-api", e)
        }
    }
}

data class UploadPostPolicyResponse(
    val id: String,
    val url: String,
    val fields: Map<String, String>,
    val contentType: String,
    val maxSize: Long,
)

data class UploadUrlRequest(
    val contentType: String,
)

data class DocumentMetadataResponse(
    val exists: Boolean,
    val size: Long?,
    val contentType: String?,
)

data class ProcessResultResponse(
    val hasVirus: Boolean,
    val size: Long?,
    val contentType: String?,
    val wasConverted: Boolean,
)

data class SignedUrlRequest(
    val headers: Map<String, String> = emptyMap(),
)
