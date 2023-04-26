package no.nav.klage.util

import no.nav.klage.config.SecurityConfiguration
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Service

@Service
class TokenUtil(
    private val clientConfigurationProperties: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
    private val tokenValidationContextHolder: TokenValidationContextHolder,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val securelogger = getSecureLogger()
    }


    fun getSaksbehandlerAccessTokenWithKabalApiScope(): String {
        val clientProperties = clientConfigurationProperties.registration["kabal-api-onbehalfof"]
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.accessToken
    }

    fun getSaksbehandlerAccessTokenWithKabalInnstillingerScope(): String {
        val clientProperties = clientConfigurationProperties.registration["kabal-innstillinger-onbehalfof"]
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.accessToken
    }

    fun getSaksbehandlerAccessTokenWithSafScope(): String {
        val clientProperties = clientConfigurationProperties.registration["saf-onbehalfof"]
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.accessToken
    }

    fun getSaksbehandlerAccessTokenWithDokArkivScope(): String {
        val clientProperties = clientConfigurationProperties.registration["dok-arkiv-onbehalfof"]
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.accessToken
    }

    fun getOnBehalfOfTokenWithKlageFSSProxyScope(): String {
        val clientProperties = clientConfigurationProperties.registration["klage-fss-proxy-onbehalfof"]
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.accessToken
    }

    fun getAccessTokenFrontendSent(): String =
        tokenValidationContextHolder.tokenValidationContext.getJwtToken(SecurityConfiguration.ISSUER_AAD).tokenAsString

    fun getIdent(): String =
        tokenValidationContextHolder.tokenValidationContext.getJwtToken(SecurityConfiguration.ISSUER_AAD)
            .jwtTokenClaims?.get("NAVident")?.toString()
            ?: throw RuntimeException("Ident not found in token")

    fun getRoleIdsFromToken(): List<String> =
        tokenValidationContextHolder.tokenValidationContext.getJwtToken(SecurityConfiguration.ISSUER_AAD)
            .jwtTokenClaims?.getAsList("groups").orEmpty().toList()

    private fun getClaim(name: String) =
        tokenValidationContextHolder.tokenValidationContext.getJwtToken(SecurityConfiguration.ISSUER_AAD)
            .jwtTokenClaims?.getStringClaim(name)

}
