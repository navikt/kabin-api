package no.nav.klage.clients.klanke

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

data class KlankeSearchInput(
    val fnr: String,
    val sakstype: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SakFromKlanke(
    val sakId: String,
    val fagsakId: String,
    val tema: String,
    val enhetsnummer: String,
    val vedtaksdato: LocalDate,
    val fnr: String,
    val sakstype: String,
)

data class HandledInKabalInput(
    val fristAsString: String
)

data class Access(
    val access: Boolean
)

data class GetSakAppAccessInput(
    val saksbehandlerIdent: String
)
