package no.nav.klage.clients.gosysoppgave

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateGosysOppgaveInput(
    val versjon: Int,
    val endretAvEnhetsnr: String,
    val tilordnetRessurs: String?,
    val tildeltEnhetsnr: String?,
    val beskrivelse: String?,
    val kommentar: Kommentar?,
) {
    data class Kommentar(
        val tekst: String,
        val automatiskGenerert: Boolean,
    )
}

data class FerdigstillGosysOppgaveRequest(
    val oppgaveId: Long,
    val versjon: Int,
    val status: Status = Status.FERDIGSTILT,
    val endretAvEnhetsnr: String,
)