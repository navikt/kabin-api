package no.nav.klage.api.controller.view

import no.nav.klage.domain.entities.HandlingEnum
import java.time.LocalDateTime
import java.util.*

data class RegistreringView(
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
) {
    data class MulighetView(
        val id: String,
        val fagsystemId: String,
    )

    data class OverstyringerView(
        val mottattVedtaksinstans: String?,
        val mottattKlageinstans: String?,
        val behandlingstid: BehandlingstidView?,
        val hjemmelIdList: List<String>,
        val ytelseId: String?,
        val fullmektig: PartViewWithUtsendingskanal?,
        val klager: PartViewWithUtsendingskanal?,
        val avsender: PartViewWithUtsendingskanal?,
        val saksbehandlerIdent: String?,
        val oppgaveId: String?
    )

    data class BehandlingstidView(
        val unitTypeId: String,
        val units: Int,
    )

    data class SvarbrevView(
        val send: Boolean?,
        val behandlingstid: BehandlingstidView?,
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