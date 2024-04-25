package no.nav.klage.api.controller.view

import java.time.LocalDate
import java.util.*

data class CreatedAnkebehandlingStatusView(
    val typeId: String,
    val ytelseId: String,
    val vedtakDate: LocalDate?,
    val sakenGjelder: PartViewWithUtsendingskanal,
    val klager: PartViewWithUtsendingskanal,
    val fullmektig: PartViewWithUtsendingskanal?,
    val mottattKlageinstans: LocalDate,
    val frist: LocalDate,
    val fagsakId: String,
    val fagsystemId: String,
    val journalpost: DokumentReferanseForReceipt,
    val tildeltSaksbehandler: TildeltSaksbehandler?,
    val svarbrev: Svarbrev?,
) {
    data class Svarbrev(
        val dokumentUnderArbeidId: UUID,
        val title: String,
        val receivers: List<Receiver>,
    ) {
        data class Receiver(
            val part: PartViewWithUtsendingskanal,
            val overriddenAddress: Address?,
            val handling: SvarbrevWithReceiverInput.Receiver.HandlingEnum,
        ) {
            data class Address(
                val adresselinje1: String?,
                val adresselinje2: String?,
                val adresselinje3: String?,
                val landkode: String,
                val postnummer: String?,
                val poststed: String?,
            )
        }
    }
}

data class CreatedKlagebehandlingStatusView(
    val typeId: String,
    val ytelseId: String,
    val vedtakDate: LocalDate,
    val sakenGjelder: PartViewWithUtsendingskanal,
    val klager: PartViewWithUtsendingskanal,
    val fullmektig: PartViewWithUtsendingskanal?,
    val mottattVedtaksinstans: LocalDate,
    val mottattKlageinstans: LocalDate,
    val frist: LocalDate,
    val fagsakId: String,
    val fagsystemId: String,
    val journalpost: DokumentReferanseForReceipt,
    val tildeltSaksbehandler: TildeltSaksbehandler?,
)

data class TildeltSaksbehandler(
    val navIdent: String,
    val navn: String,
)