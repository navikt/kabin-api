package no.nav.klage.service

import no.nav.klage.api.controller.view.OppgaveView
import no.nav.klage.clients.oppgaveapi.*
import no.nav.klage.clients.pdl.PdlClient
import no.nav.klage.kodeverk.Tema
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OppgaveService(
    private val oppgaveClient: OppgaveClient,
    private val pdlClient: PdlClient,
    private val kabalApiService: KabalApiService,
    private val microsoftGraphService: MicrosoftGraphService,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    fun getOppgaveList(fnr: String, tema: Tema?): List<OppgaveView> {
        val aktoerId = pdlClient.hentAktoerIdent(fnr = fnr)

        val oppgaveList = oppgaveClient.fetchOppgaveForAktoerIdAndTema(
            aktoerId = aktoerId,
            tema = tema,
        )
        //TODO: Legg til filter i spørringen mot oppgave-api
        return oppgaveList.map { it.toOppgaveView() }.filter { it.oppgavetype !in listOf("Journalføring", "Kontakt bruker") }
    }

    fun getGjelderKodeverkForTema(tema: Tema): List<Gjelder> {
        return oppgaveClient.getGjelderKodeverkForTema(tema = tema)
    }

    fun getOppgavetypeKodeverkForTema(tema: Tema): List<OppgavetypeResponse> {
        return oppgaveClient.getOppgavetypeKodeverkForTema(tema = tema)
    }

    fun updateOppgave(
        oppgaveId: Long,
        frist: LocalDate,
        tildeltSaksbehandlerIdent: String?
    ) {
        val currentUserIdent = tokenUtil.getCurrentIdent()
        val currentUserInfo = microsoftGraphService.getSaksbehandlerPersonligInfo(navIdent = currentUserIdent)
        val currentOppgave = oppgaveClient.getOppgave(oppgaveId = oppgaveId)

        val newComment = "Overførte oppgaven fra Kabin til Kabal."

        var newBeskrivelsePart = "$newComment\nOppdaterte frist."

        val (tilordnetRessurs, tildeltEnhetsnr) = if (tildeltSaksbehandlerIdent != null) {
            val tildeltSaksbehandlerInfo =
                microsoftGraphService.getSaksbehandlerPersonligInfo(tildeltSaksbehandlerIdent)
            newBeskrivelsePart += "\nTildelte oppgaven til $tildeltSaksbehandlerIdent."
            tildeltSaksbehandlerIdent to tildeltSaksbehandlerInfo.enhet.enhetId
        } else {
            null to null
        }
        oppgaveClient.updateOppgave(
            oppgaveId = oppgaveId,
            updateOppgaveInput = UpdateOppgaveInput(
                versjon = currentOppgave.versjon,
                fristFerdigstillelse = frist,
                mappeId = null,
                endretAvEnhetsnr = currentUserInfo.enhet.enhetId,
                tilordnetRessurs = tilordnetRessurs,
                tildeltEnhetsnr = tildeltEnhetsnr,
                beskrivelse = null,
                kommentar = UpdateOppgaveInput.Kommentar(
                    tekst = newComment,
                    automatiskGenerert = true
                ),
                tema = null,
                prioritet = null,
                orgnr = null,
                status = null,
                behandlingstema = null,
                behandlingstype = null,
                aktivDato = null,
                oppgavetype = null,
                journalpostId = null,
                saksreferanse = null,
                behandlesAvApplikasjon = null,
                personident = null,
            )
        )
    }

    private fun getNewBeskrivelse(
        newBeskrivelsePart: String,
        existingBeskrivelse: String?,
        currentUserInfo: MicrosoftGraphService.SaksbehandlerPersonligInfo,
    ): String {
        val formattedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

        val nameOfCurrentUser = currentUserInfo.sammensattNavn
        val currentUserEnhet = currentUserInfo.enhet.enhetId
        val header = "--- $formattedDate $nameOfCurrentUser (${currentUserInfo.navIdent}, $currentUserEnhet) ---"
        return "$header\n$newBeskrivelsePart\n\n$existingBeskrivelse\n".trimIndent()
    }

    fun OppgaveApiRecord.toOppgaveView(): OppgaveView {
        val tema = Tema.fromNavn(tema)
        val alreadyUsed = kabalApiService.oppgaveIsDuplicate(oppgaveId = id)
        return OppgaveView(
            id = id,
            temaId = tema.id,
            gjelder = getGjelder(behandlingstype = behandlingstype, tema = tema),
            oppgavetype = getOppgavetype(oppgavetype = oppgavetype, tema = tema),
            opprettetAv = opprettetAv,
            tildeltEnhetsnr = tildeltEnhetsnr,
            beskrivelse = beskrivelse,
            endretAv = endretAv,
            endretAvEnhetsnr = endretAvEnhetsnr,
            endretTidspunkt = endretTidspunkt,
            opprettetAvEnhetsnr = opprettetAvEnhetsnr,
            opprettetTidspunkt = opprettetTidspunkt,
            fristFerdigstillelse = fristFerdigstillelse,
            alreadyUsed = alreadyUsed,
        )
    }

    private fun getGjelder(behandlingstype: String?, tema: Tema): String? {
        return getGjelderKodeverkForTema(tema = tema).firstOrNull { it.behandlingstype == behandlingstype }?.behandlingstypeTerm
    }

    private fun getOppgavetype(oppgavetype: String?, tema: Tema): String? {
        return getOppgavetypeKodeverkForTema(tema = tema).firstOrNull { it.oppgavetype == oppgavetype }?.term
    }
}