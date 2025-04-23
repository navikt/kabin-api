package no.nav.klage.service

import io.mockk.mockk
import no.nav.klage.api.controller.view.PartIdInput
import no.nav.klage.api.controller.view.PartType
import no.nav.klage.domain.entities.HandlingEnum
import no.nav.klage.domain.entities.PartId
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.domain.entities.SvarbrevReceiver
import no.nav.klage.kodeverk.PartIdType
import no.nav.klage.kodeverk.TimeUnitType
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.ytelse.Ytelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ModifySvarbrevReiceversTest {

    @Test
    fun `add fullmektig same identifikator as klager`() {
        val registreringService = getRegistreringService()

        val registrering = getRegistrering()
        registrering.sakenGjelder = PartId(type = PartIdType.PERSON, value = "sakenGjelder")
        registrering.klager = PartId(type = PartIdType.PERSON, value = "123")
        registrering.svarbrevReceivers.add(getSvarbrevRecipient("123"))

        registreringService.handleReceiversWhenChangingPart(
            unchangedRegistrering = registrering,
            partIdInput = PartIdInput(
                type = PartType.FNR,
                identifikator = "123",
            ),
            partISaken = RegistreringService.PartISaken.FULLMEKTIG
        )

        assertThat(registrering.svarbrevReceivers).hasSize(1)
        assertThat(registrering.svarbrevReceivers.first().part.value).isEqualTo("123")
    }

    @Test
    fun `set default receiver when needed`() {
        val registrering = getRegistrering()
        registrering.sakenGjelder = PartId(type = PartIdType.PERSON, value = "sakenGjelder")

        assertThat(registrering.svarbrevReceivers).hasSize(0)

        registrering.handleSvarbrevReceivers()

        assertThat(registrering.svarbrevReceivers).hasSize(1)
        assertThat(registrering.svarbrevReceivers.first().part.value).isEqualTo("sakenGjelder")
    }

    @Test
    fun `remove default receiver when needed`() {
        val registreringService = getRegistreringService()
        val registrering = getRegistrering()
        registrering.sakenGjelder = PartId(type = PartIdType.PERSON, value = "sakenGjelder")
        registrering.handleSvarbrevReceivers()

        assertThat(registrering.svarbrevReceivers).hasSize(1)

        registreringService.handleReceiversWhenChangingPart(
            unchangedRegistrering = registrering,
            partIdInput = PartIdInput(
                type = PartType.FNR,
                identifikator = "fullmektig",
            ),
            partISaken = RegistreringService.PartISaken.FULLMEKTIG
        )
        registrering.fullmektig = PartId(type = PartIdType.PERSON, value = "fullmektig")

        assertThat(registrering.svarbrevReceivers).hasSize(0)
    }

    @Test
    fun `remove fullmektig when same as klager`() {
        val registreringService = getRegistreringService()

        val registrering = getRegistrering()
        registrering.sakenGjelder = PartId(type = PartIdType.PERSON, value = "sakenGjelder")
        registrering.klager = PartId(type = PartIdType.PERSON, value = "123")
        registrering.fullmektig = PartId(type = PartIdType.PERSON, value = "123")
        registrering.svarbrevReceivers.add(getSvarbrevRecipient("123"))

        registreringService.handleReceiversWhenChangingPart(
            unchangedRegistrering = registrering,
            partIdInput = null,
            partISaken = RegistreringService.PartISaken.FULLMEKTIG
        )

        assertThat(registrering.svarbrevReceivers).hasSize(1)
    }

    @Test
    fun `remove chosen receiver when removed as fullmektig`() {
        val registreringService = getRegistreringService()

        val registrering = getRegistrering()
        registrering.sakenGjelder = PartId(type = PartIdType.PERSON, value = "sakenGjelder")
        registrering.fullmektig = PartId(type = PartIdType.PERSON, value = "fullmektig")

        registrering.svarbrevReceivers.add(getSvarbrevRecipient("fullmektig"))

        registreringService.handleReceiversWhenChangingPart(
            unchangedRegistrering = registrering,
            partIdInput = null,
            partISaken = RegistreringService.PartISaken.FULLMEKTIG
        )
        registrering.fullmektig = null

        assertThat(registrering.svarbrevReceivers).hasSize(0)
    }

    @Test
    fun `remove chosen receiver when fullmektig changes`() {
        val registreringService = getRegistreringService()

        val registrering = getRegistrering()
        registrering.sakenGjelder = PartId(type = PartIdType.PERSON, value = "sakenGjelder")
        registrering.fullmektig = PartId(type = PartIdType.PERSON, value = "stian")

        registrering.svarbrevReceivers.add(getSvarbrevRecipient("stian"))

        registreringService.handleReceiversWhenChangingPart(
            unchangedRegistrering = registrering,
            partIdInput = PartIdInput(
                type = PartType.FNR,
                identifikator = "berit",
            ),
            partISaken = RegistreringService.PartISaken.FULLMEKTIG
        )
        registrering.fullmektig = PartId(type = PartIdType.PERSON, value = "berit")

        assertThat(registrering.svarbrevReceivers).hasSize(0)
    }

    @Test
    fun `chosen receiver not removed when same as other part`() {
        val registreringService = getRegistreringService()

        val registrering = getRegistrering()
        registrering.sakenGjelder = PartId(type = PartIdType.PERSON, value = "sakenGjelder")
        registrering.klager = PartId(type = PartIdType.PERSON, value = "stian")
        registrering.fullmektig = PartId(type = PartIdType.PERSON, value = "stian")

        registrering.svarbrevReceivers.add(getSvarbrevRecipient("stian"))

        registreringService.handleReceiversWhenChangingPart(
            unchangedRegistrering = registrering,
            partIdInput = null,
            partISaken = RegistreringService.PartISaken.FULLMEKTIG
        )
        registrering.fullmektig = null

        assertThat(registrering.svarbrevReceivers).hasSize(1)
    }

    private fun getRegistreringService(): RegistreringService {
        val registreringService = RegistreringService(
            registreringRepository = mockk(),
            tokenUtil = mockk(),
            kabalApiService = mockk(),
            klageFssProxyService = mockk(),
            klageService = mockk(),
            ankeService = mockk(),
            omgjoeringskravService = mockk(),
            documentService = mockk(),
            dokArkivService = mockk(),
        )
        return registreringService
    }

    private fun getSvarbrevRecipient(value: String): SvarbrevReceiver {
        return SvarbrevReceiver(
            part = PartId(
                type = PartIdType.PERSON,
                value = value,
            ),
            handling = HandlingEnum.AUTO,
            overriddenAddress = null,
        )
    }

    private fun getRegistrering(): Registrering {
        return Registrering(
            sakenGjelder = null,
            klager = null,
            fullmektig = null,
            avsender = null,
            journalpostId = "123456789",
            journalpostDatoOpprettet = LocalDate.now(),
            type = Type.KLAGE,
            mulighetBasedOnJournalpost = false,
            mulighetId = null,
            mottattVedtaksinstans = LocalDate.now(),
            mottattKlageinstans = LocalDate.now(),
            behandlingstidUnits = 12,
            behandlingstidUnitType = TimeUnitType.WEEKS,
            hjemmelIdList = listOf("123", "456"),
            ytelse = Ytelse.OMS_PSB,
            saksbehandlerIdent = "S223456",
            gosysOppgaveId = 923456789,
            sendSvarbrev = true,
            overrideSvarbrevBehandlingstid = true,
            overrideSvarbrevCustomText = true,
            svarbrevTitle = "a title",
            svarbrevCustomText = "custom text",
            svarbrevInitialCustomText = "initial custom text",
            svarbrevBehandlingstidUnits = 5,
            svarbrevBehandlingstidUnitType = TimeUnitType.MONTHS,
            svarbrevFullmektigFritekst = "fullmektig fritekst",
            svarbrevReceivers = mutableSetOf(),
            createdBy = "S123456",
            finished = LocalDateTime.now(),
            behandlingId = UUID.randomUUID(),
            willCreateNewJournalpost = false,
            muligheterFetched = null,
        )
    }
}