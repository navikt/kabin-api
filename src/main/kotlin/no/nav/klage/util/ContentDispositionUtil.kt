package no.nav.klage.util

import org.springframework.http.ContentDisposition
import java.nio.charset.StandardCharsets

private const val EXTENDED_FILENAME_SEPARATOR = "; filename*="

/**
 * Builds a Content-Disposition header value where the filename is properly escaped and encoded.
 * Interpolating the filename directly into the header breaks the header when the filename contains
 * a double quote or a backslash, and non-ASCII characters are not allowed in header values at all.
 *
 * The value is also sent to GCS as a signed URL query parameter. GCS rejects the signature when the
 * ASCII fallback (filename="...") contains a raw '%', so it is replaced there. The exact name is kept
 * in filename*, which browsers prefer.
 */
fun contentDispositionHeaderValue(
    filename: String,
    type: String,
): String {
    val fallback =
        buildContentDisposition(filename = filename.replace(oldChar = '%', newChar = '_'), type = type)
            .substringBefore(EXTENDED_FILENAME_SEPARATOR)
    val extended =
        buildContentDisposition(filename = filename, type = type)
            .substringAfter(EXTENDED_FILENAME_SEPARATOR)
    return "$fallback$EXTENDED_FILENAME_SEPARATOR$extended"
}

private fun buildContentDisposition(
    filename: String,
    type: String,
): String =
    ContentDisposition
        .builder(type)
        .filename(filename, StandardCharsets.UTF_8)
        .build()
        .toString()
