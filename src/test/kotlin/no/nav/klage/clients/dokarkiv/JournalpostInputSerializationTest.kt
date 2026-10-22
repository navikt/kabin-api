package no.nav.klage.clients.dokarkiv

import no.nav.klage.kodeverk.Tema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper

class JournalpostInputSerializationTest {
    private val objectMapper = jacksonObjectMapper()

    private val bruker = Bruker(id = "12345678910", idType = BrukerIdType.FNR)

    @Test
    fun `Sak for GENERELL_SAK does not contain fagsak fields`() {
        val json =
            objectMapper.writeValueAsString(
                UpdateSakInJournalpostRequest(
                    tema = Tema.OPP,
                    bruker = bruker,
                    sak = Sak(sakstype = Sakstype.GENERELL_SAK, fagsaksystem = null, fagsakid = null),
                    journalfoerendeEnhet = "4200",
                ),
            )

        assertThat(json).contains("\"sakstype\":\"GENERELL_SAK\"")
        assertThat(json).doesNotContain("fagsakid", "fagsakId", "fagsaksystem", "arkivsaksnummer")
    }

    @Test
    fun `CreateNewJournalpostBasedOnExistingJournalpostRequest for GENERELL_SAK does not contain fagsak fields`() {
        val json =
            objectMapper.writeValueAsString(
                CreateNewJournalpostBasedOnExistingJournalpostRequest(
                    sakstype = Sakstype.GENERELL_SAK,
                    fagsakId = null,
                    fagsaksystem = null,
                    tema = Tema.OPP,
                    bruker = bruker,
                    journalfoerendeEnhet = "4200",
                ),
            )

        assertThat(json).contains("\"sakstype\":\"GENERELL_SAK\"", "\"journalfoerendeEnhet\":\"4200\"", "\"bruker\"", "\"tema\"")
        assertThat(json).doesNotContain("fagsakid", "fagsakId", "fagsaksystem", "arkivsaksnummer")
    }

    @Test
    fun `Sak for FAGSAK contains fagsak fields`() {
        val json =
            objectMapper.writeValueAsString(
                Sak(sakstype = Sakstype.FAGSAK, fagsaksystem = FagsaksSystem.FS38, fagsakid = "fagsak-1"),
            )

        assertThat(json).contains("\"fagsakid\":\"fagsak-1\"", "\"fagsaksystem\":\"FS38\"")
    }
}
