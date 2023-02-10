package no.nav.klage.service

import no.nav.klage.api.controller.view.CreateAnkeBasedOnKlagebehandling
import no.nav.klage.api.controller.view.IdnummerInput
import no.nav.klage.clients.KabalApiClient
import org.springframework.stereotype.Service

@Service
class KabalApiService(
    private val kabalApiClient: KabalApiClient
) {

    fun getCompletedKlagebehandlingerByIdnummer(idnummerInput: IdnummerInput): List<KabalApiClient.CompletedKlagebehandling> {
        return kabalApiClient.getCompletedKlagebehandlingerByIdnummer(idnummerInput)
    }

    fun createAnkeInKabal(input: CreateAnkeBasedOnKlagebehandling) {
        kabalApiClient.createAnkeInKabal(input)
    }
}