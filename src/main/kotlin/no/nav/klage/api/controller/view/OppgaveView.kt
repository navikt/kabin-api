package no.nav.klage.api.controller.view

data class OppgaveView(
    val id: Long,
    val temaId: String,
    //Må parses via kodeverk
    val gjelder: String?,
    //Må parses via kodeverk
    val oppgavetype: String?,
    val opprettetAv: String?,
    val tildeltEnhetsnr: String?,
    val beskrivelse: String?,
    val tilordnetRessurs: String?,
    val endretAv: String?,
)