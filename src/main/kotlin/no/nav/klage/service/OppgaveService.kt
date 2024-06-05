package no.nav.klage.service

import no.nav.klage.clients.oppgaveapi.Gjelder
import no.nav.klage.clients.oppgaveapi.GjelderResponse
import no.nav.klage.clients.oppgaveapi.OppgaveApiRecord
import no.nav.klage.clients.oppgaveapi.OppgaveClient
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

    fun getOppgaveList(fnr: String, tema: Tema?): List<OppgaveApiRecord> {
        val aktoerId = pdlClient.hentAktoerIdent(fnr = fnr)

        return oppgaveClient.fetchOppgaveForAktoerIdAndTema(
            aktoerId = aktoerId,
            tema = tema,
        )
    }

    fun getGjelderKodeverkForTema(tema: Tema): GjelderResponse {
        return oppgaveClient.getGjelderKodeverkForTema(tema = tema)
    }
}