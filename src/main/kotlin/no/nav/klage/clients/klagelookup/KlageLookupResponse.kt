package no.nav.klage.clients.klagelookup

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExtendedUserResponse (
    val navIdent: String,
    val sammensattNavn: String,
    val fornavn: String,
    val etternavn: String,
    val enhet: Enhet,
)

data class Enhet (
    val enhetNr: String,
    val enhetNavn: String,
)