package no.nav.klage.api.controller.view

import no.nav.klage.domain.entities.HandlingEnum
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class FullRegistreringView(
    val id: UUID,
    val sakenGjelderValue: String?,
    val journalpostId: String?,
    val typeId: String?,
    val mulighet: MulighetView?,
    val overstyringer: OverstyringerView,
    val svarbrev: SvarbrevView,
    val created: LocalDateTime,
    val modified: LocalDateTime,
    val createdBy: String,
    val finished: LocalDateTime?,
    val behandlingId: UUID?,
) {

    data class OverstyringerView(
        val mottattVedtaksinstans: LocalDate?,
        val mottattKlageinstans: LocalDate?,
        val behandlingstid: BehandlingstidView,
        val calculatedFrist: LocalDate?,
        val hjemmelIdList: List<String>,
        val ytelseId: String?,
        val fullmektig: PartViewWithUtsendingskanal?,
        val klager: PartViewWithUtsendingskanal?,
        val avsender: PartViewWithUtsendingskanal?,
        val saksbehandlerIdent: String?,
        val oppgaveId: Int?
    )

    data class SvarbrevView(
        val send: Boolean?,
        val behandlingstid: BehandlingstidView?,
        val calculatedFrist: LocalDate?,
        val fullmektigFritekst: String?,
        val receivers: List<RecipientView>,
        val title: String?,
        val overrideCustomText: Boolean,
        val overrideBehandlingstid: Boolean,
        val customText: String?
    ) {
        data class RecipientView(
            val part: PartViewWithUtsendingskanal,
            val handling: HandlingEnum,
            val overriddenAddress: AddressView?
        ) {
            data class AddressView(
                val adresselinje1: String?,
                val adresselinje2: String?,
                val adresselinje3: String?,
                val landkode: String?,
                val postnummer: String?
            )
        }
    }
}

data class TypeChangeRegistreringView(
    val id: UUID,
    val typeId: String?,
    val mulighet: MulighetView? = null,
    val overstyringer: OverstyringerView,
    val svarbrev: SvarbrevView,
    val modified: LocalDateTime,
) {

    data class OverstyringerView(
        val mottattVedtaksinstans: LocalDate? = null,
        val mottattKlageinstans: LocalDate? = null,
        val behandlingstid: BehandlingstidView? = null,
        val calculatedFrist: LocalDate? = null,
        val hjemmelIdList: List<String> = emptyList(),
        val ytelseId: String? = null,
        val fullmektig: PartViewWithUtsendingskanal? = null,
        val klager: PartViewWithUtsendingskanal? = null,
        val avsender: PartViewWithUtsendingskanal? = null,
        val saksbehandlerIdent: String? = null,
        val oppgaveId: String? = null
    )

    data class SvarbrevView(
        val send: Boolean? = null,
        val behandlingstid: BehandlingstidView? = null,
        val fullmektigFritekst: String? = null,
        val receivers: List<RecipientView> = emptyList(),
        val title: String? = null,
        val overrideCustomText: Boolean = false,
        val overrideBehandlingstid: Boolean = false,
        val customText: String? = null,
    ) {
        data class RecipientView(
            val part: PartViewWithUtsendingskanal,
            val handling: HandlingEnum,
            val overriddenAddress: AddressView?
        ) {
            data class AddressView(
                val adresselinje1: String?,
                val adresselinje2: String?,
                val adresselinje3: String?,
                val landkode: String?,
                val postnummer: String?
            )
        }
    }
}

data class MulighetView(
    val id: String,
    val fagsystemId: String,
)

data class BehandlingstidView(
    val unitTypeId: String,
    val units: Int,
)

data class MulighetChangeRegistreringView(
    val id: UUID,
    val mulighet: MulighetView?,
    val overstyringer: OverstyringerView,
    val svarbrev: SvarbrevView,
    val modified: LocalDateTime,
) {

    data class OverstyringerView(
        val mottattVedtaksinstans: LocalDate? = null,
        val mottattKlageinstans: LocalDate? = null,
        val behandlingstid: BehandlingstidView? = null,
        val calculatedFrist: LocalDate? = null,
        val hjemmelIdList: List<String> = emptyList(),
        val ytelseId: String? = null,
        val fullmektig: PartViewWithUtsendingskanal? = null,
        val klager: PartViewWithUtsendingskanal? = null,
        val avsender: PartViewWithUtsendingskanal? = null,
        val saksbehandlerIdent: String? = null,
        val oppgaveId: Int? = null
    )

    data class SvarbrevView(
        val send: Boolean? = null,
        val behandlingstid: BehandlingstidView? = null,
        val fullmektigFritekst: String? = null,
        val receivers: List<RecipientView> = emptyList(),
        val title: String? = null,
        val overrideCustomText: Boolean = false,
        val overrideBehandlingstid: Boolean = false,
        val customText: String? = null,
    ) {
        data class RecipientView(
            val part: PartViewWithUtsendingskanal,
            val handling: HandlingEnum,
            val overriddenAddress: AddressView?
        ) {
            data class AddressView(
                val adresselinje1: String?,
                val adresselinje2: String?,
                val adresselinje3: String?,
                val landkode: String?,
                val postnummer: String?
            )
        }
    }
}

data class FerdigstiltRegistreringView(
    val id: UUID,
    val finished: LocalDateTime,
    val behandlingId: UUID,
)

data class MottattVedtaksinstansChangeRegistreringView(
    val id: UUID,
    val overstyringer: OverstyringerView,
    val modified: LocalDateTime,
) {
    data class OverstyringerView(
        val mottattVedtaksinstans: LocalDate?,
    )
}

data class MottattKlageinstansChangeRegistreringView(
    val id: UUID,
    val overstyringer: OverstyringerView,
    val modified: LocalDateTime,
) {
    data class OverstyringerView(
        val mottattKlageinstans: LocalDate?,
        val calculatedFrist: LocalDate?,
    )
}

data class BehandlingstidChangeRegistreringView(
    val id: UUID,
    val overstyringer: OverstyringerView,
    val modified: LocalDateTime,
) {
    data class OverstyringerView(
        val behandlingstid: BehandlingstidView,
        val calculatedFrist: LocalDate?,
    )
}

data class YtelseChangeRegistreringView(
    val id: UUID,
    val overstyringer: OverstyringerView,
    val modified: LocalDateTime,
) {
    data class OverstyringerView(
        val ytelseId: String?,
        val saksbehandlerIdent: String?,
    )
}

data class SvarbrevBehandlingstidChangeRegistreringView(
    val id: UUID,
    val svarbrev: SvarbrevView,
    val modified: LocalDateTime,
) {
    data class SvarbrevView(
        val behandlingstid: BehandlingstidView?,
        val calculatedFrist: LocalDate?,
    )
}