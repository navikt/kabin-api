package no.nav.klage.clients.oppgaveapi

data class GjelderResponse(
    val gjelder: List<Gjelder>,
)

data class Gjelder(
    val behandlingsTema: String?,
    val behandlingstemaTerm : String?,
    val behandlingstype: String?,
    val behandlingstypeTerm: String?,
)
