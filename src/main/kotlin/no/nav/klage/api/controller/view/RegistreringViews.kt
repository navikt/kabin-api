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
    val overstyringer: FullRegistreringOverstyringerView,
    val svarbrev: FullRegistreringSvarbrevView,
    val created: LocalDateTime,
    val modified: LocalDateTime,
    val createdBy: String,
    val finished: LocalDateTime?,
    val behandlingId: UUID?,
) {

    data class FullRegistreringOverstyringerView(
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

    data class FullRegistreringSvarbrevView(
        val send: Boolean?,
        val behandlingstid: BehandlingstidView?,
        val calculatedFrist: LocalDate?,
        val fullmektigFritekst: String?,
        val receivers: List<RecipientView>,
        val title: String,
        val overrideCustomText: Boolean,
        val overrideBehandlingstid: Boolean,
        val customText: String?
    )
}

data class TypeChangeRegistreringView(
    val id: UUID,
    val typeId: String?,
    val mulighet: MulighetView? = null,
    val overstyringer: TypeChangeRegistreringOverstyringerView,
    val svarbrev: TypeChangeRegistreringSvarbrevView,
    val modified: LocalDateTime,
) {

    data class TypeChangeRegistreringOverstyringerView(
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

    data class TypeChangeRegistreringSvarbrevView(
        val send: Boolean? = null,
        val behandlingstid: BehandlingstidView? = null,
        val fullmektigFritekst: String? = null,
        val receivers: List<RecipientView> = emptyList(),
        val overrideCustomText: Boolean = false,
        val overrideBehandlingstid: Boolean = false,
        val customText: String? = null,
    )
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
    val overstyringer: MulighetChangeRegistreringOverstyringerView,
    val svarbrev: MulighetChangeRegistreringSvarbrevView,
    val modified: LocalDateTime,
) {

    data class MulighetChangeRegistreringOverstyringerView(
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

    data class MulighetChangeRegistreringSvarbrevView(
        val send: Boolean? = null,
        val behandlingstid: BehandlingstidView? = null,
        val fullmektigFritekst: String? = null,
        val receivers: List<RecipientView> = emptyList(),
        val overrideCustomText: Boolean = false,
        val overrideBehandlingstid: Boolean = false,
        val customText: String? = null,
    )
}

data class RecipientView(
    val id: UUID,
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

data class FerdigstiltRegistreringView(
    val id: UUID,
    val finished: LocalDateTime,
    val modified: LocalDateTime,
    val behandlingId: UUID,
)

data class MottattVedtaksinstansChangeRegistreringView(
    val id: UUID,
    val overstyringer: MottattVedtaksinstansChangeRegistreringOverstyringerView,
    val modified: LocalDateTime,
) {
    data class MottattVedtaksinstansChangeRegistreringOverstyringerView(
        val mottattVedtaksinstans: LocalDate?,
    )
}

data class MottattKlageinstansChangeRegistreringView(
    val id: UUID,
    val overstyringer: MottattKlageinstansChangeRegistreringOverstyringerView,
    val modified: LocalDateTime,
) {
    data class MottattKlageinstansChangeRegistreringOverstyringerView(
        val mottattKlageinstans: LocalDate?,
        val calculatedFrist: LocalDate?,
    )
}

data class BehandlingstidChangeRegistreringView(
    val id: UUID,
    val overstyringer: BehandlingstidChangeRegistreringOverstyringerView,
    val modified: LocalDateTime,
) {
    data class BehandlingstidChangeRegistreringOverstyringerView(
        val behandlingstid: BehandlingstidView,
        val calculatedFrist: LocalDate?,
    )
}

data class YtelseChangeRegistreringView(
    val id: UUID,
    val overstyringer: YtelseChangeRegistreringOverstyringerView,
    val modified: LocalDateTime,
) {
    data class YtelseChangeRegistreringOverstyringerView(
        val ytelseId: String?,
        val saksbehandlerIdent: String?,
    )
}

data class SvarbrevBehandlingstidChangeRegistreringView(
    val id: UUID,
    val svarbrev: SvarbrevBehandlingstidChangeRegistreringSvarbrevView,
    val modified: LocalDateTime,
) {
    data class SvarbrevBehandlingstidChangeRegistreringSvarbrevView(
        val behandlingstid: BehandlingstidView?,
        val calculatedFrist: LocalDate?,
    )
}

data class FullmektigChangeRegistreringView(
    val id: UUID,
    val svarbrev: FullmektigChangeSvarbrevView,
    val overstyringer: FullmektigChangeRegistreringOverstyringerView,
    val modified: LocalDateTime,
) {
    data class FullmektigChangeRegistreringOverstyringerView(
        val fullmektig: PartViewWithUtsendingskanal?,
    )

    data class FullmektigChangeSvarbrevView(
        val fullmektigFritekst: String?,
        val receivers: List<RecipientView>,
    )
}

data class KlagerChangeRegistreringView(
    val id: UUID,
    val svarbrev: KlagerChangeRegistreringViewSvarbrevView,
    val overstyringer: KlagerChangeRegistreringViewRegistreringOverstyringerView,
    val modified: LocalDateTime,
) {
    data class KlagerChangeRegistreringViewRegistreringOverstyringerView(
        val klager: PartViewWithUtsendingskanal?,
    )

    data class KlagerChangeRegistreringViewSvarbrevView(
        val receivers: List<RecipientView>,
    )
}

data class AvsenderChangeRegistreringView(
    val id: UUID,
    val svarbrev: AvsenderChangeRegistreringViewSvarbrevView,
    val overstyringer: AvsenderChangeRegistreringViewRegistreringOverstyringerView,
    val modified: LocalDateTime,
) {
    data class AvsenderChangeRegistreringViewRegistreringOverstyringerView(
        val avsender: PartViewWithUtsendingskanal?,
    )

    data class AvsenderChangeRegistreringViewSvarbrevView(
        val receivers: List<RecipientView>,
    )
}