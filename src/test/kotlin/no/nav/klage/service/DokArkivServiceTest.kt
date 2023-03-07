package no.nav.klage.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.klage.clients.KabalApiClient
import no.nav.klage.clients.dokarkiv.*
import no.nav.klage.clients.dokarkiv.Sak
import no.nav.klage.clients.saf.graphql.*
import no.nav.klage.clients.saf.graphql.Tema.OMS
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.Utfall
import no.nav.klage.kodeverk.Ytelse
import no.nav.klage.util.TokenUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class DokArkivServiceTest {

    val dokArkivClient: DokArkivClient = mockk()

    val kabalApiService: KabalApiService = mockk()

    val safGraphQlClient: SafGraphQlClient = mockk()

    val tokenUtil: TokenUtil = mockk()

    lateinit var dokArkivService: DokArkivService

    private val SAKS_ID = "SAKS_ID"
    private val FAGSYSTEM = Fagsystem.FS38
    private val PERSON = KabalApiClient.PersonView(
        foedselsnummer = "28838098519",
        navn = KabalApiClient.NavnView(
            fornavn = "FORNAVN",
            mellomnavn = null,
            etternavn = "ETTERNAVN"
        ),
        kjoenn = null
    )
    private val ENHET = "4295"

    private val JOURNALPOST_ID = "12345"
    private val JOURNALPOST_ID_2 = "54321"
    private val TITTEL = "TITTEL"
    private val IDENT = "IDENT"

    @BeforeEach
    fun setup() {
        dokArkivService = DokArkivService(
            dokArkivClient = dokArkivClient,
            kabalApiService = kabalApiService,
            safGraphQlClient = safGraphQlClient,
            tokenUtil = tokenUtil
        )
    }

    @Test
    fun getSakWorksAsExpected() {
        val input = KabalApiClient.CompletedKlagebehandling(
            behandlingId = UUID.randomUUID(),
            ytelseId = "",
            utfallId = "",
            vedtakDate = LocalDateTime.now(),
            sakenGjelder = KabalApiClient.SakenGjelderView(person = null, virksomhet = null),
            klager = KabalApiClient.KlagerView(person = null, virksomhet = null),
            fullmektig = null,
            tilknyttedeDokumenter = listOf(),
            sakFagsakId = SAKS_ID,
            sakFagsystem = FAGSYSTEM,
            klageBehandlendeEnhet = "",
            alreadyUsedJournalpostIdList = listOf(),
        )

        val expectedOutput = Sak(
            sakstype = Sakstype.FAGSAK,
            fagsaksystem = FagsaksSystem.FS38,
            fagsakid = SAKS_ID
        )

        assertEquals(
            expectedOutput,
            dokArkivService.getSak(input)
        )
    }

    @Nested
    inner class HandleJournalpost {
        @Test
        fun `unfinished journalpost is updated and finalized`() {
            every { kabalApiService.getCompletedKlagebehandling(any()) } returns getCompletedKlagebehandling()
            every { safGraphQlClient.getJournalpostAsSaksbehandler(any()) } returns getMottattIncomingJournalpost()
            every { dokArkivClient.updateSaksId(any(), any()) } returns Unit
            every { dokArkivClient.finalizeJournalpost(any(), any()) } returns Unit

            val resultingJournalpost = dokArkivService.handleJournalpost(JOURNALPOST_ID, UUID.randomUUID())

            verify(exactly = 1) {
                dokArkivClient.updateSaksId(
                    journalpostId = any(),
                    input = any(),
                )
            }

            verify(exactly = 1) {
                dokArkivClient.finalizeJournalpost(
                    journalpostId = any(),
                    journalfoerendeEnhet = any(),
                )
            }

            verify(exactly = 0) {
                dokArkivClient.createNewJournalpostBasedOnExistingJournalpost(
                    payload = any(),
                    oldJournalpostId = any(),
                    journalfoerendeSaksbehandlerIdent = any()
                )
            }

            verify(exactly = 0) {
                dokArkivClient.registerErrorInSaksId(
                    journalpostId = any()
                )
            }

            assertEquals(JOURNALPOST_ID, resultingJournalpost)
        }

        @Test
        fun `journalfoert incoming journalpost with correct fagsak is returned directly`() {
            every { kabalApiService.getCompletedKlagebehandling(any()) } returns getCompletedKlagebehandling()
            every { safGraphQlClient.getJournalpostAsSaksbehandler(any()) } returns getJournalfoertIncomingJournalpostWithDefinedFagsak()

            val resultingJournalpost = dokArkivService.handleJournalpost(JOURNALPOST_ID, UUID.randomUUID())

            verify(exactly = 0) {
                dokArkivClient.updateSaksId(
                    journalpostId = any(),
                    input = any(),
                )
            }

            verify(exactly = 0) {
                dokArkivClient.finalizeJournalpost(
                    journalpostId = any(),
                    journalfoerendeEnhet = any(),
                )
            }

            verify(exactly = 0) {
                dokArkivClient.createNewJournalpostBasedOnExistingJournalpost(
                    payload = any(),
                    oldJournalpostId = any(),
                    journalfoerendeSaksbehandlerIdent = any()
                )
            }

            verify(exactly = 0) {
                dokArkivClient.registerErrorInSaksId(
                    journalpostId = any()
                )
            }

            assertEquals(JOURNALPOST_ID, resultingJournalpost)
        }

        @Test
        fun `journalfoert incoming journalpost with incorrect fagsak is handled correctly`() {
            every { kabalApiService.getCompletedKlagebehandling(any()) } returns getCompletedKlagebehandling()
            every { safGraphQlClient.getJournalpostAsSaksbehandler(any()) } returns getJournalfoertIncomingJournalpost()
            every { tokenUtil.getIdent() } returns IDENT
            every {
                dokArkivClient.createNewJournalpostBasedOnExistingJournalpost(
                    any(),
                    any(),
                    any()
                )
            } returns CreateNewJournalpostBasedOnExistingJournalpostResponse(JOURNALPOST_ID_2)
            every { dokArkivClient.registerErrorInSaksId(any()) } returns Unit

            val resultingJournalpost = dokArkivService.handleJournalpost(JOURNALPOST_ID, UUID.randomUUID())

            verify(exactly = 0) {
                dokArkivClient.updateSaksId(
                    journalpostId = any(),
                    input = any(),
                )
            }

            verify(exactly = 0) {
                dokArkivClient.finalizeJournalpost(
                    journalpostId = any(),
                    journalfoerendeEnhet = any(),
                )
            }

            verify(exactly = 1) {
                dokArkivClient.createNewJournalpostBasedOnExistingJournalpost(
                    payload = any(),
                    oldJournalpostId = any(),
                    journalfoerendeSaksbehandlerIdent = any()
                )
            }

            verify(exactly = 1) {
                dokArkivClient.registerErrorInSaksId(
                    journalpostId = any()
                )
            }

            assertEquals(JOURNALPOST_ID_2, resultingJournalpost)
        }
    }


    private fun getCompletedKlagebehandling(): KabalApiClient.CompletedKlagebehandling {
        return KabalApiClient.CompletedKlagebehandling(
            behandlingId = UUID.randomUUID(),
            ytelseId = Ytelse.OMS_OLP.id,
            utfallId = Utfall.STADFESTELSE.id,
            vedtakDate = LocalDateTime.now(),
            sakenGjelder = KabalApiClient.SakenGjelderView(
                person = PERSON,
                virksomhet = null
            ),
            klager = KabalApiClient.KlagerView(person = null, virksomhet = null),
            fullmektig = null,
            tilknyttedeDokumenter = listOf(),
            sakFagsakId = SAKS_ID,
            sakFagsystem = FAGSYSTEM,
            klageBehandlendeEnhet = ENHET,
            alreadyUsedJournalpostIdList = listOf(),
        )
    }

    private fun getMottattIncomingJournalpost(): Journalpost {
        return Journalpost(
            journalpostId = JOURNALPOST_ID,
            tittel = TITTEL,
            journalposttype = Journalposttype.I,
            journalstatus = Journalstatus.MOTTATT,
            tema = OMS,
            temanavn = null,
            behandlingstema = null,
            behandlingstemanavn = null,
            sak = null,
            avsenderMottaker = null,
            journalfoerendeEnhet = ENHET,
            journalfortAvNavn = null,
            opprettetAvNavn = null,
            skjerming = null,
            datoOpprettet = LocalDateTime.now(),
            dokumenter = listOf(),
            relevanteDatoer = listOf(),
            antallRetur = null,
            tilleggsopplysninger = listOf(),
            kanal = Kanal.NAV_NO,
            kanalnavn = "",
            utsendingsinfo = null
        )
    }

    private fun getJournalfoertIncomingJournalpost(): Journalpost {
        return getMottattIncomingJournalpost().copy(journalstatus = Journalstatus.JOURNALFOERT)
    }

    private fun getJournalfoertIncomingJournalpostWithDefinedFagsak(): Journalpost {
        return getMottattIncomingJournalpost().copy(
            journalstatus = Journalstatus.JOURNALFOERT,
            sak = no.nav.klage.clients.saf.graphql.Sak(
                datoOpprettet = null,
                fagsakId = SAKS_ID,
                fagsaksystem = FagsaksSystem.FS38.name
            )
        )
    }
}