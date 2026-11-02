package no.nav.klage.util

import io.mockk.every
import io.mockk.mockk
import no.nav.klage.auth.TexasClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class TokenUtilTest {
    private val texasClient = mockk<TexasClient>()
    private val tokenUtil =
        TokenUtil(
            texasClient = texasClient,
            pdlScope = "pdl",
            kabalApiScope = "kabal-api",
            kabalInnstillingerScope = "kabal-innstillinger",
            kabalFileApiScope = "kabal-file-api",
            safScope = "saf",
            gosysOppgaveScope = "gosys-oppgave",
            dokArkivScope = "dok-arkiv",
            klageFssProxyScope = "klage-fss-proxy",
            klankeScope = "klanke",
            klageLookupScope = "klage-lookup",
        )

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `reads NAVident and token value from the current token`() {
        authenticate(claims = mapOf("NAVident" to "Z123456"))

        assertThat(tokenUtil.getCurrentIdent()).isEqualTo("Z123456")
        assertThat(tokenUtil.getAccessTokenFrontendSent()).isEqualTo("frontend-token")
    }

    @Test
    fun `fails without NAVident or token`() {
        assertThrows<RuntimeException> { tokenUtil.getAccessTokenFrontendSent() }

        authenticate(claims = mapOf("idtyp" to "app"))
        assertThrows<RuntimeException> { tokenUtil.getCurrentIdent() }
    }

    @Test
    fun `asks Texas for on-behalf-of tokens with the right targets`() {
        every { texasClient.onBehalfOfCurrentUser(any()) } answers { "obo-${firstArg<String>()}" }

        assertThat(tokenUtil.getOnBehalfOfTokenWithPdlScope()).isEqualTo("obo-pdl")
        assertThat(tokenUtil.getOnBehalfOfTokenWithKabalApiScope()).isEqualTo("obo-kabal-api")
        assertThat(tokenUtil.getOnBehalfOfTokenWithKabalInnstillingerScope()).isEqualTo("obo-kabal-innstillinger")
        assertThat(tokenUtil.getOnBehalfOfTokenWithKabalFileApiScope()).isEqualTo("obo-kabal-file-api")
        assertThat(tokenUtil.getOnBehalfOfTokenWithSafScope()).isEqualTo("obo-saf")
        assertThat(tokenUtil.getOnBehalfOfTokenWithGosysOppgaveScope()).isEqualTo("obo-gosys-oppgave")
        assertThat(tokenUtil.getOnBehalfOfTokenWithDokArkivScope()).isEqualTo("obo-dok-arkiv")
        assertThat(tokenUtil.getOnBehalfOfTokenWithKlageFSSProxyScope()).isEqualTo("obo-klage-fss-proxy")
        assertThat(tokenUtil.getOnBehalfOfTokenWithKlankeScope()).isEqualTo("obo-klanke")
        assertThat(tokenUtil.getOnBehalfOfTokenWithKlageLookupScope()).isEqualTo("obo-klage-lookup")
    }

    @Test
    fun `asks Texas for machine-to-machine tokens with the right targets`() {
        every { texasClient.machineToMachineToken(any()) } answers { "m2m-${firstArg<String>()}" }

        assertThat(tokenUtil.getMaskinTilMaskinTokenWithKabalApiScope()).isEqualTo("m2m-kabal-api")
        assertThat(tokenUtil.getMaskinTilMaskinTokenWithKlageFSSProxyScope()).isEqualTo("m2m-klage-fss-proxy")
        assertThat(tokenUtil.getMaskinTilMaskinTokenWithKlankeScope()).isEqualTo("m2m-klanke")
        assertThat(tokenUtil.getMaskinTilMaskinTokenWithKlageLookupScope()).isEqualTo("m2m-klage-lookup")
    }

    private fun authenticate(claims: Map<String, Any>) {
        val jwt =
            Jwt
                .withTokenValue("frontend-token")
                .header("alg", "RS256")
                .claim("sub", "subject")
                .claims { it.putAll(claims) }
                .build()
        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt)
    }
}
