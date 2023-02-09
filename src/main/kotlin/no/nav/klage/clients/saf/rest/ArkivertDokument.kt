package no.nav.klage.clients.saf.rest

import org.springframework.http.MediaType

data class ArkivertDokument(
    val bytes: ByteArray,
    val contentType: MediaType
)
