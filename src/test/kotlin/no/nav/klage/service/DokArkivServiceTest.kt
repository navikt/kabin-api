package no.nav.klage.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.klage.api.controller.view.PartId
import no.nav.klage.clients.dokarkiv.*
import no.nav.klage.clients.dokarkiv.BrukerIdType
import no.nav.klage.clients.dokarkiv.Sak
import no.nav.klage.clients.kabalapi.CompletedKlagebehandling
import no.nav.klage.clients.kabalapi.NavnView
import no.nav.klage.clients.kabalapi.PartView
import no.nav.klage.clients.kabalapi.PersonView
import no.nav.klage.clients.saf.graphql.*
import no.nav.klage.clients.saf.graphql.AvsenderMottaker
import no.nav.klage.clients.saf.graphql.Tema.OMS
import no.nav.klage.exceptions.SectionedValidationErrorWithDetailsException
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.Tema
import no.nav.klage.kodeverk.Utfall
import no.nav.klage.kodeverk.Ytelse
import no.nav.klage.util.TokenUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

class DokArkivServiceTest {

    val dokArkivClient: DokArkivClient = mockk()

    val genericApiService: GenericApiService = mockk()

    val safGraphQlClient: SafGraphQlClient = mockk()

    val tokenUtil: TokenUtil = mockk()

    lateinit var dokArkivService: DokArkivService

    private val SAKS_ID = "SAKS_ID"
    private val FAGSYSTEM = Fagsystem.FS38
    private val FNR = "28838098519"
    private val PERSON = PersonView(
        foedselsnummer = FNR,
        navn = NavnView(
            fornavn = "FORNAVN",
            mellomnavn = null,
            etternavn = "ETTERNAVN"
        ),
        kjoenn = null
    )
    private val avsenderMottaker = AvsenderMottaker(
        id = "12345678910",
        type = AvsenderMottaker.AvsenderMottakerIdType.FNR,
        navn = null,
        land = null,
        erLikBruker = false
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
            genericApiService = genericApiService,
            safGraphQlClient = safGraphQlClient,
            tokenUtil = tokenUtil
        )
    }

    @Test
    fun getSakWorksAsExpected() {
        val input = CompletedKlagebehandling(
            behandlingId = UUID.randomUUID(),
            ytelseId = "",
            utfallId = "",
            vedtakDate = LocalDateTime.now(),
            sakenGjelder = PartView(person = null, virksomhet = null),
            klager = PartView(person = null, virksomhet = null),
            fullmektig = null,
            tilknyttedeDokumenter = listOf(),
            sakFagsakId = SAKS_ID,
            fagsakId = SAKS_ID,
            sakFagsystem = FAGSYSTEM,
            fagsystem = FAGSYSTEM,
            fagsystemId = FAGSYSTEM.id,
            klageBehandlendeEnhet = "",
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
        fun `unfinished journalpost with avsender - No avsender in request - Sak is updated and journalpost is finalized`() {
            every { genericApiService.getCompletedKlagebehandling(any()) } returns getCompletedKlagebehandling()
            every { safGraphQlClient.getJournalpostAsSaksbehandler(any()) } returns getMottattIncomingJournalpostWithAvsenderMottaker()
            every { dokArkivClient.updateSakInJournalpost(any(), any()) } returns Unit
            every { dokArkivClient.finalizeJournalpost(any(), any()) } returns Unit

            val resultingJournalpost = dokArkivService.handleJournalpost(JOURNALPOST_ID, UUID.randomUUID(), null)

            verify(exactly = 1) {
                dokArkivClient.updateSakInJournalpost(
                    journalpostId = any(),
                    input = eq(
                        UpdateSakInJournalpostRequest(
                            tema = Tema.OMS,
                            bruker = Bruker(
                                id = FNR,
                                idType = BrukerIdType.FNR
                            ),
                            sak = Sak(
                                sakstype = Sakstype.FAGSAK,
                                fagsaksystem = FagsaksSystem.FS38,
                                fagsakid = SAKS_ID,
                            ),
                            journalfoerendeEnhet = ENHET,
                        )
                    ),
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

            verify(exactly = 0) {
                dokArkivClient.updateAvsenderMottakerInJournalpost(
                    journalpostId = any(),
                    input = any()
                )
            }

            assertEquals(JOURNALPOST_ID, resultingJournalpost)
        }

        @Test
        fun `unfinished journalpost without avsender - Avsender in request - Sak and avsender is updated and journalpost is finalized`() {
            every { genericApiService.getCompletedKlagebehandling(any()) } returns getCompletedKlagebehandling()
            every { safGraphQlClient.getJournalpostAsSaksbehandler(any()) } returns getMottattIncomingJournalpost()
            every { dokArkivClient.updateSakInJournalpost(any(), any()) } returns Unit
            every { dokArkivClient.updateAvsenderMottakerInJournalpost(any(), any()) } returns Unit
            every { dokArkivClient.finalizeJournalpost(any(), any()) } returns Unit

            val resultingJournalpost = dokArkivService.handleJournalpost(
                journalpostId = JOURNALPOST_ID,
                klagebehandlingId = UUID.randomUUID(),
                avsender = PartId(
                    type = no.nav.klage.api.controller.view.PartView.PartType.FNR,
                    id = FNR
                )
            )

            verify(exactly = 1) {
                dokArkivClient.updateAvsenderMottakerInJournalpost(
                    journalpostId = any(),
                    input = eq(
                        UpdateAvsenderMottakerInJournalpostRequest(
                            avsenderMottaker = no.nav.klage.clients.dokarkiv.AvsenderMottaker(
                                id = FNR,
                                idType = AvsenderMottakerIdType.FNR,
                            )
                        )
                    )
                )
            }

            verify(exactly = 1) {
                dokArkivClient.updateSakInJournalpost(
                    journalpostId = any(),
                    input = eq(
                        UpdateSakInJournalpostRequest(
                            tema = Tema.OMS,
                            bruker = Bruker(
                                id = FNR,
                                idType = BrukerIdType.FNR
                            ),
                            sak = Sak(
                                sakstype = Sakstype.FAGSAK,
                                fagsaksystem = FagsaksSystem.FS38,
                                fagsakid = SAKS_ID,
                            ),
                            journalfoerendeEnhet = ENHET,
                        )
                    ),
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
        fun `unfinished journalpost without avsender - No avsender in request - throws validation error`() {
            every { genericApiService.getCompletedKlagebehandling(any()) } returns getCompletedKlagebehandling()
            every { safGraphQlClient.getJournalpostAsSaksbehandler(any()) } returns getMottattIncomingJournalpost()

            assertThrows<SectionedValidationErrorWithDetailsException> {
                dokArkivService.handleJournalpost(
                    JOURNALPOST_ID,
                    UUID.randomUUID(),
                    null
                )
            }
        }

        @Test
        fun `journalfoert incoming journalpost - No avsender in request, correct fagsak - returned directly`() {
            every { genericApiService.getCompletedKlagebehandling(any()) } returns getCompletedKlagebehandling()
            every { safGraphQlClient.getJournalpostAsSaksbehandler(any()) } returns getJournalfoertIncomingJournalpostWithDefinedFagsak()

            val resultingJournalpost = dokArkivService.handleJournalpost(JOURNALPOST_ID, UUID.randomUUID(), null)

            verify(exactly = 0) {
                dokArkivClient.updateAvsenderMottakerInJournalpost(
                    journalpostId = any(),
                    input = any(),
                )
            }

            verify(exactly = 0) {
                dokArkivClient.updateSakInJournalpost(
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
        fun `journalfoert incoming journalpost - Avsender in request, correct fagsak - Avsender updated, then returned`() {
            every { genericApiService.getCompletedKlagebehandling(any()) } returns getCompletedKlagebehandling()
            every { safGraphQlClient.getJournalpostAsSaksbehandler(any()) } returns getJournalfoertIncomingJournalpostWithDefinedFagsak()
            every { dokArkivClient.updateAvsenderMottakerInJournalpost(any(), any()) } returns Unit

            val resultingJournalpost = dokArkivService.handleJournalpost(
                journalpostId = JOURNALPOST_ID,
                klagebehandlingId = UUID.randomUUID(),
                avsender = PartId(
                    type = no.nav.klage.api.controller.view.PartView.PartType.FNR,
                    id = FNR
                )
            )

            verify(exactly = 1) {
                dokArkivClient.updateAvsenderMottakerInJournalpost(
                    journalpostId = any(),
                    input = eq(
                        UpdateAvsenderMottakerInJournalpostRequest(
                            avsenderMottaker = no.nav.klage.clients.dokarkiv.AvsenderMottaker(
                                id = FNR,
                                idType = AvsenderMottakerIdType.FNR,
                            )
                        )
                    )
                )
            }

            verify(exactly = 0) {
                dokArkivClient.updateSakInJournalpost(
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
        fun `journalfoert incoming journalpost - No avsender in request, incorrect fagsak - Handled correctly`() {
            every { genericApiService.getCompletedKlagebehandling(any()) } returns getCompletedKlagebehandling()
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

            val resultingJournalpost = dokArkivService.handleJournalpost(JOURNALPOST_ID, UUID.randomUUID(), null)

            verify(exactly = 0) {
                dokArkivClient.updateAvsenderMottakerInJournalpost(
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

    @Test
    fun `journalfoert incoming journalpost - Avsender in request, incorrect fagsak - Handled correctly`() {
        every { genericApiService.getCompletedKlagebehandling(any()) } returns getCompletedKlagebehandling()
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
        every { dokArkivClient.updateAvsenderMottakerInJournalpost(any(), any()) } returns Unit

        val resultingJournalpost = dokArkivService.handleJournalpost(
            journalpostId = JOURNALPOST_ID,
            klagebehandlingId = UUID.randomUUID(),
            avsender = PartId(
                type = no.nav.klage.api.controller.view.PartView.PartType.FNR,
                id = FNR
            )
        )

        verify(exactly = 1) {
            dokArkivClient.updateAvsenderMottakerInJournalpost(
                journalpostId = any(),
                input = eq(
                    UpdateAvsenderMottakerInJournalpostRequest(
                        avsenderMottaker = no.nav.klage.clients.dokarkiv.AvsenderMottaker(
                            id = FNR,
                            idType = AvsenderMottakerIdType.FNR,
                        )
                    )
                )
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


    private fun getCompletedKlagebehandling(): CompletedKlagebehandling {
        return CompletedKlagebehandling(
            behandlingId = UUID.randomUUID(),
            ytelseId = Ytelse.OMS_OLP.id,
            utfallId = Utfall.STADFESTELSE.id,
            vedtakDate = LocalDateTime.now(),
            sakenGjelder = PartView(
                person = PERSON,
                virksomhet = null
            ),
            klager = PartView(person = null, virksomhet = null),
            fullmektig = null,
            tilknyttedeDokumenter = listOf(),
            sakFagsakId = SAKS_ID,
            fagsakId = SAKS_ID,
            sakFagsystem = FAGSYSTEM,
            fagsystem = FAGSYSTEM,
            fagsystemId = FAGSYSTEM.id,
            klageBehandlendeEnhet = ENHET,
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
            avsenderMottaker = AvsenderMottaker(
                id = null,
                type = AvsenderMottaker.AvsenderMottakerIdType.NULL,
                navn = null,
                land = null,
                erLikBruker = false
            ),
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

    private fun getMottattIncomingJournalpostWithAvsenderMottaker(): Journalpost {
        return getMottattIncomingJournalpost().copy(
            avsenderMottaker = avsenderMottaker
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