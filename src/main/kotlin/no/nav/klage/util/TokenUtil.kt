package no.nav.klage.util

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.GrantType.CLIENT_CREDENTIALS
import com.nimbusds.oauth2.sdk.GrantType.JWT_BEARER
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.util.TokenUtil.Companion.CACHE_HIT
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.ClientCredentialsGrantRequest
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.core.oauth2.OnBehalfOfGrantRequest
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TokenUtil(
    private val clientConfigurationProperties: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
    private val tokenValidationContextHolder: TokenValidationContextHolder,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)

        private const val INSTRUMENTATION_SCOPE = "no.nav.klage.kabin-api"
        private const val SPAN_NAME = "azuread.token.acquire"

        private val REGISTRATION = AttributeKey.stringKey("token.client.registration")
        private val GRANT_TYPE = AttributeKey.stringKey("token.grant_type")
        private val SCOPE = AttributeKey.stringKey("token.scope")
        private val CACHE_HIT = AttributeKey.booleanKey("token.cache_hit")
        private val LIFETIME_SECONDS = AttributeKey.longKey("token.lifetime_seconds")
        private val REMAINING_SECONDS = AttributeKey.longKey("token.remaining_seconds")
        private val ISSUED_SECONDS_AGO = AttributeKey.longKey("token.issued_seconds_ago")
    }

    private val tracer = GlobalOpenTelemetry.getTracer(INSTRUMENTATION_SCOPE)

    /**
     * Wraps token acquisition in its own span so that time spent on cache misses, TLS handshakes and
     * calls to the Azure AD token endpoint is attributable in traces instead of showing up as an
     * unexplained gap in the parent span.
     */
    private fun getAccessToken(registrationName: String): String {
        val clientProperties = clientConfigurationProperties.registration[registrationName]!!

        val span = tracer.spanBuilder(SPAN_NAME)
            .setAttribute(REGISTRATION, registrationName)
            .setAttribute(GRANT_TYPE, clientProperties.grantType.value)
            .setAttribute(SCOPE, clientProperties.scope.joinToString(separator = " "))
            .startSpan()

        cacheHit(clientProperties)?.let { span.setAttribute(CACHE_HIT, it) }

        return try {
            span.makeCurrent().use {
                val accessToken = oAuth2AccessTokenService.getAccessToken(clientProperties).access_token!!
                addTokenAgeAttributes(span, accessToken)
                accessToken
            }
        } catch (e: Throwable) {
            span.setStatus(StatusCode.ERROR, e.message ?: e.javaClass.simpleName)
            span.recordException(e)
            throw e
        } finally {
            span.end()
        }
    }

    /**
     * Probes the token-support cache before the call, so we can tell a local cache hit from a real
     * round trip to Azure AD. Note that this cannot be derived from the token itself: Azure AD hands
     * back a token it minted earlier, so a freshly fetched token can still look "old".
     * Purely diagnostic, so any failure is reported as unknown rather than propagated.
     */
    private fun cacheHit(clientProperties: ClientProperties): Boolean? =
        runCatching {
            when (clientProperties.grantType) {
                JWT_BEARER -> oAuth2AccessTokenService.onBehalfOfGrantCache
                    ?.getIfPresent(OnBehalfOfGrantRequest(clientProperties, getAccessTokenFrontendSent()))

                CLIENT_CREDENTIALS -> oAuth2AccessTokenService.clientCredentialsGrantCache
                    ?.getIfPresent(ClientCredentialsGrantRequest(clientProperties))

                else -> return null
            } != null
        }.getOrNull()

    /**
     * Records how long ago the token was issued. Combined with [CACHE_HIT] this separates our own
     * caching from Azure AD's: an old token on a cache miss means Azure served it from its side.
     * Purely diagnostic, so any failure to parse is ignored.
     */
    private fun addTokenAgeAttributes(span: Span, accessToken: String) {
        runCatching {
            val claims = SignedJWT.parse(accessToken).jwtClaimsSet
            val issuedAt = claims.issueTime?.toInstant() ?: return
            val expiresAt = claims.expirationTime?.toInstant() ?: return
            val now = Instant.now()

            span.setAttribute(LIFETIME_SECONDS, expiresAt.epochSecond - issuedAt.epochSecond)
            span.setAttribute(REMAINING_SECONDS, expiresAt.epochSecond - now.epochSecond)
            span.setAttribute(ISSUED_SECONDS_AGO, now.epochSecond - issuedAt.epochSecond)
        }
    }

    fun getOnBehalfOfTokenWithPdlScope(): String = getAccessToken("pdl-onbehalfof")

    fun getOnBehalfOfTokenWithKabalApiScope(): String = getAccessToken("kabal-api-onbehalfof")

    fun getMaskinTilMaskinTokenWithKabalApiScope(): String = getAccessToken("kabal-api-maskintilmaskin")

    fun getOnBehalfOfTokenWithKabalInnstillingerScope(): String = getAccessToken("kabal-innstillinger-onbehalfof")

    fun getOnBehalfOfTokenWithKabalFileApiScope(): String = getAccessToken("kabal-file-api-onbehalfof")

    fun getOnBehalfOfTokenWithSafScope(): String = getAccessToken("saf-onbehalfof")

    fun getOnBehalfOfTokenWithGosysOppgaveScope(): String = getAccessToken("gosys-oppgave-onbehalfof")

    fun getOnBehalfOfTokenWithDokArkivScope(): String = getAccessToken("dok-arkiv-onbehalfof")

    fun getOnBehalfOfTokenWithKlageFSSProxyScope(): String = getAccessToken("klage-fss-proxy-onbehalfof")

    fun getMaskinTilMaskinTokenWithKlageFSSProxyScope(): String = getAccessToken("klage-fss-proxy-maskintilmaskin")

    fun getOnBehalfOfTokenWithKlankeScope(): String = getAccessToken("klanke-onbehalfof")

    fun getMaskinTilMaskinTokenWithKlankeScope(): String = getAccessToken("klanke-maskintilmaskin")

    fun getOnBehalfOfTokenWithKlageLookupScope(): String = getAccessToken("klage-lookup-onbehalfof")

    fun getMaskinTilMaskinTokenWithKlageLookupScope(): String = getAccessToken("klage-lookup-maskintilmaskin")

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
