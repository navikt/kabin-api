package no.nav.klage.domain.entities

import no.nav.klage.kodeverk.TimeUnitType
import no.nav.klage.kodeverk.Type
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RegistreringAnkeTypeTest {
    @Test
    fun `an anke from Trygderetten is an anke etter 2027`() {
        val registrering = registreringWith(source = RegistreringSource.ANKE)

        assertThat(registrering.getAnkeType()).isEqualTo(Type.ANKE_ETTER_2027)
    }

    @Test
    fun `other sources give an anke foer 2027`() {
        val registrering = registreringWith(source = RegistreringSource.UPLOADED_DOCUMENTS)

        assertThat(registrering.getAnkeType()).isEqualTo(Type.ANKE_FOER_2027)
    }

    private fun registreringWith(source: RegistreringSource): Registrering =
        Registrering(
            sakenGjelder = null,
            klager = null,
            fullmektig = null,
            avsender = null,
            journalpostId = null,
            journalpostDatoOpprettet = null,
            type = null,
            mulighetIsBasedOnJournalpost = false,
            mulighetId = null,
            additionalKabalMulighetId = null,
            mottattVedtaksinstans = null,
            mottattKlageinstans = null,
            behandlingstidUnits = 12,
            behandlingstidUnitType = TimeUnitType.WEEKS,
            hjemmelIdList = listOf(),
            ytelse = null,
            saksbehandlerIdent = null,
            gosysOppgaveId = null,
            sendSvarbrev = null,
            svarbrevTitle = "title",
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
            muligheterFetched = null,
            forrigeBehandlendeEnhetId = null,
            reasonNoLetter = null,
            source = source,
        )
}
