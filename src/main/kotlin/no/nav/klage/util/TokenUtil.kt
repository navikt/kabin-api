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
    }

    fun getOnBehalfOfTokenWithPdlScope(): String {
        val clientProperties = clientConfigurationProperties.registration["pdl-onbehalfof"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.access_token!!
    }

    fun getOnBehalfOfTokenWithKabalApiScope(): String {
        val clientProperties = clientConfigurationProperties.registration["kabal-api-onbehalfof"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.access_token!!
    }

    fun getMaskinTilMaskinTokenWithKabalApiScope(): String {
        val clientProperties = clientConfigurationProperties.registration["kabal-api-maskintilmaskin"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.access_token!!
    }

    fun getOnBehalfOfTokenWithKabalInnstillingerScope(): String {
        val clientProperties = clientConfigurationProperties.registration["kabal-innstillinger-onbehalfof"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.access_token!!
    }

    fun getOnBehalfOfTokenWithSafScope(): String {
        val clientProperties = clientConfigurationProperties.registration["saf-onbehalfof"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.access_token!!
    }

    fun getOnBehalfOfTokenWithGosysOppgaveScope(): String {
        val clientProperties = clientConfigurationProperties.registration["gosys-oppgave-onbehalfof"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.access_token!!
    }

    fun getOnBehalfOfTokenWithDokArkivScope(): String {
        val clientProperties = clientConfigurationProperties.registration["dok-arkiv-onbehalfof"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.access_token!!
    }

    fun getOnBehalfOfTokenWithKlageFSSProxyScope(): String {
        val clientProperties = clientConfigurationProperties.registration["klage-fss-proxy-onbehalfof"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.access_token!!
    }

    fun getMaskinTilMaskinTokenWithKlageFSSProxyScope(): String {
        val clientProperties = clientConfigurationProperties.registration["klage-fss-proxy-maskintilmaskin"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.access_token!!
    }

    fun getOnBehalfOfTokenWithKlageLookupScope(): String {
        val clientProperties = clientConfigurationProperties.registration["klage-lookup-onbehalfof"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.access_token!!
    }

    fun getMaskinTilMaskinTokenWithKlageLookupScope(): String {
        val clientProperties = clientConfigurationProperties.registration["klage-lookup-maskintilmaskin"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.access_token!!
    }

    fun getAccessTokenFrontendSent(): String =
        tokenValidationContextHolder.getTokenValidationContext().getJwtToken(SecurityConfiguration.ISSUER_AAD)!!.encodedToken

    fun getCurrentIdent(): String =
        tokenValidationContextHolder.getTokenValidationContext().getJwtToken(SecurityConfiguration.ISSUER_AAD)
            ?.jwtTokenClaims?.get("NAVident")?.toString()
            ?: throw RuntimeException("Ident not found in token")

    fun getCurrentTokenType(): TokenType {
        val validationContext = runCatching { tokenValidationContextHolder.getTokenValidationContext() }.getOrNull()
        val tokenType = if (validationContext == null) {
            TokenType.UNAUTHENTICATED
        } else {
            val idtype =
                runCatching { validationContext.getJwtToken(SecurityConfiguration.ISSUER_AAD)?.jwtTokenClaims?.get("idtyp") }.getOrNull()
            val navIdent =
                runCatching {
                    validationContext.getJwtToken(SecurityConfiguration.ISSUER_AAD)?.jwtTokenClaims?.get("NAVident")
                }.getOrNull()
            if (idtype != null && idtype == "app") {
                TokenType.CC
            } else if (navIdent != null) {
                TokenType.OBO
            } else {
                TokenType.UNAUTHENTICATED
            }
        }
        return tokenType
    }

    enum class TokenType {
        CC,
        OBO,
        UNAUTHENTICATED,
    }
}
