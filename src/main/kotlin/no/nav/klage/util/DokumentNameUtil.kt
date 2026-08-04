package no.nav.klage.util

import no.nav.klage.domain.entities.RegistreringDokument
import no.nav.klage.exceptions.IllegalInputException

/**
 * Returns [name] with its file extension replaced by [extension]. This is applied by the API itself
 * (and not by a client that could pick a shorter name), so the base name is truncated rather than
 * rejected when the result would be too long.
 */
fun withExtension(name: String, extension: String): String {
    val suffix = ".$extension"
    val base = name.dropExtension().trim().ifBlank { "dokument" }
    val maxBaseLength = RegistreringDokument.MAX_NAME_LENGTH - suffix.length

    return base.take(maxBaseLength).trimEnd() + suffix
}

/**
 * Returns [newName] with the file extension of [currentName] applied. Clients are free to rename a
 * document, but the extension has to keep describing the file we actually have stored, so it is never
 * taken from the client input.
 */
fun renameKeepingExtension(currentName: String, newName: String): String {
    val extension = currentName.substringAfterLast('.', "")
    val trimmed = newName.trim()
    val base = trimmed.dropExtension().trim()

    if (base.isEmpty()) {
        throw IllegalInputException("Dokumentnavnet kan ikke være tomt.")
    }

    return if (extension.isEmpty()) {
        validateDokumentName(base)
    } else {
        validateDokumentName("$base.$extension")
    }
}

//The only extensions a document here can have: uploads are limited to these content types, and
//anything that reaches DONE is a PDF.
private val KNOWN_EXTENSIONS = setOf("pdf", "jpg", "jpeg", "png", "tif", "tiff")

/**
 * Drops a trailing file extension, if there is one. Only a known extension counts, so that names that
 * just happen to contain a dot ("Rapport v1.2") keep all of their parts.
 */
private fun String.dropExtension(): String {
    val separatorIndex = lastIndexOf('.')
    if (separatorIndex <= 0) {
        return this
    }

    return if (substring(separatorIndex + 1).lowercase() in KNOWN_EXTENSIONS) {
        substring(0, separatorIndex)
    } else {
        this
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
