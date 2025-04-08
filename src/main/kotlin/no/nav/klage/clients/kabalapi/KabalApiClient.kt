package no.nav.klage.clients.kabalapi

import no.nav.klage.api.controller.view.IdnummerInput
import no.nav.klage.api.controller.view.SearchPartInput
import no.nav.klage.api.controller.view.SearchPartWithUtsendingskanalInput
import no.nav.klage.api.controller.view.SearchUsedJournalpostIdInput
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
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

    fun checkBehandlingDuplicateInKabal(input: BehandlingIsDuplicateInput, token: String): Mono<BehandlingIsDuplicateResponse> {
        return kabalApiWebClient.post()
            .uri { it.path("/api/internal/checkbehandlingisduplicate").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                token,
            )
            .bodyValue(input)
            .retrieve()
            .bodyToMono<BehandlingIsDuplicateResponse>()
    }

    fun checkGosysOppgaveDuplicateInKabal(input: GosysOppgaveIsDuplicateInput): Boolean {
        return kabalApiWebClient.post()
            .uri { it.path("/api/internal/oppgaveisduplicate").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .bodyValue(input)
            .retrieve()
            .bodyToMono<Boolean>()
            .block() ?: throw RuntimeException("No response")
    }

    fun createBehandlingInKabal(input: CreateBehandlingBasedOnKabalInput): CreatedBehandlingResponse {
        return kabalApiWebClient.post()
            .uri { it.path("/api/internal/createbehandling").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .bodyValue(input)
            .retrieve()
            .bodyToMono<CreatedBehandlingResponse>()
            .block() ?: throw RuntimeException("No response")
    }

    fun createAnkeFromInfotrygdInputInKabal(input: CreateAnkeBasedOnKabinInput): CreatedBehandlingResponse {
        return kabalApiWebClient.post()
            .uri { it.path("/api/internal/createankefromcompleteinput").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .bodyValue(input)
            .retrieve()
            .bodyToMono<CreatedBehandlingResponse>()
            .block() ?: throw RuntimeException("No response")
    }

    fun getAnkemuligheterByIdnummer(idnummerInput: IdnummerInput, token: String): Mono<List<MulighetFromKabal>> {
        return kabalApiWebClient.post()
            .uri { it.path("/api/internal/ankemuligheter").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                token,
            )
            .bodyValue(idnummerInput)
            .retrieve()
            .bodyToMono<List<MulighetFromKabal>>()
    }

    fun getOmgjoeringskravmuligheterByIdnummer(idnummerInput: IdnummerInput, token: String): Mono<List<MulighetFromKabal>> {
        return kabalApiWebClient.post()
            .uri { it.path("/api/internal/omgjoeringskravmuligheter").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                token,
            )
            .bodyValue(idnummerInput)
            .retrieve()
            .bodyToMono<List<MulighetFromKabal>>()
    }

    fun searchPart(searchPartInput: SearchPartInput): SearchPartView {
        return kabalApiWebClient.post()
            .uri { it.path("/searchpart").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .bodyValue(searchPartInput)
            .retrieve()
            .bodyToMono<SearchPartView>()
            .block() ?: throw RuntimeException("null part returned")
    }

    fun searchPartWithUtsendingskanal(searchPartInput: SearchPartWithUtsendingskanalInput): PartViewWithUtsendingskanal {
        return kabalApiWebClient.post()
            .uri { it.path("/searchpartwithutsendingskanal").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .bodyValue(searchPartInput)
            .retrieve()
            .bodyToMono<PartViewWithUtsendingskanal>()
            .block() ?: throw RuntimeException("null part returned")
    }

    fun getBehandlingStatus(behandlingId: UUID): CreatedBehandlingStatus {
        return kabalApiWebClient.get()
            .uri { it.path("/api/internal/behandlinger/{behandlingId}/status").build(behandlingId) }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .retrieve()
            .bodyToMono<CreatedBehandlingStatus>()
            .block() ?: throw RuntimeException("Could not get status for behandlingId $behandlingId")
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

    fun getSvarbrevSettings(ytelseId: String, typeId: String): SvarbrevSettingsView {
        return kabalApiWebClient.get()
            .uri { it.path("/svarbrev-settings/ytelser/{ytelseId}/typer/{typeId}").build(ytelseId, typeId) }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .retrieve()
            .bodyToMono<SvarbrevSettingsView>()
            .block() ?: throw RuntimeException("null returned for getSvarbrevSettingsForRegistrering")
    }
}
