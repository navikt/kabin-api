package no.nav.klage.service

import no.nav.klage.api.controller.view.GosysOppgaveView
import no.nav.klage.clients.gosysoppgave.*
import no.nav.klage.clients.pdl.PdlClient
import no.nav.klage.clients.pdl.grahql.IdentGruppe
import no.nav.klage.kodeverk.Tema
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class GosysOppgaveService(
    private val gosysOppgaveClient: GosysOppgaveClient,
    private val pdlClient: PdlClient,
    private val kabalApiService: KabalApiService,
    private val saksbehandlerService: SaksbehandlerService,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getGosysOppgaveList(fnr: String, tema: Tema?): List<GosysOppgaveView> {
        val aktoerId = pdlClient.hentIdent(ident = fnr, IdentGruppe.AKTORID)

        val temaList = if (tema != null) {
            if (tema == Tema.MED) {
                //Legger til TRY når vi søker på MED.
                listOf(tema, Tema.TRY)
            } else {
                listOf(tema)
            }.plus(Tema.KTR).distinct() //Tema KTR er relevant for alle områder.
        } else null

        val gosysOppgaveList = gosysOppgaveClient.fetchGosysOppgaverForAktoerIdAndTema(
            aktoerId = aktoerId,
            temaList = temaList,
        )
        //TODO: Legg til filter i spørringen mot oppgave-api
        return gosysOppgaveList.map { it.toOppgaveView() }.filter { it.oppgavetype !in listOf("Journalføring", "Kontakt bruker") }
    }

    fun getGjelderKodeverkForTema(tema: Tema): List<Gjelder> {
        return gosysOppgaveClient.getGjelderKodeverkForTema(tema = tema)
    }

    fun getGosysOppgavetypeKodeverkForTema(tema: Tema): List<GosysOppgavetypeResponse> {
        return gosysOppgaveClient.getGosysOppgavetypeKodeverkForTema(tema = tema)
    }

    fun updateGosysOppgave(
        gosysOppgaveId: Long,
        tildeltSaksbehandlerIdent: String?,
    ) {
        val currentUserIdent = tokenUtil.getCurrentIdent()
        val currentUserInfo = saksbehandlerService.getSaksbehandlerPersonligInfo(navIdent = currentUserIdent)
        val currentGosysOppgave = gosysOppgaveClient.getGosysOppgave(gosysOppgaveId = gosysOppgaveId)

        var comment = "Overførte oppgaven fra Kabin til Kabal."

        val (tilordnetRessurs, tildeltEnhetsnr) = if (tildeltSaksbehandlerIdent != null) {
            val tildeltSaksbehandlerInfo =
                saksbehandlerService.getSaksbehandlerPersonligInfo(tildeltSaksbehandlerIdent)
            comment += "\nTildelte oppgaven til $tildeltSaksbehandlerIdent."
            tildeltSaksbehandlerIdent to tildeltSaksbehandlerInfo.enhet.enhetId
        } else {
            null to null
        }
        gosysOppgaveClient.updateGosysOppgave(
            gosysOppgaveId = gosysOppgaveId,
            updateGosysOppgaveInput = UpdateGosysOppgaveInput(
                versjon = currentGosysOppgave.versjon,
                endretAvEnhetsnr = currentUserInfo.enhet.enhetId,
                tilordnetRessurs = tilordnetRessurs,
                tildeltEnhetsnr = tildeltEnhetsnr,
                beskrivelse = getNewBeskrivelse(
                    newBeskrivelsePart = comment,
                    existingBeskrivelse = currentGosysOppgave.beskrivelse,
                    currentUserInfo = currentUserInfo
                ),
                kommentar = UpdateGosysOppgaveInput.Kommentar(
                    tekst = comment,
                    automatiskGenerert = true
                ),
            )
        )
    }

    private fun getNewBeskrivelse(
        newBeskrivelsePart: String,
        existingBeskrivelse: String?,
        currentUserInfo: SaksbehandlerService.SaksbehandlerPersonligInfo,
    ): String {
        val formattedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

        val nameOfCurrentUser = currentUserInfo.sammensattNavn
        val currentUserEnhet = currentUserInfo.enhet.enhetId
        val header = "--- $formattedDate $nameOfCurrentUser (${currentUserInfo.navIdent}, $currentUserEnhet) ---"
        return "$header\n$newBeskrivelsePart\n\n$existingBeskrivelse\n".trimIndent()
    }

    fun GosysOppgaveRecord.toOppgaveView(): GosysOppgaveView {
        val tema = Tema.fromNavn(tema)
        val alreadyUsed = kabalApiService.gosysOppgaveIsDuplicate(gosysOppgaveId = id)
        return GosysOppgaveView(
            id = id,
            temaId = tema.id,
            gjelder = getGjelder(behandlingstype = behandlingstype, tema = tema),
            oppgavetype = getGosysOppgavetype(oppgavetype = oppgavetype, tema = tema),
            opprettetAv = opprettetAv,
            tildeltEnhetsnr = tildeltEnhetsnr,
            beskrivelse = beskrivelse,
            endretAv = endretAv,
            endretAvEnhetsnr = endretAvEnhetsnr,
            endretTidspunkt = endretTidspunkt?.toLocalDateTime(),
            opprettetAvEnhetsnr = opprettetAvEnhetsnr,
            opprettetTidspunkt = opprettetTidspunkt.toLocalDateTime(),
            fristFerdigstillelse = fristFerdigstillelse,
            alreadyUsed = alreadyUsed,
        )
    }

    private fun getGjelder(behandlingstype: String?, tema: Tema): String? {
        return getGjelderKodeverkForTema(tema = tema).firstOrNull { it.behandlingstype == behandlingstype }?.behandlingstypeTerm
    }

    private fun getGosysOppgavetype(oppgavetype: String?, tema: Tema): String? {
        return getGosysOppgavetypeKodeverkForTema(tema = tema).firstOrNull { it.oppgavetype == oppgavetype }?.term
    }
}