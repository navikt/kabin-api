package no.nav.klage.config

import no.nav.klage.exceptions.AttachmentHasVirusException
import no.nav.klage.exceptions.AttachmentTooLargeException
import no.nav.klage.exceptions.AttachmentUnsupportedTypeException
import no.nav.klage.exceptions.GosysOppgaveClientException
import no.nav.klage.exceptions.IllegalInputException
import no.nav.klage.exceptions.IllegalUpdateException
import no.nav.klage.exceptions.InvalidSourceException
import no.nav.klage.exceptions.JournalpostNotFoundException
import no.nav.klage.exceptions.MissingAccessException
import no.nav.klage.exceptions.MulighetNotFoundException
import no.nav.klage.exceptions.RegistreringNotFoundException
import no.nav.klage.exceptions.SectionedValidationErrorWithDetailsException
import no.nav.klage.exceptions.UserNotFoundException
import no.nav.klage.util.getLogger
import no.nav.klage.util.getTeamLogger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
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

    // Override to get better info when client gets 400-error
    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val body = create(httpStatus = HttpStatus.valueOf(status.value()), ex = ex)
        return handleExceptionInternal(ex, body, headers, status, request)
    }

    @ExceptionHandler
    fun handleResponseStatusException(ex: WebClientResponseException): ResponseEntity<Any> = createProblemForWebClientResponseException(ex)

    @ExceptionHandler
    fun handleJournalpostNotFoundException(ex: JournalpostNotFoundException): ProblemDetail =
        create(httpStatus = HttpStatus.NOT_FOUND, ex = ex)

    @ExceptionHandler
    fun handleRegistreringNotFoundException(ex: RegistreringNotFoundException): ProblemDetail =
        create(httpStatus = HttpStatus.NOT_FOUND, ex = ex)

    @ExceptionHandler
    fun handleAttachmentTooLargeException(ex: AttachmentTooLargeException): ProblemDetail =
        create(httpStatus = HttpStatus.CONTENT_TOO_LARGE, ex = ex)

    @ExceptionHandler
    fun handleAttachmentHasVirusException(ex: AttachmentHasVirusException): ProblemDetail =
        create(httpStatus = HttpStatus.BAD_REQUEST, ex = ex)

    @ExceptionHandler
    fun handleAttachmentUnsupportedTypeException(ex: AttachmentUnsupportedTypeException): ProblemDetail =
        create(httpStatus = HttpStatus.UNPROCESSABLE_CONTENT, ex = ex)

    @ExceptionHandler
    fun handleMulighetNotFoundException(ex: MulighetNotFoundException): ProblemDetail = create(httpStatus = HttpStatus.NOT_FOUND, ex = ex)

    @ExceptionHandler
    fun handleGosysOppgaveClientException(ex: GosysOppgaveClientException): ProblemDetail =
        create(httpStatus = HttpStatus.INTERNAL_SERVER_ERROR, ex = ex)

    @ExceptionHandler
    fun handleInvalidSourceException(ex: InvalidSourceException): ProblemDetail = create(httpStatus = HttpStatus.BAD_REQUEST, ex = ex)

    @ExceptionHandler
    fun handleMissingAccessException(ex: MissingAccessException): ProblemDetail = create(httpStatus = HttpStatus.FORBIDDEN, ex = ex)

    @ExceptionHandler
    fun handleIllegalUpdateException(ex: IllegalUpdateException): ProblemDetail = create(httpStatus = HttpStatus.BAD_REQUEST, ex = ex)

    @ExceptionHandler
    fun handleIllegalInputException(ex: IllegalInputException): ProblemDetail = create(httpStatus = HttpStatus.BAD_REQUEST, ex = ex)

    @ExceptionHandler
    fun handleRuntimeException(ex: RuntimeException): ProblemDetail = create(httpStatus = HttpStatus.INTERNAL_SERVER_ERROR, ex = ex)

    @ExceptionHandler
    fun handleSectionedValidationErrorWithDetailsException(ex: SectionedValidationErrorWithDetailsException): ProblemDetail =
        createSectionedValidationProblem(ex)

    @ExceptionHandler
    fun handleUserNotFoundException(ex: UserNotFoundException): ProblemDetail = create(httpStatus = HttpStatus.NOT_FOUND, ex = ex)

    private fun createProblemForWebClientResponseException(ex: WebClientResponseException): ResponseEntity<Any> {
        logError(
            httpStatus = HttpStatus.valueOf(ex.statusCode.value()),
            errorMessage = ex.statusText,
            exception = ex,
        )

        val contentType = ex.headers.contentType
        if (contentType != null && MediaType.APPLICATION_PROBLEM_JSON.isCompatibleWith(contentType)) {
            logger.debug("Upstream returned problem+json compatible error, passing through as-is.")
            // Pass through as-is when upstream already returned problem+json
            val body = ex.responseBodyAsByteArray
            return ResponseEntity.status(ex.statusCode).contentType(contentType).body(body)
        }

        // Fallback: wrap into a ProblemDetail
        val problemDetail =
            ProblemDetail.forStatus(ex.statusCode).apply {
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
            exception = ex,
        )

        return ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
            this.title = ex.title
            this.setProperty("sections", ex.sections)
        }
    }

    private fun create(
        httpStatus: HttpStatus,
        ex: Exception,
    ): ProblemDetail {
        val errorMessage = ex.message ?: "No error message available"

        logError(
            httpStatus = httpStatus,
            errorMessage = errorMessage,
            exception = ex,
        )

        return ProblemDetail.forStatusAndDetail(httpStatus, errorMessage).apply {
            title = errorMessage
        }
    }

    private fun logError(
        httpStatus: HttpStatus,
        errorMessage: String,
        exception: Exception,
    ) {
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
