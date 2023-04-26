package no.nav.klage.clients.kabalapi

import no.nav.klage.api.controller.view.IdnummerInput
import no.nav.klage.api.controller.view.SearchPartInput
import no.nav.klage.api.controller.view.SearchUsedJournalpostIdInput
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.*

@Component
class KabalApiClient(
    private val kabalApiWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun checkDuplicateInKabal(input: IsDuplicateInput): Boolean {
        return kabalApiWebClient.post()
            .uri { it.path("/api/internal/isduplicate").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .bodyValue(input)
            .retrieve()
            .bodyToMono<Boolean>()
            .block() ?: throw RuntimeException("No response")
    }

    fun createAnkeInKabal(input: CreateAnkeBasedOnKlagebehandling): CreatedBehandlingResponse {
        return kabalApiWebClient.post()
            .uri { it.path("/api/internal/createanke").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .bodyValue(input)
            .retrieve()
            .bodyToMono<CreatedBehandlingResponse>()
            .block() ?: throw RuntimeException("No response")
    }

    fun getCompletedKlagebehandlingerByIdnummer(idnummerInput: IdnummerInput): List<CompletedKlagebehandling> {
        return kabalApiWebClient.post()
            .uri { it.path("/api/internal/completedklagebehandlinger").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .bodyValue(idnummerInput)
            .retrieve()
            .bodyToMono<List<CompletedKlagebehandling>>()
            .block() ?: throw RuntimeException("Didn't get any completedklagebehandlinger")
    }

    fun getCompletedKlagebehandling(klagebehandlingId: UUID): CompletedKlagebehandling {
        return kabalApiWebClient.get()
            .uri { it.path("/api/internal/completedklagebehandlinger/{klagebehandlingId}").build(klagebehandlingId) }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .retrieve()
            .bodyToMono<CompletedKlagebehandling>()
            .block() ?: throw RuntimeException("Could not get klagebehandling with id $klagebehandlingId")
    }

    fun searchPart(searchPartInput: SearchPartInput): PartView {
        return kabalApiWebClient.post()
            .uri { it.path("/searchfullmektig").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .bodyValue(searchPartInput)
            .retrieve()
            .bodyToMono<PartView>()
            .block() ?: throw RuntimeException("null part returned")
    }

    fun getCreatedAnkeStatus(mottakId: UUID): CreatedAnkebehandlingStatus {
        return kabalApiWebClient.get()
            .uri { it.path("/api/internal/anker/{mottakId}/status").build(mottakId) }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .retrieve()
            .bodyToMono<CreatedAnkebehandlingStatus>()
            .block() ?: throw RuntimeException("Could not get ankestatus for mottakId $mottakId")
    }

    fun getCreatedKlageStatus(mottakId: UUID): CreatedKlagebehandlingStatus {
        return kabalApiWebClient.get()
            .uri { it.path("/api/internal/klager/{mottakId}/status").build(mottakId) }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .retrieve()
            .bodyToMono<CreatedKlagebehandlingStatus>()
            .block() ?: throw RuntimeException("Could not get klagestatus for mottakId $mottakId")
    }

    fun getUsedJournalpostIdListForPerson(fnr: String): List<String> {
        return kabalApiWebClient.post()
            .uri { it.path("/api/internal/searchusedjournalpostid").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .bodyValue(SearchUsedJournalpostIdInput(fnr = fnr))
            .retrieve()
            .bodyToMono<List<String>>()
            .block() ?: throw RuntimeException("null returned for getUsedJournalpostIdListForPerson")
    }

    fun createKlageInKabal(input: CreateKlageBasedOnKabinInput): CreatedBehandlingResponse {
        return kabalApiWebClient.post()
            .uri { it.path("/api/internal/createklage").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .bodyValue(input)
            .retrieve()
            .bodyToMono<CreatedBehandlingResponse>()
            .block() ?: throw RuntimeException("No response")
    }
}
