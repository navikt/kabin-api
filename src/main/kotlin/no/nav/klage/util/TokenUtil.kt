package no.nav.klage.util

import com.nimbusds.jwt.SignedJWT
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import no.nav.klage.auth.TexasClient
import no.nav.klage.auth.currentAzureToken
import no.nav.klage.auth.navIdent
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TokenUtil(
    private val texasClient: TexasClient,
    @Value($$"api://${PDL_SCOPE}/.default") private val pdlScope: String,
    @Value($$"api://${NAIS_CLUSTER_NAME}.${NAIS_NAMESPACE}.${KABAL_API_APP_NAME}/.default") private val kabalApiScope: String,
    @Value($$"api://${NAIS_CLUSTER_NAME}.${NAIS_NAMESPACE}.${KABAL_INNSTILLINGER_APP_NAME}/.default")
    private val kabalInnstillingerScope: String,
    @Value($$"api://${NAIS_CLUSTER_NAME}.${NAIS_NAMESPACE}.${KABAL_FILE_API_APP_NAME}/.default") private val kabalFileApiScope: String,
    @Value($$"api://${SAF_SCOPE}/.default") private val safScope: String,
    @Value($$"api://${GOSYS_OPPGAVE_SCOPE}/.default") private val gosysOppgaveScope: String,
    @Value($$"api://${DOK_ARKIV_SCOPE}/.default") private val dokArkivScope: String,
    @Value($$"api://${FSS_CLUSTER}.klage.klage-fss-proxy/.default") private val klageFssProxyScope: String,
    @Value($$"api://${FSS_CLUSTER}.klage.klanke/.default") private val klankeScope: String,
    @Value($$"api://${NAIS_CLUSTER_NAME}.${NAIS_NAMESPACE}.${KLAGE_LOOKUP_APP_NAME}/.default") private val klageLookupScope: String,
) {
    companion object {
        private const val INSTRUMENTATION_SCOPE = "no.nav.klage.kabin-api"
        private const val SPAN_NAME = "azuread.token.acquire"

        private val REGISTRATION = AttributeKey.stringKey("token.client.registration")
        private val GRANT_TYPE = AttributeKey.stringKey("token.grant_type")
        private val SCOPE = AttributeKey.stringKey("token.scope")
        private val LIFETIME_SECONDS = AttributeKey.longKey("token.lifetime_seconds")
        private val REMAINING_SECONDS = AttributeKey.longKey("token.remaining_seconds")
        private val ISSUED_SECONDS_AGO = AttributeKey.longKey("token.issued_seconds_ago")
    }

    private enum class GrantType(
        val value: String,
    ) {
        ON_BEHALF_OF("urn:ietf:params:oauth:grant-type:jwt-bearer"),
        CLIENT_CREDENTIALS("client_credentials"),
    }

    private val tracer = GlobalOpenTelemetry.getTracer(INSTRUMENTATION_SCOPE)

    /**
     * Wraps token acquisition in its own span so that time spent on Texas cache misses and calls to
     * Entra ID is attributable in traces instead of showing up as an unexplained gap in the parent span.
     */
    private fun getAccessToken(
        registration: String,
        grantType: GrantType,
        scope: String,
    ): String {
        val span =
            tracer
                .spanBuilder(SPAN_NAME)
                .setAttribute(REGISTRATION, registration)
                .setAttribute(GRANT_TYPE, grantType.value)
                .setAttribute(SCOPE, scope)
                .startSpan()

        return try {
            val accessToken =
                when (grantType) {
                    GrantType.ON_BEHALF_OF -> texasClient.onBehalfOfCurrentUser(scope)
                    GrantType.CLIENT_CREDENTIALS -> texasClient.machineToMachineToken(scope)
                }
            addTokenAgeAttributes(span, accessToken)
            accessToken
        } catch (e: Throwable) {
            span.setStatus(StatusCode.ERROR, e.message ?: e.javaClass.simpleName)
            span.recordException(e)
            throw e
        } finally {
            span.end()
        }
    }

    /**
     * Records how long ago the token was issued, and how much lifetime is left.
     * Purely diagnostic, so any failure to parse is ignored.
     */
    private fun addTokenAgeAttributes(
        span: Span,
        accessToken: String,
    ) {
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

    private fun onBehalfOf(
        target: String,
        scope: String,
    ) = getAccessToken(registration = "$target-onbehalfof", grantType = GrantType.ON_BEHALF_OF, scope = scope)

    private fun machineToMachine(
        target: String,
        scope: String,
    ) = getAccessToken(registration = "$target-maskintilmaskin", grantType = GrantType.CLIENT_CREDENTIALS, scope = scope)

    fun getOnBehalfOfTokenWithPdlScope(): String = onBehalfOf(target = "pdl", scope = pdlScope)

    fun getOnBehalfOfTokenWithKabalApiScope(): String = onBehalfOf(target = "kabal-api", scope = kabalApiScope)

    fun getMaskinTilMaskinTokenWithKabalApiScope(): String = machineToMachine(target = "kabal-api", scope = kabalApiScope)

    fun getOnBehalfOfTokenWithKabalInnstillingerScope(): String =
        onBehalfOf(target = "kabal-innstillinger", scope = kabalInnstillingerScope)

    fun getOnBehalfOfTokenWithKabalFileApiScope(): String = onBehalfOf(target = "kabal-file-api", scope = kabalFileApiScope)

    fun getOnBehalfOfTokenWithSafScope(): String = onBehalfOf(target = "saf", scope = safScope)

    fun getOnBehalfOfTokenWithGosysOppgaveScope(): String = onBehalfOf(target = "gosys-oppgave", scope = gosysOppgaveScope)

    fun getOnBehalfOfTokenWithDokArkivScope(): String = onBehalfOf(target = "dok-arkiv", scope = dokArkivScope)

    fun getOnBehalfOfTokenWithKlageFSSProxyScope(): String = onBehalfOf(target = "klage-fss-proxy", scope = klageFssProxyScope)

    fun getMaskinTilMaskinTokenWithKlageFSSProxyScope(): String = machineToMachine(target = "klage-fss-proxy", scope = klageFssProxyScope)

    fun getOnBehalfOfTokenWithKlankeScope(): String = onBehalfOf(target = "klanke", scope = klankeScope)

    fun getMaskinTilMaskinTokenWithKlankeScope(): String = machineToMachine(target = "klanke", scope = klankeScope)

    fun getOnBehalfOfTokenWithKlageLookupScope(): String = onBehalfOf(target = "klage-lookup", scope = klageLookupScope)

    fun getMaskinTilMaskinTokenWithKlageLookupScope(): String = machineToMachine(target = "klage-lookup", scope = klageLookupScope)

    fun getAccessTokenFrontendSent(): String = currentAzureToken()?.tokenValue ?: throw RuntimeException("No token in current request")

    fun getCurrentIdent(): String = currentAzureToken()?.navIdent ?: throw RuntimeException("Ident not found in token")
}
