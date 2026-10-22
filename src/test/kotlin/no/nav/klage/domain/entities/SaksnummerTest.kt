package no.nav.klage.domain.entities

import no.nav.klage.clients.saf.graphql.Sak
import no.nav.klage.kodeverk.Sakstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SaksnummerTest {
    @Test
    fun `fagsak gives fagsakId`() {
        assertThat(getSaksnummer(sakstype = Sakstype.FAGSAK, fagsakId = "123", arkivsaksnummer = "140411735")).isEqualTo("123")
    }

    @Test
    fun `generell sak gives arkivsaksnummer`() {
        assertThat(getSaksnummer(sakstype = Sakstype.GENERELL_SAK, fagsakId = null, arkivsaksnummer = "140411735")).isEqualTo("140411735")
    }

    @Test
    fun `saf sak uses sakstype to choose saksnummer`() {
        assertThat(safSak(sakstype = "FAGSAK", fagsakId = "123").getSaksnummer()).isEqualTo("123")
        assertThat(safSak(sakstype = "GENERELL_SAK", fagsakId = null).getSaksnummer()).isEqualTo("140411735")
        assertThat(safSak(sakstype = "ARKIVSAK", fagsakId = null).getSaksnummer()).isEqualTo("140411735")
    }

    @Test
    fun `saf fagsak without fagsakId gives arkivsaksnummer`() {
        assertThat(safSak(sakstype = "FAGSAK", fagsakId = null).getSaksnummer()).isEqualTo("140411735")
    }

    @Test
    fun `saf sak without sakstype gives arkivsaksnummer`() {
        assertThat(safSak(sakstype = null, fagsakId = null).getSaksnummer()).isEqualTo("140411735")
    }

    private fun safSak(
        sakstype: String?,
        fagsakId: String?,
    ) = Sak(
        datoOpprettet = null,
        fagsakId = fagsakId,
        fagsaksystem = "FS22",
        sakstype = sakstype,
        arkivsaksnummer = "140411735",
    )
}
