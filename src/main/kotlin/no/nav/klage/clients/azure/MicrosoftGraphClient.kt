package no.nav.klage.clients.azure

import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.klage.util.logErrorResponse
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class MicrosoftGraphClient(
    private val microsoftGraphWebClient: WebClient,
    private val tokenUtil: TokenUtil
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()

        private const val userSelect =
            "onPremisesSamAccountName,displayName,givenName,surname,userPrincipalName,streetAddress"
    }

    fun getSaksbehandlerInfo(navIdent: String): AzureUser {
        logger.debug("${::getSaksbehandlerInfo.name} $navIdent")
        return microsoftGraphWebClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/users")
                    .queryParam("\$filter", "onPremisesSamAccountName eq '$navIdent'")
                    .queryParam("\$select", userSelect)
                    .queryParam("\$count", true)
                    .build()
            }
            .header("Authorization", "Bearer ${tokenUtil.getAppAccessTokenWithGraphScope()}")
            .header("ConsistencyLevel", "eventual")
            .retrieve()
            .onStatus(HttpStatusCode::isError) { response ->
                logErrorResponse(response, ::getSaksbehandlerInfo.name, secureLogger)
            }
            .bodyToMono<AzureUserList>()
            .block()?.value?.firstOrNull()
            ?.let { secureLogger.debug("Saksbehandler: {}", it); it }
            ?: throw RuntimeException("AzureAD data about user by nav ident could not be fetched")
    }
}