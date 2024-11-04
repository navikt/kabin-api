package no.nav.klage.clients.gosysoppgave

data class Gjelder(
    val behandlingsTema: String?,
    val behandlingstemaTerm : String?,
    val behandlingstype: String?,
    val behandlingstypeTerm: String?,
)

data class GosysOppgavetypeResponse(
    val oppgavetype: String,
    val term: String,
)
