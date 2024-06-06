package no.nav.klage.api.controller.view

import java.time.LocalDate
import java.time.LocalDateTime

data class OppgaveView(
    val id: Long,
    val tildeltEnhetsnr: String?,
    val endretAvEnhetsnr: String?,
    val endretAv: String?,
    val endretTidspunkt: LocalDateTime?,
    val opprettetAvEnhetsnr: String?,
    val opprettetAv: String?,
    val opprettetTidspunkt: LocalDateTime?,
    val beskrivelse: String?,
    val temaId: String,
    //Må parses via kodeverk
    val gjelder: String?,
    //Må parses via kodeverk
    val oppgavetype: String?,
    val tilordnetRessurs: String?,
    val fristFerdigstillelse: LocalDate?,
)