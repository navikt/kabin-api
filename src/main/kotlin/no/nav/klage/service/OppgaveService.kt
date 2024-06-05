package no.nav.klage.service

import no.nav.klage.api.controller.view.OppgaveView
import no.nav.klage.clients.oppgaveapi.*
import no.nav.klage.clients.pdl.PdlClient
import no.nav.klage.kodeverk.Tema
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.stereotype.Service

@Service
class OppgaveService(
    private val oppgaveClient: OppgaveClient,
    private val pdlClient: PdlClient
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

        return oppgaveList.map { it.toOppgaveView() }
    }

    fun getGjelderKodeverkForTema(tema: Tema): List<Gjelder> {
        return oppgaveClient.getGjelderKodeverkForTema(tema = tema)
    }

    fun getOppgavetypeKodeverkForTema(tema: Tema): List<OppgavetypeResponse> {
        return oppgaveClient.getOppgavetypeKodeverkForTema(tema = tema)
    }

    fun OppgaveApiRecord.toOppgaveView(): OppgaveView {
        val tema = Tema.fromNavn(tema)
        return OppgaveView(
            id = id,
            temaId = tema.id,
            gjelder = getGjelder(behandlingstype = behandlingstype, tema = tema),
            oppgavetype = getOppgavetype(oppgavetype = oppgavetype, tema = tema),
            opprettetAv = opprettetAv,
            tildeltEnhetsnr = tildeltEnhetsnr,
            beskrivelse = beskrivelse,
            tilordnetRessurs = tilordnetRessurs,
            endretAv = endretAv,
        )
    }

    private fun getGjelder(behandlingstype: String?, tema: Tema): String? {
        return getGjelderKodeverkForTema(tema = tema).firstOrNull { it.behandlingstype == behandlingstype }?.behandlingstypeTerm
    }

    private fun getOppgavetype(oppgavetype: String?, tema: Tema): String? {
        return getOppgavetypeKodeverkForTema(tema = tema).firstOrNull { it.oppgavetype == oppgavetype }?.term
    }
}