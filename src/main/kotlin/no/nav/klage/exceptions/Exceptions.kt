package no.nav.klage.exceptions

data class InvalidProperty(val field: String, val reason: String)

class SectionedValidationErrorWithDetailsException(val title: String, val sections: List<ValidationSection>) :
    RuntimeException()

data class ValidationSection(val section: String, val properties: List<InvalidProperty>)

class JournalpostNotFoundException(override val message: String): RuntimeException(message)

class InvalidSourceException(override val message: String): RuntimeException(message)

class IllegalUpdateException(override val message: String): RuntimeException(message)

class ReceiverNotFoundException(override val message: String): RuntimeException(message)

class IllegalInputException(override val message: String): RuntimeException(message)

class EnhetNotFoundForSaksbehandlerException(msg: String) : RuntimeException(msg)

class RegistreringNotFoundException(msg: String) : RuntimeException(msg)

class MissingAccessException(msg: String) : RuntimeException(msg)

class MulighetNotFoundException(msg: String) : RuntimeException(msg)

class GosysOppgaveClientException : RuntimeException {
    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)
}

class UserNotFoundException(msg: String) : RuntimeException(msg)

class AttachmentTooLargeException(override val message: String = "TOO_LARGE") : RuntimeException()

class AttachmentHasVirusException(override val message: String = "VIRUS") : RuntimeException()

class AttachmentCouldNotBeConvertedException(override val message: String = "FILE_COULD_NOT_BE_CONVERTED") : RuntimeException()

class AttachmentCouldNotBeScannedException(override val message: String = "FILE_COULD_NOT_BE_SCANNED") : RuntimeException()