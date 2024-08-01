package no.nav.klage.domain.entities

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class Address(
    @Column(name = "adresselinje1")
    var adresselinje1: String?,
    @Column(name = "adresselinje2")
    var adresselinje2: String?,
    @Column(name = "adresselinje3")
    var adresselinje3: String?,
    @Column(name = "postnummer")
    var postnummer: String?,
    @Column(name = "landkode")
    var landkode: String?
)