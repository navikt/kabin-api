package no.nav.klage.clients

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.klage.api.controller.view.*
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.LocalDate
import java.time.LocalDateTime
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

    fun createAnkeInKabal(input: CreateAnkeBasedOnKlagebehandling): CreatedAnkeResponse {
        return kabalApiWebClient.post()
            .uri { it.path("/api/internal/createanke").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .bodyValue(input)
            .retrieve()
            .bodyToMono<CreatedAnkeResponse>()
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

    fun getCreatedAnkeStatus(mottakId: UUID): CreatedBehandlingStatus {
        return kabalApiWebClient.get()
            .uri { it.path("/api/internal/anker/{mottakId}/status").build(mottakId) }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .retrieve()
            .bodyToMono<CreatedBehandlingStatus>()
            .block() ?: throw RuntimeException("Could not get ankestatus for mottakId $mottakId")
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CreatedAnkeResponse(
        val mottakId: UUID,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CompletedKlagebehandling(
        val behandlingId: UUID,
        val ytelseId: String,
        val utfallId: String,
        val vedtakDate: LocalDateTime,
        val sakenGjelder: SakenGjelderView,
        val klager: KlagerView,
        val fullmektig: PartView?,
        val tilknyttedeDokumenter: List<TilknyttetDokument>,
        val sakFagsakId: String,
        val sakFagsystem: Fagsystem,
        val klageBehandlendeEnhet: String,
    )

    data class TilknyttetDokument(val journalpostId: String, val dokumentInfoId: String)

    data class NavnView(
        val fornavn: String?,
        val mellomnavn: String?,
        val etternavn: String?,
    )

    data class KlagerView(
        val person: PersonView?,
        val virksomhet: VirksomhetView?
    )

    data class SakenGjelderView(
        val person: PersonView?,
        val virksomhet: VirksomhetView?
    )

    data class PartView(
        val person: PersonView?,
        val virksomhet: VirksomhetView?
    )

    data class VirksomhetView(
        val virksomhetsnummer: String?,
        val navn: String?,
    )

    data class PersonView(
        val foedselsnummer: String?,
        val navn: NavnView?,
        val kjoenn: String?,
    )

    data class CreatedBehandlingStatus(
        val typeId: String,
        val behandlingId: UUID,
        val ytelseId: String,
        val utfallId: String,
        val vedtakDate: LocalDateTime,
        val sakenGjelder: SakenGjelderView,
        val klager: KlagerView,
        val fullmektig: PartView?,
        val tilknyttedeDokumenter: List<TilknyttetDokument>,
        val mottattNav: LocalDate,
        val frist: LocalDate,
        val sakFagsakId: String,
        val sakFagsystem: Fagsystem,
        val journalpost: DokumentReferanse,
    )
}