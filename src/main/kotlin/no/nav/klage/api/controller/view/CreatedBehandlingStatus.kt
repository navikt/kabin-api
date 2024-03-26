package no.nav.klage.api.controller.view

import java.time.LocalDate

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
    val journalpost: DokumentReferanse,
    val tildeltSaksbehandler: TildeltSaksbehandler?,
)

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
    val journalpost: DokumentReferanse,
    val tildeltSaksbehandler: TildeltSaksbehandler?,
)

data class TildeltSaksbehandler(
    val navIdent: String,
    val navn: String,
)