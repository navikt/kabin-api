package no.nav.klage.clients.oppgaveapi

data class Gjelder(
    val behandlingsTema: String?,
    val behandlingstemaTerm : String?,
    val behandlingstype: String?,
    val behandlingstypeTerm: String?,
)

data class OppgavetypeResponse(
    val oppgavetype: String,
    val term: String,
)
