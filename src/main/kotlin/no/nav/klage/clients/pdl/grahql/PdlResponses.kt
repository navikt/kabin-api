package no.nav.klage.clients.pdl.grahql

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class HentIdenterResponse(val data: HentIdenterDataWrapper?, val errors: List<PdlError>? = null)

data class HentIdenterDataWrapper(val hentIdenter: Identer)

data class Identer(
    val identer: List<Ident>,
)

data class Ident(
    val ident: String,
    val gruppe: IdentGruppe,
)