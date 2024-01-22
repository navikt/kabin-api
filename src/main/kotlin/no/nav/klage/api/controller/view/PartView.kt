package no.nav.klage.api.controller.view

import java.time.LocalDate

data class PartView(
    val id: String,
    val type: PartType,
    val name: String?,
    val available: Boolean,
    val statusList: List<PartStatus>,
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
        }
    }
}
