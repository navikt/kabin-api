package no.nav.klage.util

import com.nimbusds.jwt.SignedJWT
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import no.nav.klage.config.SecurityConfiguration
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
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

        /** Slack to absorb clock skew between us and Azure AD when deciding cache hit vs. fresh token. */
        private const val CACHE_HIT_SLACK_SECONDS = 5
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
     * Derives whether the token came from the local cache by comparing its remaining lifetime with its
     * total lifetime. A freshly issued token has (almost) its full lifetime left, a cached one does not.
     * Purely diagnostic, so any failure to parse is ignored.
     */
    private fun addTokenAgeAttributes(span: Span, accessToken: String) {
        runCatching {
            val claims = SignedJWT.parse(accessToken).jwtClaimsSet
            val issuedAt = claims.issueTime?.toInstant() ?: return
            val expiresAt = claims.expirationTime?.toInstant() ?: return

            val lifetimeSeconds = expiresAt.epochSecond - issuedAt.epochSecond
            val remainingSeconds = expiresAt.epochSecond - Instant.now().epochSecond

            span.setAttribute(LIFETIME_SECONDS, lifetimeSeconds)
            span.setAttribute(REMAINING_SECONDS, remainingSeconds)
            span.setAttribute(CACHE_HIT, remainingSeconds < lifetimeSeconds - CACHE_HIT_SLACK_SECONDS)
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
