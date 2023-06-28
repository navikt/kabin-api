package no.nav.klage.api.controller.view

data class PartView(
    val id: String,
    val type: PartType,
    val name: String?,
    val available: Boolean,
) {
    enum class PartType {
        FNR, ORGNR
    }
}
