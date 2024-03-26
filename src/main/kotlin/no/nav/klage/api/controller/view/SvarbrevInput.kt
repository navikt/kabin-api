package no.nav.klage.api.controller.view

data class SvarbrevInput(
    val title: String = "Anke - orientering om saksbehandlingstid",
    val receivers: List<Receiver>,
    val enhetId: String,
    val fullmektigFritekst: String?,
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