package no.nav.klage.service

import no.nav.klage.clients.klagelookup.KlageLookupClient
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service

@Service
class SaksbehandlerService(
    private val klageLookupClient: KlageLookupClient,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getSaksbehandlerPersonligInfo(navIdent: String): SaksbehandlerPersonligInfo {
        val data = try {
            klageLookupClient.getUserInfo(navIdent = navIdent)
        } catch (e: Exception) {
            logger.warn("Failed to call getSaksbehandler", e)
            throw e
        }
        return SaksbehandlerPersonligInfo(
            navIdent = data.navIdent,
            fornavn = data.fornavn,
            etternavn = data.etternavn,
            sammensattNavn = data.sammensattNavn,
            enhet = Enhet(
                enhetId = data.enhet.enhetNr,
                navn = data.enhet.enhetNavn
            )
        )
    }

    data class SaksbehandlerPersonligInfo(
        val navIdent: String,
        val fornavn: String,
        val etternavn: String,
        val sammensattNavn: String,
        val enhet: Enhet,
    )

    data class Enhet(val enhetId: String, val navn: String)
}