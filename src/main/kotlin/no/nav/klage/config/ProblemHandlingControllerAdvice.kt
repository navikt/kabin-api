package no.nav.klage.config

import no.nav.klage.exceptions.*
import no.nav.klage.util.getLogger
import no.nav.klage.util.getTeamLogger
import org.springframework.http.*
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class ProblemHandlingControllerAdvice : ResponseEntityExceptionHandler() {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val ourLogger = getLogger(javaClass.enclosingClass)
        private val teamLogger = getTeamLogger()
    }

    /* Override to get better info when client gets 400-error */
    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val body = create(httpStatus = HttpStatus.valueOf(status.value()), ex = ex)
        return handleExceptionInternal(ex, body, headers, status, request)
    }

    @ExceptionHandler
    fun handleResponseStatusException(ex: WebClientResponseException): ResponseEntity<Any> =
        createProblemForWebClientResponseException(ex)

    @ExceptionHandler
    fun handleJournalpostNotFoundException(
        ex: JournalpostNotFoundException,
    ): ProblemDetail =
        create(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler
    fun handleRegistreringNotFoundException(
        ex: RegistreringNotFoundException,
    ): ProblemDetail =
        create(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler
    fun handleMulighetNotFoundException(
        ex: MulighetNotFoundException,
    ): ProblemDetail =
        create(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler
    fun handleGosysOppgaveClientException(
        ex: GosysOppgaveClientException,
    ): ProblemDetail =
        create(HttpStatus.INTERNAL_SERVER_ERROR, ex)

    @ExceptionHandler
    fun handleInvalidSourceException(
        ex: InvalidSourceException,
    ): ProblemDetail =
        create(HttpStatus.BAD_REQUEST, ex)

    @ExceptionHandler
    fun handleMissingAccessException(
        ex: MissingAccessException,
    ): ProblemDetail =
        create(HttpStatus.FORBIDDEN, ex)

    @ExceptionHandler
    fun handleIllegalUpdateException(
        ex: IllegalUpdateException,
    ): ProblemDetail =
        create(HttpStatus.BAD_REQUEST, ex)

    @ExceptionHandler
    fun handleIllegalInputException(
        ex: IllegalInputException,
    ): ProblemDetail =
        create(HttpStatus.BAD_REQUEST, ex)

    @ExceptionHandler
    fun handleRuntimeException(
        ex: RuntimeException,
    ): ProblemDetail =
        create(HttpStatus.INTERNAL_SERVER_ERROR, ex)

    @ExceptionHandler
    fun handleSectionedValidationErrorWithDetailsException(
        ex: SectionedValidationErrorWithDetailsException,
    ): ProblemDetail =
        createSectionedValidationProblem(ex)

    private fun createProblemForWebClientResponseException(ex: WebClientResponseException): ResponseEntity<Any> {
        logError(
            httpStatus = HttpStatus.valueOf(ex.statusCode.value()),
            errorMessage = ex.statusText,
            exception = ex
        )

        val contentType = ex.headers.contentType
        if (contentType != null && MediaType.APPLICATION_PROBLEM_JSON.isCompatibleWith(contentType)) {
            logger.debug("Upstream returned problem+json compatible error, passing through as-is.")
            // Pass through as-is when upstream already returned problem+json
            val body = ex.responseBodyAsByteArray
            return ResponseEntity.status(ex.statusCode).contentType(contentType).body(body)
        }

        // Fallback: wrap into a ProblemDetail
        val problemDetail = ProblemDetail.forStatus(ex.statusCode).apply {
            title = ex.statusText
            detail = ex.responseBodyAsString
        }
        return ResponseEntity
            .status(ex.statusCode)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problemDetail)
    }

    private fun createSectionedValidationProblem(ex: SectionedValidationErrorWithDetailsException): ProblemDetail {
        logError(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorMessage = ex.title,
            exception = ex
        )

        return ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
            this.title = ex.title
            this.setProperty("sections", ex.sections)
        }
    }

    private fun create(httpStatus: HttpStatus, ex: Exception): ProblemDetail {
        val errorMessage = ex.message ?: "No error message available"

        logError(
            httpStatus = httpStatus,
            errorMessage = errorMessage,
            exception = ex
        )

        return ProblemDetail.forStatusAndDetail(httpStatus, errorMessage).apply {
            title = errorMessage
        }
    }

    private fun logError(httpStatus: HttpStatus, errorMessage: String, exception: Exception) {
        when {
            httpStatus.is5xxServerError -> {
                ourLogger.error("Exception thrown to client: ${exception.javaClass.name}. See team-logs for more details.")
                teamLogger.error("Exception thrown to client: ${httpStatus.reasonPhrase}, $errorMessage", exception)
            }

            else -> {
                ourLogger.warn("Exception thrown to client: ${exception.javaClass.name}. See team-logs for more details.")
                teamLogger.warn("Exception thrown to client: ${httpStatus.reasonPhrase}, $errorMessage", exception)
            }
        }
    }
}
