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
import no.nav.klage.kodeverk.Ytelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ModifySvarbrevReiceversTest {

//    @Test
//    fun `add fullmektig as only new part`() {
//        val registreringService = RegistreringService(
//            registreringRepository = mockk(),
//            tokenUtil = mockk(),
//            kabalApiClient = mockk(),
//        )
//
//        val registrering = getRegistrering()
//
//        registrering.sakenGjelder = PartId(type = PartIdType.PERSON, value = "sakenGjelder")
//
//        registreringService.handleReceiversWhenAddingPart(
//            unchangedRegistrering = registrering,
//            partIdInput = PartIdInput(
//                type = PartType.FNR,
//                id = "fullmektig",
//            ),
//            partISaken = RegistreringService.PartISaken.FULLMEKTIG
//        )
//
//        assertThat(registrering.svarbrevReceivers).hasSize(1)
//        assertThat(registrering.svarbrevReceivers.first().part.value).isEqualTo("fullmektig")
//    }

    @Test
    fun `add fullmektig same id as klager`() {
        val registreringService = getRegistreringService()

        val registrering = getRegistrering()
        registrering.sakenGjelder = PartId(type = PartIdType.PERSON, value = "sakenGjelder")
        registrering.klager = PartId(type = PartIdType.PERSON, value = "123")
        registrering.svarbrevReceivers.add(getSvarbrevRecipient("123"))

        registreringService.handleReceiversWhenChangingPart(
            unchangedRegistrering = registrering,
            partIdInput = PartIdInput(
                type = PartType.FNR,
                id = "123",
            ),
            partISaken = RegistreringService.PartISaken.FULLMEKTIG
        )

        assertThat(registrering.svarbrevReceivers).hasSize(1)
        assertThat(registrering.svarbrevReceivers.first().part.value).isEqualTo("123")
    }

//    @Test
//    fun `add fullmektig when we already have a klager`() {
//        val registreringService = RegistreringService(
//            registreringRepository = mockk(),
//            tokenUtil = mockk(),
//            kabalApiClient = mockk(),
//        )
//
//        val registrering = getRegistrering()
//        registrering.sakenGjelder = PartId(type = PartIdType.PERSON, value = "sakenGjelder")
//        registrering.klager = PartId(type = PartIdType.PERSON, value = "klager")
//        registrering.svarbrevReceivers.add(getSvarbrevRecipient("klager"))
//
//        registreringService.handleReceiversWhenAddingPart(
//            unchangedRegistrering = registrering,
//            partIdInput = PartIdInput(
//                type = PartType.FNR,
//                id = "fullmektig",
//            ),
//            partISaken = RegistreringService.PartISaken.FULLMEKTIG
//        )
//
//        assertThat(registrering.svarbrevReceivers).hasSize(2)
//        assertThat(registrering.svarbrevReceivers.first().part.value).isEqualTo("klager")
//        assertThat(registrering.svarbrevReceivers.last().part.value).isEqualTo("fullmektig")
//    }

//    @Test
//    fun `remove fullmektig as the only one`() {
//        val registreringService = getRegistreringService()
//
//        val registrering = getRegistrering()
//        registrering.sakenGjelder = PartId(type = PartIdType.PERSON, value = "sakenGjelder")
//        registrering.fullmektig = PartId(type = PartIdType.PERSON, value = "fullmektig")
//        registrering.svarbrevReceivers.add(getSvarbrevRecipient("fullmektig"))
//
//        registreringService.handleReceiversWhenChangingPart(
//            unchangedRegistrering = registrering,
//            partIdInput = null,
//            partISaken = RegistreringService.PartISaken.FULLMEKTIG
//        )
//
//        assertThat(registrering.svarbrevReceivers).isEmpty()
//    }

//    @Test
//    fun `remove klager as the only one`() {
//        val registreringService = getRegistreringService()
//
//        val registrering = getRegistrering()
//        registrering.sakenGjelder = PartId(type = PartIdType.PERSON, value = "sakenGjelder")
//        registrering.klager = PartId(type = PartIdType.PERSON, value = "klager")
//        registrering.svarbrevReceivers.add(getSvarbrevRecipient("klager"))
//
//        registreringService.handleReceiversWhenChangingPart(
//            unchangedRegistrering = registrering,
//            partIdInput = null,
//            partISaken = RegistreringService.PartISaken.KLAGER
//        )
//
//        assertThat(registrering.svarbrevReceivers).isEmpty()
//    }

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
    fun `remove avsender when same as klager`() {
        val registreringService = getRegistreringService()

        val registrering = getRegistrering()
        registrering.sakenGjelder = PartId(type = PartIdType.PERSON, value = "sakenGjelder")
        registrering.klager = PartId(type = PartIdType.PERSON, value = "123")
        registrering.avsender = PartId(type = PartIdType.PERSON, value = "123")
        registrering.svarbrevReceivers.add(getSvarbrevRecipient("123"))

        registreringService.handleReceiversWhenChangingPart(
            unchangedRegistrering = registrering,
            partIdInput = null,
            partISaken = RegistreringService.PartISaken.AVSENDER
        )

        assertThat(registrering.svarbrevReceivers).hasSize(1)
    }

    private fun getRegistreringService(): RegistreringService {
        val registreringService = RegistreringService(
            registreringRepository = mockk(),
            tokenUtil = mockk(),
            kabalApiClient = mockk(),
            klageService = mockk(),
            ankeService = mockk(),
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
            mulighetId = null,
            mottattVedtaksinstans = LocalDate.now(),
            mottattKlageinstans = LocalDate.now(),
            behandlingstidUnits = 12,
            behandlingstidUnitType = TimeUnitType.WEEKS,
            hjemmelIdList = listOf("123", "456"),
            ytelse = Ytelse.OMS_PSB,
            saksbehandlerIdent = "S223456",
            oppgaveId = 923456789,
            sendSvarbrev = true,
            overrideSvarbrevBehandlingstid = true,
            overrideSvarbrevCustomText = true,
            svarbrevTitle = "a title",
            svarbrevCustomText = "custom text",
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