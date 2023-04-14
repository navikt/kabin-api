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

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CreatedBehandlingResponse(
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
        val fagsakId: String,
        val sakFagsystem: Fagsystem,
        val fagsystem: Fagsystem,
        val fagsystemId: String,
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CreatedAnkebehandlingStatus(
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
        val fagsakId: String,
        val sakFagsystem: Fagsystem,
        val fagsystem: Fagsystem,
        val fagsystemId: String,
        val journalpost: DokumentReferanse,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CreatedKlagebehandlingStatus(
        val typeId: String,
        val behandlingId: UUID,
        val ytelseId: String,
        val sakenGjelder: SakenGjelderView,
        val klager: KlagerView,
        val fullmektig: PartView?,
        val mottattVedtaksinstans: LocalDate,
        val mottattKlageinstans: LocalDate,
        val frist: LocalDate,
        val fagsakId: String,
        val fagsystemId: String,
        val journalpost: DokumentReferanse,
        val kildereferanse: String,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CreateKlageBasedOnKabinInput(
        val sakenGjelder: OversendtPartId,
        val klager: OversendtPartId?,
        val fullmektig: OversendtPartId?,
        val fagsakId: String,
        val fagsystemId: String,
        val hjemmelIdList: List<String>?,
        val forrigeBehandlendeEnhet: String,
        val klageJournalpostId: String,
        val brukersHenvendelseMottattNav: LocalDate,
        val sakMottattKa: LocalDate,
        val frist: LocalDate,
        val ytelseId: String,
        val kildereferanse: String,
    )
}