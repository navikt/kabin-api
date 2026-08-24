package no.nav.klage.clients.fileapi

import no.nav.klage.exceptions.AttachmentConversionFailedException
import no.nav.klage.exceptions.AttachmentUnsupportedTypeException
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.resilience.annotation.Retryable
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

    @Retryable(
        maxRetries = 3,
        delay = 1000,
        multiplier = 2.0,
    )
    fun createUploadPolicies(contentTypes: List<String>): List<UploadPostPolicyResponse> {
        logger.debug("Requesting {} signed upload policies from kabal-file-api", contentTypes.size)

        val token = tokenUtil.getOnBehalfOfTokenWithKabalFileApiScope()

        return fileWebClient
            .post()
            .uri("/document/uploadpolicies")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .bodyValue(UploadUrlsRequest(contentTypes = contentTypes))
            .retrieve()
            .onStatus(HttpStatusCode::isError) { clientResponse ->
                clientResponse.bodyToMono<String>().map { body ->
                    logger.error("Error requesting signed upload policies from kabal-file-api: $body")
                    RuntimeException("Error requesting signed upload policies from kabal-file-api")
                }
            }
            .bodyToMono<List<UploadPostPolicyResponse>>()
            .block() ?: throw RuntimeException("No response from kabal-file-api on upload policy request")
    }

    @Retryable(
        maxRetries = 3,
        delay = 1000,
        multiplier = 2.0,
    )
    fun getDocumentMetadata(id: String): DocumentMetadataResponse {
        logger.debug("Fetching document metadata for id {} from kabal-file-api", id)

        val token = tokenUtil.getOnBehalfOfTokenWithKabalFileApiScope()

        return fileWebClient
            .get()
            .uri { it.path("/document/{id}/metadata").build(id) }
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .onStatus(HttpStatusCode::isError) { clientResponse ->
                clientResponse.bodyToMono<String>().map { body ->
                    logger.error("Error fetching document metadata from kabal-file-api: $body")
                    RuntimeException("Error fetching document metadata from kabal-file-api")
                }
            }
            .bodyToMono<DocumentMetadataResponse>()
            .block() ?: throw RuntimeException("No response from kabal-file-api on metadata request")
    }

    @Retryable(
        excludes = [AttachmentUnsupportedTypeException::class],
        maxRetries = 3,
        delay = 1000,
        multiplier = 2.0,
    )
    fun scanDocument(id: String): ScanResultResponse {
        logger.debug("Requesting virus scan for document with id {} from kabal-file-api", id)

        val token = tokenUtil.getOnBehalfOfTokenWithKabalFileApiScope()

        return fileWebClient
            .post()
            .uri { it.path("/document/{id}/scan").build(id) }
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .onStatus({ it.value() == 422 }) { clientResponse ->
                clientResponse.bodyToMono<String>().map { body ->
                    logger.warn("kabal-file-api rejected the document as an unsupported file type: $body")
                    AttachmentUnsupportedTypeException()
                }
            }
            .onStatus(HttpStatusCode::isError) { clientResponse ->
                clientResponse.bodyToMono<String>().map { body ->
                    logger.error("Error scanning document in kabal-file-api: $body")
                    RuntimeException("Error scanning document in kabal-file-api")
                }
            }
            .bodyToMono<ScanResultResponse>()
            .block() ?: throw RuntimeException("No response from kabal-file-api on scan request")
    }

    @Retryable(
        excludes = [AttachmentUnsupportedTypeException::class, AttachmentConversionFailedException::class],
        maxRetries = 3,
        delay = 1000,
        multiplier = 2.0,
    )
    fun convertDocument(id: String, scannedGeneration: Long): ConvertResultResponse {
        logger.debug("Requesting conversion to PDF for document with id {} from kabal-file-api", id)

        val token = tokenUtil.getOnBehalfOfTokenWithKabalFileApiScope()

        return fileWebClient
            .post()
            .uri { it.path("/document/{id}/convert").build(id) }
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .bodyValue(ConvertRequest(scannedGeneration = scannedGeneration))
            .retrieve()
            .onStatus({ it.value() == 422 }) { clientResponse ->
                clientResponse.bodyToMono<String>().map { body ->
                    logger.warn("kabal-file-api could not convert document to PDF (unsupported type): $body")
                    AttachmentUnsupportedTypeException()
                }
            }
            //The object was replaced in the bucket after we scanned it, so the generation we asked to
            //convert no longer exists. Not a problem with the file type: the document has to go
            //through a fresh scan to get a new generation, which is what UNEXPECTED_ERROR resets to.
            .onStatus({ it.value() == 409 }) { clientResponse ->
                clientResponse.bodyToMono<String>().map { body ->
                    logger.warn("Document was replaced after it was scanned, cannot convert it: $body")
                    AttachmentConversionFailedException()
                }
            }
            .onStatus({ it.value() == 500 }) { clientResponse ->
                clientResponse.bodyToMono<String>().map { body ->
                    logger.error("kabal-file-api reported an unexpected conversion failure: $body")
                    AttachmentConversionFailedException()
                }
            }
            .onStatus(HttpStatusCode::isError) { clientResponse ->
                clientResponse.bodyToMono<String>().map { body ->
                    logger.error("Error converting document in kabal-file-api: $body")
                    RuntimeException("Error converting document in kabal-file-api")
                }
            }
            .bodyToMono<ConvertResultResponse>()
            .block() ?: throw RuntimeException("No response from kabal-file-api on convert request")
    }

    @Retryable(
        maxRetries = 3,
        delay = 1000,
        multiplier = 2.0,
    )
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
                clientResponse.bodyToMono<String>().map { body ->
                    logger.error("Error requesting view URL from kabal-file-api: $body")
                    RuntimeException("Error requesting view URL from kabal-file-api")
                }
            }
            .bodyToMono<String>()
            .block() ?: throw RuntimeException("No response from kabal-file-api on view URL request")
    }

    @Retryable(
        maxRetries = 3,
        delay = 1000,
        multiplier = 2.0,
    )
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
                    clientResponse.bodyToMono<String>().map { body ->
                        logger.error("Error deleting document from kabal-file-api: $body")
                        RuntimeException("Error deleting document from kabal-file-api")
                    }
                }
                .toBodilessEntity()
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

data class UploadUrlsRequest(
    val contentTypes: List<String>,
)

data class DocumentMetadataResponse(
    val exists: Boolean,
    val size: Long?,
    val contentType: String?,
)

data class ScanResultResponse(
    val hasVirus: Boolean,
    val size: Long?,
    val contentType: String?,
    val requiresConversion: Boolean,
    val generation: Long,
)

data class ConvertRequest(
    val scannedGeneration: Long,
)

data class ConvertResultResponse(
    val size: Long?,
    val contentType: String?,
    val wasConverted: Boolean,
)

data class SignedUrlRequest(
    val headers: Map<String, String> = emptyMap(),
)
