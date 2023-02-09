package no.nav.klage.clients

import no.nav.klage.api.controller.CreateAnkeBasedOnKlagebehandling
import no.nav.klage.kodeverk.Utfall
import no.nav.klage.kodeverk.Ytelse
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
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

    fun createAnkeInKabal(input: CreateAnkeBasedOnKlagebehandling) {
        kabalApiWebClient.post()
            .uri { it.path("/api/internal/createanke").build() }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .bodyValue(input)
            .retrieve()
            .bodyToMono<Void>()
            .block()
    }

    fun getCompletedKlagebehandlingerByPartIdValue(partIdValue: String): List<CompletedKlagebehandling> {
        return kabalApiWebClient.get()
            .uri { it.path("/api/internal/brukere/{partIdValue}/completedklagebehandlinger").build(partIdValue) }
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"
            )
            .retrieve()
            .bodyToMono<List<CompletedKlagebehandling>>()
            .block() ?: throw RuntimeException("Didn't get any completedklagebehandlinger")
    }

    data class CompletedKlagebehandling(
        val behandlingId: UUID,
        val ytelse: Ytelse,
        val utfall: Utfall,
        val vedtakDate: LocalDateTime,
        val sakenGjelder: SakenGjelderView,
        val klager: KlagerView,
        val tilknyttedeDokumenter: List<TilknyttetDokument>,
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

    data class VirksomhetView(
        val virksomhetsnummer: String?,
        val navn: String?,
    )

    data class PersonView(
        val foedselsnummer: String?,
        val navn: NavnView?,
        val kjoenn: String?,
    )
}