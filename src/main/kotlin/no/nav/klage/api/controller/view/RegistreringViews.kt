package no.nav.klage.api.controller.view

import no.nav.klage.domain.entities.HandlingEnum
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class FinishedRegistreringView(
    val id: UUID,
    val sakenGjelderValue: String,
    val typeId: String,
    val ytelseId: String,
    val finished: LocalDateTime,
    val created: LocalDateTime,
    val behandlingId: UUID,
)

data class FullRegistreringView(
    val id: UUID,
    val sakenGjelderValue: String?,
    val journalpostId: String?,
    val typeId: String?,
    val mulighetIsBasedOnJournalpost: Boolean,
    val mulighet: MulighetIdView?,
    val overstyringer: FullRegistreringOverstyringerView,
    val svarbrev: FullRegistreringSvarbrevView,
    val created: LocalDateTime,
    val modified: LocalDateTime,
    val createdBy: String,
    val finished: LocalDateTime?,
    val behandlingId: UUID?,
    val willCreateNewJournalpost: Boolean,
    @Deprecated("Use muligheter instead")
    val klagemuligheter: List<KlagemulighetView>,
    @Deprecated("Use muligheter instead")
    val ankemuligheter: List<KabalmulighetView>,
    @Deprecated("Use muligheter instead")
    val omgjoeringskravmuligheter: List<KabalmulighetView>,
    @Deprecated("Use muligheter instead")
    val gjenopptaksmuligheter: List<KabalmulighetView>,
    @Deprecated("Use muligheter instead")
    val muligheterFetched: LocalDateTime?,
    val muligheter: MuligheterView,
) {

    data class FullRegistreringOverstyringerView(
        val mottattVedtaksinstans: LocalDate?,
        val mottattKlageinstans: LocalDate?,
        val behandlingstid: BehandlingstidView,
        val calculatedFrist: LocalDate?,
        val hjemmelIdList: List<String>,
        val ytelseId: String?,
        val fullmektig: PartViewWithOptionalUtsendingskanal?,
        val klager: PartViewWithOptionalUtsendingskanal?,
        val avsender: PartViewWithOptionalUtsendingskanal?,
        val saksbehandlerIdent: String?,
        val gosysOppgaveId: Long?
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
        val customText: String?,
        val initialCustomText: String?,
        val reasonNoLetter: String?,
    )
}

data class TypeChangeRegistreringView(
    val id: UUID,
    val typeId: String?,
    val mulighetIsBasedOnJournalpost: Boolean,
    val mulighet: MulighetIdView? = null,
    val overstyringer: TypeChangeRegistreringOverstyringerView,
    val svarbrev: TypeChangeRegistreringSvarbrevView,
    val modified: LocalDateTime,
    val willCreateNewJournalpost: Boolean,
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
        val gosysOppgaveId: Long? = null
    )

    data class TypeChangeRegistreringSvarbrevView(
        val send: Boolean?,
        val behandlingstid: BehandlingstidView?,
        val fullmektigFritekst: String?,
        val receivers: List<RecipientView> = emptyList(),
        val overrideCustomText: Boolean,
        val overrideBehandlingstid: Boolean,
        val customText: String?,
        val initialCustomText: String?,
        val reasonNoLetter: String?,
    )
}

data class MulighetIdView(
    val id: String,
)

data class BehandlingstidView(
    val unitTypeId: String,
    val units: Int,
)

data class MulighetChangeRegistreringView(
    val id: UUID,
    val mulighet: MulighetIdView?,
    val overstyringer: MulighetChangeRegistreringOverstyringerView,
    val svarbrev: MulighetChangeRegistreringSvarbrevView,
    val modified: LocalDateTime,
    val willCreateNewJournalpost: Boolean,
) {

    data class MulighetChangeRegistreringOverstyringerView(
        val mottattVedtaksinstans: LocalDate?,
        val mottattKlageinstans: LocalDate?,
        val behandlingstid: BehandlingstidView?,
        val calculatedFrist: LocalDate?,
        val hjemmelIdList: List<String>,
        val ytelseId: String?,
        val fullmektig: PartViewWithOptionalUtsendingskanal?,
        val klager: PartViewWithOptionalUtsendingskanal?,
        val avsender: PartViewWithOptionalUtsendingskanal?,
        val saksbehandlerIdent: String?,
        val gosysOppgaveId: Long?
    )

    data class MulighetChangeRegistreringSvarbrevView(
        val send: Boolean?,
        val behandlingstid: BehandlingstidView?,
        val fullmektigFritekst: String?,
        val receivers: List<RecipientView>,
        val overrideCustomText: Boolean,
        val overrideBehandlingstid: Boolean,
        val customText: String?,
        val initialCustomText: String?,
        val reasonNoLetter: String?,
    )
}

data class RecipientView(
    val id: UUID,
    val part: PartViewWithOptionalUtsendingskanal,
    val handling: HandlingEnum?,
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
    val svarbrev: YtelseChangeRegistreringSvarbrevView,
    val modified: LocalDateTime,
) {
    data class YtelseChangeRegistreringOverstyringerView(
        val ytelseId: String?,
        val saksbehandlerIdent: String?,
    )

    data class YtelseChangeRegistreringSvarbrevView(
        val send: Boolean?,
        val behandlingstid: BehandlingstidView?,
        val fullmektigFritekst: String?,
        val receivers: List<RecipientView>,
        val overrideCustomText: Boolean,
        val overrideBehandlingstid: Boolean,
        val customText: String?,
        val initialCustomText: String?,
        val reasonNoLetter: String?,
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
        val fullmektig: PartViewWithOptionalUtsendingskanal?,
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
        val klager: PartViewWithOptionalUtsendingskanal?,
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
        val avsender: PartViewWithOptionalUtsendingskanal?,
    )

    data class AvsenderChangeRegistreringViewSvarbrevView(
        val receivers: List<RecipientView>,
    )
}

data class SaksbehandlerIdentChangeRegistreringView(
    val id: UUID,
    val overstyringer: SaksbehandlerIdentChangeRegistreringOverstyringerView,
    val modified: LocalDateTime,
) {
    data class SaksbehandlerIdentChangeRegistreringOverstyringerView(
        val saksbehandlerIdent: String?,
    )
}

data class GosysOppgaveIdChangeRegistreringView(
    val id: UUID,
    val overstyringer: GosysOppgaveIdChangeRegistreringOverstyringerView,
    val modified: LocalDateTime,
) {
    data class GosysOppgaveIdChangeRegistreringOverstyringerView(
        val gosysOppgaveId: Long?,
    )
}

data class SendSvarbrevChangeRegistreringView(
    val id: UUID,
    val svarbrev: SendSvarbrevChangeRegistreringSvarbrevView,
    val modified: LocalDateTime,
) {
    data class SendSvarbrevChangeRegistreringSvarbrevView(
        val send: Boolean,
        val reasonNoLetter: String?,
        val calculatedFrist: LocalDate?,
    )
}

data class ReasonNoLetterChangeRegistreringView(
    val id: UUID,
    val svarbrev: ReasonNoLetterChangeRegistreringSvarbrevView,
    val modified: LocalDateTime,
) {
    data class ReasonNoLetterChangeRegistreringSvarbrevView(
        val reasonNoLetter: String,
    )
}

data class SvarbrevOverrideCustomTextChangeRegistreringView(
    val id: UUID,
    val svarbrev: SvarbrevOverrideCustomTextChangeRegistreringSvarbrevView,
    val modified: LocalDateTime,
) {
    data class SvarbrevOverrideCustomTextChangeRegistreringSvarbrevView(
        val overrideCustomText: Boolean,
        val customText: String?,
    )
}

data class SvarbrevOverrideBehandlingstidChangeRegistreringView(
    val id: UUID,
    val svarbrev: SvarbrevOverrideBehandlingstidChangeRegistreringSvarbrevView,
    val modified: LocalDateTime,
) {
    data class SvarbrevOverrideBehandlingstidChangeRegistreringSvarbrevView(
        val overrideBehandlingstid: Boolean,
        val behandlingstid: BehandlingstidView?,
        val calculatedFrist: LocalDate?,
    )
}

data class SvarbrevTitleChangeRegistreringView(
    val id: UUID,
    val svarbrev: SvarbrevTitleChangeRegistreringSvarbrevView,
    val modified: LocalDateTime,
) {
    data class SvarbrevTitleChangeRegistreringSvarbrevView(
        val title: String,
    )
}

data class SvarbrevCustomTextChangeRegistreringView(
    val id: UUID,
    val svarbrev: SvarbrevCustomTextChangeRegistreringSvarbrevView,
    val modified: LocalDateTime,
) {
    data class SvarbrevCustomTextChangeRegistreringSvarbrevView(
        val customText: String,
    )
}

data class SvarbrevInitialCustomTextChangeRegistreringView(
    val id: UUID,
    val svarbrev: SvarbrevInitialCustomTextChangeRegistreringSvarbrevView,
    val modified: LocalDateTime,
) {
    data class SvarbrevInitialCustomTextChangeRegistreringSvarbrevView(
        val initialCustomText: String?,
    )
}

data class HjemmelIdListChangeRegistreringView(
    val id: UUID,
    val overstyringer: HjemmelIdListChangeRegistreringOverstyringerView,
    val modified: LocalDateTime,
) {
    data class HjemmelIdListChangeRegistreringOverstyringerView(
        val hjemmelIdList: List<String>,
    )
}

data class SvarbrevReceiverChangeRegistreringView(
    val id: UUID,
    val svarbrev: SvarbrevReceiverChangeRegistreringSvarbrevView,
    val modified: LocalDateTime,
) {
    data class SvarbrevReceiverChangeRegistreringSvarbrevView(
        val receivers: List<RecipientView>,
    )
}

data class SvarbrevFullmektigFritekstChangeRegistreringView(
    val id: UUID,
    val svarbrev: SvarbrevFullmektigFritekstChangeRegistreringSvarbrevView,
    val modified: LocalDateTime,
) {
    data class SvarbrevFullmektigFritekstChangeRegistreringSvarbrevView(
        val fullmektigFritekst: String?,
    )
}