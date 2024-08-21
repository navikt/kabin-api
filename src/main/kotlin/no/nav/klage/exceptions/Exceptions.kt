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