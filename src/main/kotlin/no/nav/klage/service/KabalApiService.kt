package no.nav.klage.service

import no.nav.klage.api.controller.CreateAnkeBasedOnKlagebehandling
import no.nav.klage.clients.KabalApiClient
import org.springframework.stereotype.Service

@Service
class KabalApiService(
    private val kabalApiClient: KabalApiClient
) {

    fun getCompletedKlagebehandlingerByPartIdValue(partIdValue: String): List<KabalApiClient.CompletedKlagebehandling> {
        return kabalApiClient.getCompletedKlagebehandlingerByPartIdValue(partIdValue)
    }

    fun createAnkeInKabal(input: CreateAnkeBasedOnKlagebehandling) {
        kabalApiClient.createAnkeInKabal(input)
    }

}