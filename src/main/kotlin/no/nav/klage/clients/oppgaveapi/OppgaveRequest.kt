package no.nav.klage.clients.oppgaveapi

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateOppgaveInput(
    val versjon: Int,
    val fristFerdigstillelse: LocalDate?,
    val mappeId: Long?,
    val orgnr: String?,
    val status: Status?,
    val endretAvEnhetsnr: String?,
    val tilordnetRessurs: String?,
    val tildeltEnhetsnr: String?,
    val prioritet: Prioritet?,
    val behandlingstema: String?,
    val behandlingstype: String?,
    val aktivDato: String?,
    val oppgavetype: String?,
    val tema: String?,
    val journalpostId: String?,
    val saksreferanse: String?,
    val behandlesAvApplikasjon: String?,
    val personident: String?,
    val beskrivelse: String?,
    val kommentar: Kommentar?,
) {
    data class Kommentar(
        val tekst: String,
        val automatiskGenerert: Boolean,
    )
}
