package no.nav.klage.api.controller.view

import java.time.LocalDate

data class PartView(
    val id: String,
    val name: String,
    val type: PartType,
    val available: Boolean,
    val language: String?,
    val statusList: List<PartStatus>,
    val address: Address?,
    val utsendingskanal: Utsendingskanal,
) {
    enum class PartType {
        FNR, ORGNR
    }

    data class PartStatus(
        val status: Status,
        val date: LocalDate?,
    ) {
        enum class Status {
            DEAD,
            DELETED,
            FORTROLIG,
            STRENGT_FORTROLIG,
            EGEN_ANSATT,
            VERGEMAAL,
            FULLMAKT,
            RESERVERT_I_KRR,
            DELT_ANSVAR,
        }
    }

    data class Address(
        val adresselinje1: String?,
        val adresselinje2: String?,
        val adresselinje3: String?,
        val landkode: String,
        val postnummer: String?,
        val poststed: String?,
    )

}

enum class Utsendingskanal(val navn: String) {
    SENTRAL_UTSKRIFT("Sentral utskrift"),
    SDP("Digital Postkasse Innbygger"),
    NAV_NO("Nav.no"),
    LOKAL_UTSKRIFT("Lokal utskrift"),
    INGEN_DISTRIBUSJON("Ingen distribusjon"),
    TRYGDERETTEN("Trygderetten"),
    DPVT("Taushetsbelagt digital post til virksomhet")
}
