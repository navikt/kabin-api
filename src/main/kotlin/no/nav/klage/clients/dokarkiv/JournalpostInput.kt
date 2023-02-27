package no.nav.klage.clients.dokarkiv

import no.nav.klage.kodeverk.Tema

enum class JournalpostType {
    INNGAAENDE,
    UTGAAENDE,
    NOTAT
}

data class AvsenderMottaker(
    val id: String,
    val idType: AvsenderMottakerIdType,
    val navn: String? = null,
    val land: String? = null
)

enum class AvsenderMottakerIdType {
    FNR,
    ORGNR,
    HPRNR,
    UTL_ORG
}

data class UpdateDocumentTitleJournalpostInput(
    val dokumenter: List<UpdateDocumentTitleDokumentInput>,
)

data class UpdateDocumentTitleDokumentInput(
    val dokumentInfoId: String,
    val tittel: String,
)

data class CreateNewJournalpostBasedOnExistingJournalpostRequest(
    val sakstype: Sakstype,
    val fagsakId: String,
    val fagsaksystem: FagsaksSystem,
    val tema: Tema,
    val bruker: Bruker,
    val journalfoerendeEnhet: String
)

data class UpdateJournalpostSaksIdRequest(
    val tema: Tema,
    val bruker: Bruker,
    val sak: Sak,
    val journalfoerendeEnhet: String,
)

data class Bruker(
    val id: String,
    val idType: BrukerIdType
)

enum class BrukerIdType {
    FNR,
    ORGNR,
    AKTOERID
}

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