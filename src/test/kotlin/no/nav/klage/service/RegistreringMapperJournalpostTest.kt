package no.nav.klage.service

import io.mockk.every
import io.mockk.mockk
import no.nav.klage.api.controller.view.Utsendingskanal
import no.nav.klage.clients.kabalapi.PartType
import no.nav.klage.clients.kabalapi.PartViewWithUtsendingskanal
import no.nav.klage.clients.saf.graphql.Bruker
import no.nav.klage.clients.saf.graphql.Journalpost
import no.nav.klage.clients.saf.graphql.Journalposttype
import no.nav.klage.clients.saf.graphql.Journalstatus
import no.nav.klage.clients.saf.graphql.Sak
import no.nav.klage.domain.entities.PartId
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.exceptions.IllegalInputException
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.PartIdType
import no.nav.klage.kodeverk.Sakstype
import no.nav.klage.kodeverk.Tema
import no.nav.klage.kodeverk.Type
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import no.nav.klage.clients.saf.graphql.Tema as SafTema

class RegistreringMapperJournalpostTest {
    private val fnr = "12345678910"

    private val kabalApiService: KabalApiService =
        mockk {
            every { searchPartWithUtsendingskanal(any()) } returns
                PartViewWithUtsendingskanal(
                    identifikator = fnr,
                    type = PartType.FNR,
                    name = "Test Person",
                    available = true,
                    statusList = emptyList(),
                    address = null,
                    utsendingskanal = Utsendingskanal.NAV_NO,
                    language = null,
                )
        }

    private val registrering: Registrering =
        mockk {
            every { type } returns Type.KLAGE
            every { sakenGjelder } returns PartId(type = PartIdType.PERSON, value = fnr)
        }

    @Test
    fun `journalpost with GENERELL_SAK and no fagsakId gives mulighet with sakstype GENERELL_SAK`() {
        val journalpost =
            journalpost(
                tema = SafTema.OPP,
                sak =
                    Sak(
                        datoOpprettet = LocalDateTime.parse("2026-10-06T10:52:10"),
                        fagsakId = null,
                        fagsaksystem = "FS22",
                        sakstype = "GENERELL_SAK",
                        arkivsaksnummer = "140411735",
                    ),
            )

        val mulighet = journalpost.toMulighet(kabalApiService = kabalApiService, registrering = registrering)

        assertThat(mulighet.sakstype).isEqualTo(Sakstype.GENERELL_SAK)
        assertThat(mulighet.fagsakId).isNull()
        assertThat(mulighet.arkivsaksnummer).isEqualTo("140411735")
        assertThat(mulighet.originalFagsystem).isEqualTo(Fagsystem.FS22)
        assertThat(mulighet.currentFagsystem).isEqualTo(Fagsystem.FS22)
        assertThat(mulighet.tema).isEqualTo(Tema.OPP)
        assertThat(mulighet.sakenGjelder.part.value).isEqualTo(fnr)
    }

    @Test
    fun `journalpost with FAGSAK gives mulighet with sakstype FAGSAK`() {
        val journalpost =
            journalpost(
                tema = SafTema.OMS,
                sak =
                    Sak(
                        datoOpprettet = null,
                        fagsakId = "fagsak-1",
                        fagsaksystem = "FS38",
                        sakstype = "FAGSAK",
                        arkivsaksnummer = null,
                    ),
            )

        val mulighet = journalpost.toMulighet(kabalApiService = kabalApiService, registrering = registrering)

        assertThat(mulighet.sakstype).isEqualTo(Sakstype.FAGSAK)
        assertThat(mulighet.fagsakId).isEqualTo("fagsak-1")
        assertThat(mulighet.arkivsaksnummer).isNull()
        assertThat(mulighet.originalFagsystem).isEqualTo(Fagsystem.FS38)
    }

    @Test
    fun `journalpost with FAGSAK without fagsakId gives understandable error`() {
        val journalpost =
            journalpost(
                tema = SafTema.OMS,
                sak = Sak(datoOpprettet = null, fagsakId = null, fagsaksystem = "FS38", sakstype = "FAGSAK", arkivsaksnummer = null),
            )

        val exception =
            assertThrows<IllegalInputException> {
                journalpost.toMulighet(kabalApiService = kabalApiService, registrering = registrering)
            }
        assertThat(exception.message).contains("mangler fagsakId")
    }

    @Test
    fun `journalpost with GENERELL_SAK without fagsaksystem gives understandable error`() {
        val journalpost =
            journalpost(
                tema = SafTema.OPP,
                sak =
                    Sak(
                        datoOpprettet = null,
                        fagsakId = null,
                        fagsaksystem = null,
                        sakstype = "GENERELL_SAK",
                        arkivsaksnummer = "140411735",
                    ),
            )

        val exception =
            assertThrows<IllegalInputException> {
                journalpost.toMulighet(kabalApiService = kabalApiService, registrering = registrering)
            }
        assertThat(exception.message).contains("mangler fagsaksystem")
    }

    @Test
    fun `journalpost without sak gives understandable error`() {
        val journalpost = journalpost(tema = SafTema.OMS, sak = null)

        val exception =
            assertThrows<IllegalInputException> {
                journalpost.toMulighet(kabalApiService = kabalApiService, registrering = registrering)
            }
        assertThat(exception.message).contains("ikke knyttet til en sak")
    }

    private fun journalpost(
        tema: SafTema,
        sak: Sak?,
    ): Journalpost =
        Journalpost(
            journalpostId = "123456789",
            tittel = null,
            journalposttype = Journalposttype.I,
            journalstatus = Journalstatus.JOURNALFOERT,
            bruker = Bruker(id = fnr, type = Bruker.BrukerIdType.FNR),
            tema = tema,
            temanavn = null,
            behandlingstema = null,
            behandlingstemanavn = null,
            sak = sak,
            avsenderMottaker = null,
            journalfoerendeEnhet = "4200",
            journalfortAvNavn = null,
            opprettetAvNavn = null,
            skjerming = null,
            datoOpprettet = LocalDateTime.now(),
            datoSortering = LocalDateTime.now(),
            dokumenter = emptyList(),
            relevanteDatoer = emptyList(),
            antallRetur = null,
            tilleggsopplysninger = emptyList(),
            kanal = "NAV_NO",
            kanalnavn = "",
            utsendingsinfo = null,
        )
}
