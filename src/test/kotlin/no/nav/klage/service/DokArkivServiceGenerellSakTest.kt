package no.nav.klage.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.klage.clients.dokarkiv.BrukerIdType
import no.nav.klage.clients.dokarkiv.CreateNewJournalpostBasedOnExistingJournalpostRequest
import no.nav.klage.clients.dokarkiv.CreateNewJournalpostBasedOnExistingJournalpostResponse
import no.nav.klage.clients.dokarkiv.DokArkivClient
import no.nav.klage.clients.dokarkiv.Sak
import no.nav.klage.clients.dokarkiv.Sakstype
import no.nav.klage.clients.dokarkiv.UpdateSakInJournalpostRequest
import no.nav.klage.clients.saf.graphql.AvsenderMottaker
import no.nav.klage.clients.saf.graphql.Bruker
import no.nav.klage.clients.saf.graphql.DokumentInfo
import no.nav.klage.clients.saf.graphql.Journalpost
import no.nav.klage.clients.saf.graphql.Journalposttype
import no.nav.klage.clients.saf.graphql.Journalstatus
import no.nav.klage.domain.entities.Mulighet
import no.nav.klage.domain.entities.PartId
import no.nav.klage.domain.entities.PartWithUtsendingskanal
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.PartIdType
import no.nav.klage.kodeverk.Tema
import no.nav.klage.util.TokenUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import no.nav.klage.clients.saf.graphql.Sak as SafSak
import no.nav.klage.clients.saf.graphql.Tema as SafTema
import no.nav.klage.kodeverk.Sakstype as KodeverkSakstype

/**
 * Om en journalpost allerede er knyttet til en generell sak avgjøres av sakstype og tema.
 * Bruker er alltid sakenGjelder, så den sjekkes ikke her.
 */
class DokArkivServiceGenerellSakTest {
    private val dokArkivClient: DokArkivClient = mockk()
    private val safService: SafService = mockk()
    private val tokenUtil: TokenUtil = mockk()
    private val saksbehandlerService: SaksbehandlerService = mockk()

    private val mulighet = mockk<Mulighet>()
    private val registrering = mockk<Registrering>()

    private lateinit var dokArkivService: DokArkivService

    private val journalpostId = "12345"
    private val nyJournalpostId = "67890"
    private val fnr = "28838098519"
    private val enhetsnummer = "4295"

    private val generellSak =
        SafSak(
            datoOpprettet = LocalDateTime.now(),
            fagsakId = null,
            fagsaksystem = "FS22",
            sakstype = "GENERELL_SAK",
            arkivsaksnummer = "140411735",
        )

    private val fagsak =
        SafSak(
            datoOpprettet = LocalDateTime.now(),
            fagsakId = "fagsakId",
            fagsaksystem = "FS38",
            sakstype = "FAGSAK",
            arkivsaksnummer = null,
        )

    private val expectedBruker =
        no.nav.klage.clients.dokarkiv
            .Bruker(id = fnr, idType = BrukerIdType.FNR)
    private val expectedGenerellSak = Sak(sakstype = Sakstype.GENERELL_SAK, fagsaksystem = null, fagsakid = null)

    @BeforeEach
    fun setup() {
        dokArkivService =
            DokArkivService(
                dokArkivClient = dokArkivClient,
                safService = safService,
                kabalInnstillingerClient = mockk(),
                kabalApiService = mockk(),
                gosysOppgaveClient = mockk(relaxed = true),
                tokenUtil = tokenUtil,
                saksbehandlerService = saksbehandlerService,
            )

        every { registrering.isBasedOnUploadedDocument() } returns false
        every { registrering.getCurrentMulighet() } returns mulighet
        every { registrering.journalpostId } returns journalpostId
        every { registrering.avsender } returns null
        every { mulighet.tema } returns Tema.OMS
        every { mulighet.currentFagsystem } returns Fagsystem.KABAL
        every { mulighet.originalFagsystem } returns Fagsystem.FS22
        every { mulighet.fagsakId } returns null
        every { mulighet.sakstype } returns KodeverkSakstype.GENERELL_SAK
        every { mulighet.arkivsaksnummer } returns "140411735"
        every { mulighet.klageBehandlendeEnhet } returns enhetsnummer
        every { mulighet.sakenGjelder } returns
            PartWithUtsendingskanal(
                part = PartId(value = fnr, type = PartIdType.PERSON),
                name = "FORNAVN ETTERNAVN",
                available = true,
                language = null,
                address = null,
                utsendingskanal = PartWithUtsendingskanal.Utsendingskanal.NAV_NO,
            )
        every { tokenUtil.getCurrentIdent() } returns "ident"
        every { saksbehandlerService.getSaksbehandlerPersonligInfo(any()) } returns mockk { every { enhet.enhetId } returns enhetsnummer }
        every { dokArkivClient.createNewJournalpostBasedOnExistingJournalpost(payload = any(), oldJournalpostId = any()) } returns
            CreateNewJournalpostBasedOnExistingJournalpostResponse(nyJournalpostId = nyJournalpostId)
        every { dokArkivClient.updateSakInJournalpost(journalpostId = any(), input = any()) } returns Unit
        every { dokArkivClient.finalizeJournalpost(journalpostId = any(), journalfoerendeEnhet = any()) } returns Unit
    }

    @Test
    fun `ferdigstilt journalpost paa generell sak med samme tema er knyttet - returneres uendret`() {
        every { safService.getJournalpostAsSaksbehandler(any()) } returns
            journalpost(journalstatus = Journalstatus.JOURNALFOERT, sak = generellSak, tema = SafTema.OMS)

        val result = dokArkivService.handleJournalpost(registrering = registrering)

        assertThat(result).isEqualTo(journalpostId)
        verify(exactly = 0) { dokArkivClient.createNewJournalpostBasedOnExistingJournalpost(payload = any(), oldJournalpostId = any()) }
        verify(exactly = 0) { dokArkivClient.updateSakInJournalpost(journalpostId = any(), input = any()) }
    }

    @Test
    fun `ferdigstilt journalpost paa generell sak med annet tema er ikke knyttet - knyttes til generell sak`() {
        every { safService.getJournalpostAsSaksbehandler(any()) } returns
            journalpost(journalstatus = Journalstatus.JOURNALFOERT, sak = generellSak, tema = SafTema.SYK)

        val result = dokArkivService.handleJournalpost(registrering = registrering)

        assertThat(result).isEqualTo(nyJournalpostId)
        verifyKnyttetTilGenerellSak()
    }

    @Test
    fun `ferdigstilt journalpost paa fagsak med samme tema er ikke knyttet - knyttes til generell sak`() {
        every { safService.getJournalpostAsSaksbehandler(any()) } returns
            journalpost(journalstatus = Journalstatus.JOURNALFOERT, sak = fagsak, tema = SafTema.OMS)

        val result = dokArkivService.handleJournalpost(registrering = registrering)

        assertThat(result).isEqualTo(nyJournalpostId)
        verifyKnyttetTilGenerellSak()
    }

    @Test
    fun `ferdigstilt journalpost uten sak er ikke knyttet - knyttes til generell sak`() {
        every { safService.getJournalpostAsSaksbehandler(any()) } returns
            journalpost(journalstatus = Journalstatus.JOURNALFOERT, sak = null, tema = SafTema.OMS)

        val result = dokArkivService.handleJournalpost(registrering = registrering)

        assertThat(result).isEqualTo(nyJournalpostId)
        verifyKnyttetTilGenerellSak()
    }

    @Test
    fun `ikke ferdigstilt journalpost uten sak - sak settes til generell sak foer ferdigstilling`() {
        every { safService.getJournalpostAsSaksbehandler(any()) } returns
            journalpost(journalstatus = Journalstatus.MOTTATT, sak = null, tema = SafTema.OMS)

        val result = dokArkivService.handleJournalpost(registrering = registrering)

        assertThat(result).isEqualTo(journalpostId)
        verify(exactly = 1) {
            dokArkivClient.updateSakInJournalpost(
                journalpostId = journalpostId,
                input =
                    eq(
                        UpdateSakInJournalpostRequest(
                            tema = Tema.OMS,
                            bruker = expectedBruker,
                            sak = expectedGenerellSak,
                            journalfoerendeEnhet = enhetsnummer,
                        ),
                    ),
            )
        }
        verify(exactly = 1) { dokArkivClient.finalizeJournalpost(journalpostId = journalpostId, journalfoerendeEnhet = enhetsnummer) }
    }

    @Test
    fun `ikke ferdigstilt journalpost paa generell sak med samme tema - ferdigstilles uten aa endre sak`() {
        every { safService.getJournalpostAsSaksbehandler(any()) } returns
            journalpost(journalstatus = Journalstatus.MOTTATT, sak = generellSak, tema = SafTema.OMS)

        val result = dokArkivService.handleJournalpost(registrering = registrering)

        assertThat(result).isEqualTo(journalpostId)
        verify(exactly = 0) { dokArkivClient.updateSakInJournalpost(journalpostId = any(), input = any()) }
        verify(exactly = 1) { dokArkivClient.finalizeJournalpost(journalpostId = journalpostId, journalfoerendeEnhet = enhetsnummer) }
    }

    private fun verifyKnyttetTilGenerellSak() {
        verify(exactly = 1) {
            dokArkivClient.createNewJournalpostBasedOnExistingJournalpost(
                payload =
                    eq(
                        CreateNewJournalpostBasedOnExistingJournalpostRequest(
                            sakstype = Sakstype.GENERELL_SAK,
                            fagsakId = null,
                            fagsaksystem = null,
                            tema = Tema.OMS,
                            bruker = expectedBruker,
                            journalfoerendeEnhet = enhetsnummer,
                        ),
                    ),
                oldJournalpostId = journalpostId,
            )
        }
        verify(exactly = 0) { dokArkivClient.updateSakInJournalpost(journalpostId = any(), input = any()) }
    }

    private fun journalpost(
        journalstatus: Journalstatus,
        sak: SafSak?,
        tema: SafTema,
    ): Journalpost =
        Journalpost(
            journalpostId = journalpostId,
            tittel = "tittel",
            journalposttype = Journalposttype.I,
            journalstatus = journalstatus,
            bruker = Bruker(id = fnr, type = Bruker.BrukerIdType.FNR),
            tema = tema,
            temanavn = null,
            behandlingstema = null,
            behandlingstemanavn = null,
            sak = sak,
            avsenderMottaker =
                AvsenderMottaker(
                    id = fnr,
                    type = AvsenderMottaker.AvsenderMottakerIdType.FNR,
                    navn = null,
                    land = null,
                    erLikBruker = true,
                ),
            journalfoerendeEnhet = enhetsnummer,
            journalfortAvNavn = null,
            opprettetAvNavn = null,
            skjerming = null,
            datoOpprettet = LocalDateTime.now(),
            datoSortering = LocalDateTime.now(),
            dokumenter =
                listOf(
                    DokumentInfo(
                        dokumentInfoId = "",
                        tittel = null,
                        brevkode = null,
                        skjerming = null,
                        dokumentvarianter = listOf(),
                        logiskeVedlegg = null,
                    ),
                ),
            relevanteDatoer = listOf(),
            antallRetur = null,
            tilleggsopplysninger = listOf(),
            kanal = "NAV_NO",
            kanalnavn = "",
            utsendingsinfo = null,
        )
}
