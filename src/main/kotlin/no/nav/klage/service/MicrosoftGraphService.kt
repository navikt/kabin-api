package no.nav.klage.service

import no.nav.klage.clients.azure.MicrosoftGraphClient
import no.nav.klage.exceptions.EnhetNotFoundForSaksbehandlerException
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.stereotype.Service
import no.nav.klage.kodeverk.Enhet as KodeverkEnhet

@Service
class MicrosoftGraphService(
    private val microsoftGraphClient: MicrosoftGraphClient,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    fun getSaksbehandlerPersonligInfo(navIdent: String): SaksbehandlerPersonligInfo {
        val data = try {
            microsoftGraphClient.getSaksbehandlerInfo(navIdent = navIdent)
        } catch (e: Exception) {
            logger.warn("Failed to call getSaksbehandler", e)
            throw e
        }
        return SaksbehandlerPersonligInfo(
            navIdent = data.onPremisesSamAccountName,
            fornavn = data.givenName,
            etternavn = data.surname,
            sammensattNavn = data.displayName,
            enhet = mapToEnhet(data.streetAddress),
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

    private fun mapToEnhet(enhetNr: String): Enhet =
        KodeverkEnhet.entries.find { it.navn == enhetNr }
            ?.let { Enhet(it.navn, it.beskrivelse) }
            ?: throw EnhetNotFoundForSaksbehandlerException("Enhet ikke funnet med enhetNr $enhetNr")

}