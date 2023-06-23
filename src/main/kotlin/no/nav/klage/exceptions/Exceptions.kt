package no.nav.klage.exceptions

data class InvalidProperty(val field: String, val reason: String)

class SectionedValidationErrorWithDetailsException(val title: String, val sections: List<ValidationSection>) :
    RuntimeException()

data class ValidationSection(val section: String, val properties: List<InvalidProperty>)

class JournalpostNotFoundException(override val message: String): RuntimeException(message)