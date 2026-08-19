package no.nav.klage.util

import io.mockk.mockk
import no.nav.klage.clients.kabalapi.KabalApiClient
import no.nav.klage.domain.entities.*
import no.nav.klage.exceptions.SectionedValidationErrorWithDetailsException
import no.nav.klage.kodeverk.PartIdType
import no.nav.klage.kodeverk.TimeUnitType
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.hjemmel.Hjemmel
import no.nav.klage.kodeverk.ytelse.Ytelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ValidationUtilTest {

    private lateinit var kabalApiClient: KabalApiClient
    private lateinit var validationUtil: ValidationUtil

    @BeforeEach
    fun setup() {
        kabalApiClient = mockk(relaxed = true)
        validationUtil = ValidationUtil(kabalApiClient = kabalApiClient)
    }

    @Nested
    inner class AnkeFromTrygderetten {
        @Test
        fun `accepts the values that are given by the source`() {
            val registrering = getAnkeRegistrering()

            assertThat(validationReasonsFor(registrering)).isEmpty()
        }

        @Test
        fun `rejects another type`() {
            val registrering = getAnkeRegistrering().apply { type = Type.OMGJOERINGSKRAV }

            assertThat(validationReasonsFor(registrering))
                .contains("En anke fra Trygderetten må ha type anke.")
        }

        @Test
        fun `rejects another avsender`() {
            val registrering = getAnkeRegistrering().apply {
                avsender = PartId(type = PartIdType.VIRKSOMHET, value = "987654321")
            }

            assertThat(validationReasonsFor(registrering))
                .contains("En anke fra Trygderetten må ha Trygderetten som avsender.")
        }

        @Test
        fun `rejects another inngaaende kanal`() {
            val registrering = getAnkeRegistrering().apply { inngaaendeKanal = InngaaendeKanal.E_POST }

            assertThat(validationReasonsFor(registrering))
                .contains("En anke fra Trygderetten må ha ALTINN_INNBOKS som inngående kanal.")
        }
    }

    /**
     * The registrering is deliberately not complete, so only the errors that this test cares about are
     * asserted on. Returns the reasons that are specific to an anke from Trygderetten.
     */
    private fun validationReasonsFor(registrering: Registrering): List<String> {
        val reasons = try {
            validationUtil.validateRegistrering(registrering = registrering, mulighet = mockk(relaxed = true))
            emptyList()
        } catch (e: SectionedValidationErrorWithDetailsException) {
            e.sections.flatMap { section -> section.properties.map { it.reason } }
        }
        return reasons.filter { it.contains("anke fra Trygderetten") }
    }

    private fun getAnkeRegistrering(): Registrering = Registrering(
        sakenGjelder = PartId(type = PartIdType.PERSON, value = "12345678901"),
        klager = PartId(type = PartIdType.PERSON, value = "12345678901"),
        fullmektig = null,
        avsender = RegistreringSource.TRYGDERETTEN_AVSENDER,
        journalpostId = null,
        journalpostDatoOpprettet = null,
        type = Type.ANKE,
        mulighetIsBasedOnJournalpost = false,
        mulighetId = null,
        additionalKabalMulighetId = null,
        mottattVedtaksinstans = null,
        mottattKlageinstans = LocalDate.now(),
        behandlingstidUnits = 0,
        behandlingstidUnitType = TimeUnitType.WEEKS,
        hjemmelIdList = listOf(Hjemmel.FTRL_8_4.id),
        ytelse = Ytelse.SYK_SYK,
        forrigeBehandlendeEnhetId = "4291",
        saksbehandlerIdent = null,
        gosysOppgaveId = null,
        sendSvarbrev = false,
        svarbrevTitle = "a title",
        overrideSvarbrevCustomText = false,
        svarbrevCustomText = null,
        svarbrevInitialCustomText = null,
        overrideSvarbrevBehandlingstid = false,
        svarbrevBehandlingstidUnits = null,
        svarbrevBehandlingstidUnitType = null,
        svarbrevFullmektigFritekst = null,
        svarbrevReceivers = mutableSetOf(),
        createdBy = "S123456",
        finished = null,
        behandlingId = null,
        willCreateNewJournalpost = false,
        muligheterFetched = LocalDateTime.now(),
        reasonNoLetter = "Ikke nødvendig",
        source = RegistreringSource.ANKE,
        inngaaendeKanal = InngaaendeKanal.ALTINN_INNBOKS,
        dokumenter = mutableSetOf(
            RegistreringDokument(
                mellomlagerId = "mellomlagerId",
                name = "dokument.pdf",
                size = 1L,
                contentType = "application/pdf",
                status = DokumentStatus.DONE,
                sortIndex = 0.0,
            )
        ),
    )
}
