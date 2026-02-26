package no.nav.klage.clients

import no.nav.klage.util.TokenUtil
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class KabalInnstillingerClient(
    private val kabalInnstillingerWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {

    fun getBrukerdata(): SaksbehandlerView {
        return kabalInnstillingerWebClient.get()
            .uri("/me/brukerdata")
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenUtil.getOnBehalfOfTokenWithKabalInnstillingerScope()}"
            )
            .retrieve()
            .bodyToMono<SaksbehandlerView>()
            .block() ?: throw RuntimeException("Couldn`t get brukerdata")
    }

    data class SaksbehandlerView(
        val navIdent: String,
        val roller: List<String>,
        val enheter: List<EnhetView>,
        val ansattEnhet: EnhetView,
        val tildelteYtelser: List<String>,
    )

    data class EnhetView(
        val id: String,
        val navn: String,
        val lovligeYtelser: List<String>
    )
}