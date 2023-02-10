package no.nav.klage.clients.dokarkiv

data class UpdateJournalpostSaksIdRequest(
    val sak: Sak
)

data class Sak(
    val sakstype: Sakstype,
    val fagsaksystem: FagsaksSystem? = null,
    val fagsakid: String? = null,
)

enum class Sakstype {
    FAGSAK,
    GENERELL_SAK,
    ARKIVSAK
}

enum class FagsaksSystem {
    AO01,
    AO11,
    BISYS,
    FS36,
    FS38,
    IT01,
    K9,
    OB36,
    OEBS,
    PP01,
    UFM,
    BA,
    EF,
    KONT,
    SUPSTONAD,
    OMSORGSPENGER
}