package no.nav.klage.domain.entities

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class Address(
    @Column(name = "adresselinje1")
    val adresselinje1: String?,
    @Column(name = "adresselinje2")
    val adresselinje2: String?,
    @Column(name = "adresselinje3")
    val adresselinje3: String?,
    @Column(name = "postnummer")
    val postnummer: String?,
    @Column(name = "landkode")
    val landkode: String?
)