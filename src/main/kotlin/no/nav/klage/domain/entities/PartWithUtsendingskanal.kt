package no.nav.klage.domain.entities

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded

@Embeddable
class PartWithUtsendingskanal(
    @Embedded
    val part: PartId,
    @Embedded
    val address: Address?,
    @Column(name = "name")
    val name: String,
    @Column(name = "available")
    val available: Boolean?,
    @Column(name = "language")
    val language: String?,
    @Column(name = "utsendingskanal")
    val utsendingskanal: Utsendingskanal?,
) {
    enum class Utsendingskanal(val navn: String) {
        SENTRAL_UTSKRIFT("Sentral utskrift"),
        SDP("Digital Postkasse Innbygger"),
        NAV_NO("Nav.no"),
        LOKAL_UTSKRIFT("Lokal utskrift"),
        INGEN_DISTRIBUSJON("Ingen distribusjon"),
        TRYGDERETTEN("Trygderetten"),
        DPVT("Taushetsbelagt digital post til virksomhet")
    }
}