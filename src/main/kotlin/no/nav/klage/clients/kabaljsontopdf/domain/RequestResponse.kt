package no.nav.klage.clients.kabaljsontopdf.domain

import java.time.LocalDate

data class SvarbrevRequest(
    val sakenGjelder: SakenGjelder,
    val enhetsnavn: String,
    val fullmektigFritekst: String?,
    val ankeReceivedDate: LocalDate,
    val behandlingstidInWeeks: Int,
) {
    data class SakenGjelder(
        val name: String,
        val fnr: String,
    )
}

