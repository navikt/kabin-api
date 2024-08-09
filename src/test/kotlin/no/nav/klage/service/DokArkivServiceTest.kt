package no.nav.klage.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.klage.api.controller.view.PartIdInput
import no.nav.klage.api.controller.view.Utsendingskanal
import no.nav.klage.clients.KabalInnstillingerClient
import no.nav.klage.clients.dokarkiv.*
import no.nav.klage.clients.dokarkiv.BrukerIdType
import no.nav.klage.clients.dokarkiv.Sak
import no.nav.klage.clients.kabalapi.CompletedBehandling
import no.nav.klage.clients.kabalapi.PartType
import no.nav.klage.clients.kabalapi.PartView
import no.nav.klage.clients.kabalapi.PartViewWithUtsendingskanal
import no.nav.klage.clients.saf.graphql.*
import no.nav.klage.clients.saf.graphql.AvsenderMottaker
import no.nav.klage.clients.saf.graphql.Tema.OMS
import no.nav.klage.exceptions.SectionedValidationErrorWithDetailsException
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.Tema
import no.nav.klage.kodeverk.Ytelse
import no.nav.klage.kodeverk.hjemmel.Hjemmel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

class DokArkivServiceTest {

    val dokArkivClient: DokArkivClient = mockk()

    val kabalApiService: KabalApiService = mockk()

    val safService: SafService = mockk()

    private val fssProxyService: KlageFssProxyService = mockk()

    private val kabalInnstillingerClient: KabalInnstillingerClient = mockk()

    lateinit var dokArkivService: DokArkivService

    private val SAKS_ID = "SAKS_ID"
    private val FAGSYSTEM = Fagsystem.FS38
    private val FNR = "28838098519"
    private val PERSON = PartView(
        id = FNR,
        name = "FORNAVN ETTERNAVN",
        type = PartType.FNR,
        available = true,
        statusList = emptyList(),
        language = null,
        address = null,
    )
    private val PART_WITH_UTSENDINGSKANAL = PartViewWithUtsendingskanal(
        id = FNR,
        name = "FORNAVN ETTERNAVN",
        type = PartType.FNR,
        available = true,
        statusList = emptyList(),
        language = null,
        address = null,
        utsendingskanal = Utsendingskanal.NAV_NO,
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
            safService = safService,
            fssProxyService = fssProxyService,
            kabalInnstillingerClient = kabalInnstillingerClient,
            kabalApiService = kabalApiService,
            oppgaveClient = mockk(relaxed = true),
        )
    }

    @Nested
    inner class HandleJournalpost {
        @Test
        fun `unfinished journalpost with avsender - No avsender in request - Sak is updated and journalpost is finalized`() {
            every { kabalApiService.getCompletedBehandling(any()) } returns getCompletedKlagebehandling()
            every { safService.getJournalpostAsSaksbehandler(any()) } returns getMottattIncomingJournalpostWithAvsenderMottaker()
            every { dokArkivClient.updateSakInJournalpost(any(), any()) } returns Unit
            every { dokArkivClient.finalizeJournalpost(any(), any()) } returns Unit

            val resultingJournalpost = dokArkivService.handleJournalpostBasedOnKabalKlagebehandling(
                journalpostId = JOURNALPOST_ID,
                klagebehandlingId = UUID.randomUUID(),
                avsender = null,
            )

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
            every { kabalApiService.getCompletedBehandling(any()) } returns getCompletedKlagebehandling()
            every { kabalApiService.searchPart(any()) } returns PERSON
            every { safService.getJournalpostAsSaksbehandler(any()) } returns getMottattIncomingJournalpost()
            every { dokArkivClient.updateSakInJournalpost(any(), any()) } returns Unit
            every { dokArkivClient.updateAvsenderMottakerInJournalpost(any(), any()) } returns Unit
            every { dokArkivClient.finalizeJournalpost(any(), any()) } returns Unit

            val resultingJournalpost = dokArkivService.handleJournalpostBasedOnKabalKlagebehandling(
                journalpostId = JOURNALPOST_ID,
                klagebehandlingId = UUID.randomUUID(),
                avsender = PartIdInput(
                    type = no.nav.klage.api.controller.view.PartType.FNR,
                    id = FNR
                ),
            )

            verify(exactly = 1) {
                dokArkivClient.updateAvsenderMottakerInJournalpost(
                    journalpostId = any(),
                    input = eq(
                        UpdateAvsenderMottakerInJournalpostRequest(
                            avsenderMottaker = no.nav.klage.clients.dokarkiv.AvsenderMottaker(
                                id = FNR,
                                idType = AvsenderMottakerIdType.FNR,
                                navn = "FORNAVN ETTERNAVN",
                            ),
                        ),
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
                )
            }

            assertEquals(JOURNALPOST_ID, resultingJournalpost)
        }

        @Test
        fun `unfinished journalpost without avsender - No avsender in request - throws validation error`() {
            every { kabalApiService.getCompletedBehandling(any()) } returns getCompletedKlagebehandling()
            every { safService.getJournalpostAsSaksbehandler(any()) } returns getMottattIncomingJournalpost()

            assertThrows<SectionedValidationErrorWithDetailsException> {
                dokArkivService.handleJournalpostBasedOnKabalKlagebehandling(
                    journalpostId = JOURNALPOST_ID,
                    klagebehandlingId = UUID.randomUUID(),
                    avsender = null,
                )
            }
        }

        @Test
        fun `journalfoert incoming journalpost - No avsender in request, correct fagsak - returned directly`() {
            every { kabalApiService.getCompletedBehandling(any()) } returns getCompletedKlagebehandling()
            every { safService.getJournalpostAsSaksbehandler(any()) } returns getJournalfoertIncomingJournalpostWithDefinedFagsak()

            val resultingJournalpost = dokArkivService.handleJournalpostBasedOnKabalKlagebehandling(
                journalpostId = JOURNALPOST_ID,
                klagebehandlingId = UUID.randomUUID(),
                avsender = null,
            )

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
                )
            }

            assertEquals(JOURNALPOST_ID, resultingJournalpost)
        }

        @Test
        fun `journalfoert incoming journalpost - Avsender in request, correct fagsak - Avsender updated, then returned`() {
            every { kabalApiService.getCompletedBehandling(any()) } returns getCompletedKlagebehandling()
            every { kabalApiService.searchPart(any()) } returns PERSON
            every { safService.getJournalpostAsSaksbehandler(any()) } returns getJournalfoertIncomingJournalpostWithDefinedFagsak()
            every { dokArkivClient.updateAvsenderMottakerInJournalpost(any(), any()) } returns Unit

            val resultingJournalpost = dokArkivService.handleJournalpostBasedOnKabalKlagebehandling(
                journalpostId = JOURNALPOST_ID,
                klagebehandlingId = UUID.randomUUID(),
                avsender = PartIdInput(
                    type = no.nav.klage.api.controller.view.PartType.FNR,
                    id = FNR
                ),
            )

            verify(exactly = 1) {
                dokArkivClient.updateAvsenderMottakerInJournalpost(
                    journalpostId = any(),
                    input = eq(
                        UpdateAvsenderMottakerInJournalpostRequest(
                            avsenderMottaker = no.nav.klage.clients.dokarkiv.AvsenderMottaker(
                                id = FNR,
                                idType = AvsenderMottakerIdType.FNR,
                                navn = "FORNAVN ETTERNAVN",
                            ),
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
                )
            }

            assertEquals(JOURNALPOST_ID, resultingJournalpost)
        }

        @Test
        fun `journalfoert incoming journalpost - No avsender in request, incorrect fagsak - Handled correctly`() {
            every { kabalApiService.getCompletedBehandling(any()) } returns getCompletedKlagebehandling()
            every { safService.getJournalpostAsSaksbehandler(any()) } returns getJournalfoertIncomingJournalpost()
            every {
                dokArkivClient.createNewJournalpostBasedOnExistingJournalpost(
                    any(),
                    any(),
                )
            } returns CreateNewJournalpostBasedOnExistingJournalpostResponse(JOURNALPOST_ID_2)

            val resultingJournalpost = dokArkivService.handleJournalpostBasedOnKabalKlagebehandling(
                journalpostId = JOURNALPOST_ID,
                klagebehandlingId = UUID.randomUUID(),
                avsender = null,
            )

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
                )
            }

            assertEquals(JOURNALPOST_ID_2, resultingJournalpost)
        }


        @Test
        fun `journalfoert incoming journalpost - Avsender in request, incorrect fagsak - Handled correctly`() {
            every { kabalApiService.getCompletedBehandling(any()) } returns getCompletedKlagebehandling()
            every { kabalApiService.searchPart(any()) } returns PERSON
            every { safService.getJournalpostAsSaksbehandler(any()) } returns getJournalfoertIncomingJournalpost()
            every {
                dokArkivClient.createNewJournalpostBasedOnExistingJournalpost(
                    any(),
                    any(),
                )
            } returns CreateNewJournalpostBasedOnExistingJournalpostResponse(JOURNALPOST_ID_2)
            every { dokArkivClient.updateAvsenderMottakerInJournalpost(any(), any()) } returns Unit

            val resultingJournalpost = dokArkivService.handleJournalpostBasedOnKabalKlagebehandling(
                journalpostId = JOURNALPOST_ID,
                klagebehandlingId = UUID.randomUUID(),
                avsender = PartIdInput(
                    type = no.nav.klage.api.controller.view.PartType.FNR,
                    id = FNR
                ),
            )

            verify(exactly = 1) {
                dokArkivClient.updateAvsenderMottakerInJournalpost(
                    journalpostId = any(),
                    input = eq(
                        UpdateAvsenderMottakerInJournalpostRequest(
                            avsenderMottaker = no.nav.klage.clients.dokarkiv.AvsenderMottaker(
                                id = FNR,
                                idType = AvsenderMottakerIdType.FNR,
                                navn = "FORNAVN ETTERNAVN",
                            ),
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
                )
            }

            assertEquals(JOURNALPOST_ID_2, resultingJournalpost)
        }
    }


    private fun getCompletedKlagebehandling(): CompletedBehandling {
        return CompletedBehandling(
            behandlingId = UUID.randomUUID(),
            ytelseId = Ytelse.OMS_OLP.id,
            vedtakDate = LocalDateTime.now(),
            sakenGjelder = PART_WITH_UTSENDINGSKANAL,
            klager = PART_WITH_UTSENDINGSKANAL,
            fullmektig = null,
            fagsakId = SAKS_ID,
            fagsystem = FAGSYSTEM,
            fagsystemId = FAGSYSTEM.id,
            klageBehandlendeEnhet = ENHET,
            tildeltSaksbehandlerIdent = "IDENT",
            tildeltSaksbehandlerNavn = "NAVN",
            hjemmelIdList = listOf(Hjemmel.FTRL_15_3.id),
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
            dokumenter = listOf(
                DokumentInfo(
                    dokumentInfoId = "",
                    tittel = null,
                    brevkode = null,
                    skjerming = null,
                    dokumentvarianter = listOf(),
                    logiskeVedlegg = null,
                )
            ),
            relevanteDatoer = listOf(),
            antallRetur = null,
            tilleggsopplysninger = listOf(),
            kanal = "NAV_NO",
            kanalnavn = "",
            utsendingsinfo = null,
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