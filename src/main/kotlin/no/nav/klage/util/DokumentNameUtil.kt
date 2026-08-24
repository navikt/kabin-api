package no.nav.klage.util

import no.nav.klage.domain.entities.RegistreringDokument
import no.nav.klage.exceptions.IllegalInputException

/**
 * Returns [name] with a ".pdf" extension. Used when serving a stored document, which is always a PDF,
 * so the filename has to describe the file we actually have rather than whatever the user typed.
 * If the name already ends with ".pdf", it is returned as is. The result is only used as a filename in
 * a response header, so it is not length limited.
 */
fun withPdfExtension(name: String): String {
    val trimmed = name.trim().ifBlank { "dokument" }
    return if (trimmed.endsWith(".pdf", ignoreCase = true)) {
        trimmed
    } else {
        "$trimmed.pdf"
    }
}

fun validateDokumentName(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) {
        throw IllegalInputException("Dokumentnavnet kan ikke være tomt.")
    }
    if (trimmed.length > RegistreringDokument.MAX_NAME_LENGTH) {
        throw IllegalInputException(
            "Dokumentnavnet kan ikke være lenger enn ${RegistreringDokument.MAX_NAME_LENGTH} tegn."
        )
    }
    return trimmed
}
