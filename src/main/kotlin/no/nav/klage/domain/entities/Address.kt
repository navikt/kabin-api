package no.nav.klage.domain.entities

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class Address(
    @Column(name = "adresslinje1")
    val addressLine1: String?,
    @Column(name = "postnummer")
    val postnummer: String?,
    @Column(name = "landkode")
    val landkode: String?
)