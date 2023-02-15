package no.nav.klage.config

import no.nav.klage.exceptions.SectionedValidationErrorWithDetailsException
import no.nav.klage.util.getSecureLogger
import org.springframework.http.*
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class ProblemHandlingControllerAdvice : ResponseEntityExceptionHandler() {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val secureLogger = getSecureLogger()
    }

    @ExceptionHandler
    fun handleResponseStatusException(
        ex: WebClientResponseException,
        request: NativeWebRequest
    ): ProblemDetail =
        createProblemForWebClientResponseException(ex)


    @ExceptionHandler
    fun handleSectionedValidationErrorWithDetailsException(
        ex: SectionedValidationErrorWithDetailsException,
        request: NativeWebRequest
    ): ProblemDetail =
        createSectionedValidationProblem(ex)

    private fun createProblemForWebClientResponseException(ex: WebClientResponseException): ProblemDetail {
        logError(
            httpStatus = HttpStatus.valueOf(ex.statusCode.value()),
            errorMessage = ex.statusText,
            exception = ex
        )

        return ProblemDetail.forStatus(ex.statusCode).apply {
            title = ex.statusText
            detail = ex.responseBodyAsString
        }
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
                secureLogger.error("Exception thrown to client: ${httpStatus.reasonPhrase}, $errorMessage", exception)
            }

            else -> {
                secureLogger.warn("Exception thrown to client: ${httpStatus.reasonPhrase}, $errorMessage", exception)
            }
        }
    }
}
