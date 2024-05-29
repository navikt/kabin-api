package no.nav.klage.clients.kabaljsontopdf.domain

import java.time.LocalDate

data class SvarbrevRequest(
    val title: String,
    val sakenGjelder: Part,
    val klager: Part?,
    val ytelsenavn: String,
    val fullmektigFritekst: String?,
    val ankeReceivedDate: LocalDate,
    val behandlingstidInWeeks: Int,
    val avsenderEnhetId: String,
) {
    data class Part(
        val name: String,
        val fnr: String,
    )
}

