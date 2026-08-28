package no.nav.klage.exceptions

data class InvalidProperty(
    val field: String,
    val reason: String,
)

class SectionedValidationErrorWithDetailsException(
    val title: String,
    val sections: List<ValidationSection>,
) : RuntimeException()

data class ValidationSection(
    val section: String,
    val properties: List<InvalidProperty>,
)

class JournalpostNotFoundException(
    override val message: String,
) : RuntimeException(message)

class InvalidSourceException(
    override val message: String,
) : RuntimeException(message)

class IllegalUpdateException(
    override val message: String,
) : RuntimeException(message)

class ReceiverNotFoundException(
    override val message: String,
) : RuntimeException(message)

class IllegalInputException(
    override val message: String,
) : RuntimeException(message)

class EnhetNotFoundForSaksbehandlerException(
    msg: String,
) : RuntimeException(msg)

class RegistreringNotFoundException(
    msg: String,
) : RuntimeException(msg)

class MissingAccessException(
    msg: String,
) : RuntimeException(msg)

class MulighetNotFoundException(
    msg: String,
) : RuntimeException(msg)

class GosysOppgaveClientException : RuntimeException {
    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)
}

class UserNotFoundException(
    msg: String,
) : RuntimeException(msg)

class AttachmentTooLargeException(
    override val message: String = "TOO_LARGE",
) : RuntimeException()

class AttachmentHasVirusException(
    override val message: String = "VIRUS",
) : RuntimeException()

/**
 * kabal-file-api rejected the file as a type it cannot turn into a PDF. Reported by both the scan
 * and the convert step, since the type is checked in both. Retrying is pointless.
 */
class AttachmentUnsupportedTypeException(
    override val message: String = "FILE_TYPE_NOT_SUPPORTED",
) : RuntimeException()

/**
 * kabal-file-api failed unexpectedly while converting a file whose type it does support. Distinct
 * from [AttachmentUnsupportedTypeException]: nothing is wrong with the file type as such, so this is
 * reported as an unexpected error rather than as an unsupported type.
 */
class AttachmentConversionFailedException(
    override val message: String = "FILE_CONVERSION_FAILED",
) : RuntimeException()
