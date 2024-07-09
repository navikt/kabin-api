package no.nav.klage.api.controller.view

import no.nav.klage.domain.BehandlingstidUnitType

data class SvarbrevWithReceiverInput(
    val title: String,
    val fullmektigFritekst: String?,
    val receivers: List<Receiver>,
    val varsletBehandlingstidUnits: Int,
    val varsletBehandlingstidUnitType: BehandlingstidUnitType,
) {

    data class Receiver(
        val id: String,
        val handling: HandlingEnum,
        val overriddenAddress: AddressInput?,
    ) {
        data class AddressInput(
            val adresselinje1: String?,
            val adresselinje2: String?,
            val adresselinje3: String?,
            val landkode: String,
            val postnummer: String?,
        )

        enum class HandlingEnum {
            AUTO,
            LOCAL_PRINT,
            CENTRAL_PRINT
        }
    }
}