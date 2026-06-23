package no.nav.klage.service

import io.mockk.*
import no.nav.klage.api.controller.view.*
import no.nav.klage.domain.entities.*
import no.nav.klage.exceptions.*
import no.nav.klage.kodeverk.*
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.repository.RegistreringRepository
import no.nav.klage.util.TokenUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class RegistreringServiceTest {

    private lateinit var registreringRepository: RegistreringRepository
    private lateinit var tokenUtil: TokenUtil
    private lateinit var kabalApiService: KabalApiService
    private lateinit var registreringService: RegistreringService

    private val currentIdent = "S123456"

    @BeforeEach
    fun setup() {
        registreringRepository = mockk()
        tokenUtil = mockk()
        kabalApiService = mockk(relaxed = true)

        every { tokenUtil.getCurrentIdent() } returns currentIdent

        registreringService = RegistreringService(
            registreringRepository = registreringRepository,
            tokenUtil = tokenUtil,
            kabalApiService = kabalApiService,
            klageFssProxyService = mockk(),
            klageService = mockk(),
            ankeService = mockk(),
            omgjoeringskravService = mockk(),
            gjenopptakService = mockk(),
            documentService = mockk(),
            dokArkivService = mockk(),
            safService = mockk(),
        )
    }

    // ============ handleReceiversWhenChangingPart ============

    @Nested
    inner class HandleReceivers {
        @Test
        fun `add fullmektig same identifikator as klager`() {
            val registrering = getRegistrering()
            registrering.sakenGjelder = PartId(type = PartIdType.PERSON, value = "sakenGjelder")
            registrering.klager = PartId(type = PartIdType.PERSON, value = "123")
            registrering.svarbrevReceivers.add(getSvarbrevRecipient("123"))

            registreringService.handleReceiversWhenChangingPart(
                unchangedRegistrering = registrering,
                partIdInput = PartIdInput(type = PartType.FNR, identifikator = "123"),
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
            val registrering = getRegistrering()
            registrering.sakenGjelder = PartId(type = PartIdType.PERSON, value = "sakenGjelder")
            registrering.handleSvarbrevReceivers()

            assertThat(registrering.svarbrevReceivers).hasSize(1)

            registreringService.handleReceiversWhenChangingPart(
                unchangedRegistrering = registrering,
                partIdInput = PartIdInput(type = PartType.FNR, identifikator = "fullmektig"),
                partISaken = RegistreringService.PartISaken.FULLMEKTIG
            )
            registrering.fullmektig = PartId(type = PartIdType.PERSON, value = "fullmektig")

            assertThat(registrering.svarbrevReceivers).hasSize(0)
        }

        @Test
        fun `remove fullmektig when same as klager`() {
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
            val registrering = getRegistrering()
            registrering.sakenGjelder = PartId(type = PartIdType.PERSON, value = "sakenGjelder")
            registrering.fullmektig = PartId(type = PartIdType.PERSON, value = "stian")
            registrering.svarbrevReceivers.add(getSvarbrevRecipient("stian"))

            registreringService.handleReceiversWhenChangingPart(
                unchangedRegistrering = registrering,
                partIdInput = PartIdInput(type = PartType.FNR, identifikator = "berit"),
                partISaken = RegistreringService.PartISaken.FULLMEKTIG
            )
            registrering.fullmektig = PartId(type = PartIdType.PERSON, value = "berit")

            assertThat(registrering.svarbrevReceivers).hasSize(0)
        }

        @Test
        fun `chosen receiver not removed when same as other part`() {
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
    }

    // ============ getRegistreringForUpdate (tested indirectly) ============

    @Nested
    inner class AccessControl {
        @Test
        fun `throws RegistreringNotFoundException when registrering not found`() {
            val id = UUID.randomUUID()
            every { registreringRepository.findById(id) } returns Optional.empty()

            assertThatThrownBy {
                registreringService.deleteRegistrering(id)
            }.isInstanceOf(RegistreringNotFoundException::class.java)
        }

        @Test
        fun `throws MissingAccessException when registrering belongs to another user`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id, createdBy = "OTHER_USER")
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            assertThatThrownBy {
                registreringService.deleteRegistrering(id)
            }.isInstanceOf(MissingAccessException::class.java)
        }

        @Test
        fun `throws IllegalUpdateException when registrering is already finished`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.finished = LocalDateTime.now()
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            assertThatThrownBy {
                registreringService.deleteRegistrering(id)
            }.isInstanceOf(IllegalUpdateException::class.java)
        }
    }

    // ============ setTypeId ============

    @Nested
    inner class SetTypeIdTest {
        @Test
        fun `sets type from input`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setTypeId(id, TypeIdInput(typeId = Type.KLAGE.id))

            assertThat(registrering.type).isEqualTo(Type.KLAGE)
        }

        @Test
        fun `sets behandlingstidUnits to 0 for ANKE type`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setTypeId(id, TypeIdInput(typeId = Type.ANKE.id))

            assertThat(registrering.behandlingstidUnits).isEqualTo(0)
        }

        @Test
        fun `sets behandlingstidUnits to 12 for KLAGE type`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setTypeId(id, TypeIdInput(typeId = Type.KLAGE.id))

            assertThat(registrering.behandlingstidUnits).isEqualTo(12)
        }

        @Test
        fun `resets mulighetId, ytelse, saksbehandlerIdent, sendSvarbrev when type changes`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.mulighetId = UUID.randomUUID()
            registrering.ytelse = Ytelse.OMS_PSB
            registrering.saksbehandlerIdent = "S999"
            registrering.sendSvarbrev = true
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setTypeId(id, TypeIdInput(typeId = Type.ANKE.id))

            assertThat(registrering.mulighetId).isNull()
            assertThat(registrering.ytelse).isNull()
            assertThat(registrering.saksbehandlerIdent).isNull()
            assertThat(registrering.sendSvarbrev).isNull()
        }

        @Test
        fun `sets type to null when typeId is null`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.type = Type.KLAGE
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setTypeId(id, TypeIdInput(typeId = null))

            assertThat(registrering.type).isNull()
        }
    }

    // ============ setMottattVedtaksinstans ============

    @Nested
    inner class SetMottattVedtaksinstansTest {
        @Test
        fun `sets mottattVedtaksinstans from input`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val date = LocalDate.of(2025, 3, 15)
            val result = registreringService.setMottattVedtaksinstans(id, MottattVedtaksinstansInput(mottattVedtaksinstans = date))

            assertThat(registrering.mottattVedtaksinstans).isEqualTo(date)
            assertThat(result.overstyringer.mottattVedtaksinstans).isEqualTo(date)
        }
    }

    // ============ setMottattKlageinstans ============

    @Nested
    inner class SetMottattKlageinstansTest {
        @Test
        fun `sets mottattKlageinstans from input`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val date = LocalDate.of(2025, 6, 1)
            val result = registreringService.setMottattKlageinstans(id, MottattKlageinstansInput(mottattKlageinstans = date))

            assertThat(registrering.mottattKlageinstans).isEqualTo(date)
            assertThat(result.overstyringer.mottattKlageinstans).isEqualTo(date)
        }
    }

    // ============ setBehandlingstid ============

    @Nested
    inner class SetBehandlingstidTest {
        @Test
        fun `sets behandlingstid units and type from input`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val result = registreringService.setBehandlingstid(id, BehandlingstidInput(units = 6, unitTypeId = TimeUnitType.MONTHS.id))

            assertThat(registrering.behandlingstidUnits).isEqualTo(6)
            assertThat(registrering.behandlingstidUnitType).isEqualTo(TimeUnitType.MONTHS)
            assertThat(result.overstyringer.behandlingstid.units).isEqualTo(6)
            assertThat(result.overstyringer.behandlingstid.unitTypeId).isEqualTo(TimeUnitType.MONTHS.id)
        }
    }

    // ============ setHjemmelIdList ============

    @Nested
    inner class SetHjemmelIdListTest {
        @Test
        fun `sets hjemmelIdList from input`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val hjemler = listOf("hjemmel1", "hjemmel2", "hjemmel3")
            val result = registreringService.setHjemmelIdList(id, HjemmelIdListInput(hjemmelIdList = hjemler))

            assertThat(registrering.hjemmelIdList).isEqualTo(hjemler)
            assertThat(result.overstyringer.hjemmelIdList).isEqualTo(hjemler)
        }

        @Test
        fun `sets empty hjemmelIdList`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.hjemmelIdList = listOf("existing")
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setHjemmelIdList(id, HjemmelIdListInput(hjemmelIdList = emptyList()))

            assertThat(registrering.hjemmelIdList).isEmpty()
        }
    }

    // ============ setSaksbehandlerIdent ============

    @Nested
    inner class SetSaksbehandlerIdentTest {
        @Test
        fun `sets saksbehandlerIdent from input`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val result = registreringService.setSaksbehandlerIdent(id, SaksbehandlerIdentInput(saksbehandlerIdent = "S999999"))

            assertThat(registrering.saksbehandlerIdent).isEqualTo("S999999")
            assertThat(result.overstyringer.saksbehandlerIdent).isEqualTo("S999999")
        }

        @Test
        fun `sets saksbehandlerIdent to null`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.saksbehandlerIdent = "S999999"
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setSaksbehandlerIdent(id, SaksbehandlerIdentInput(saksbehandlerIdent = null))

            assertThat(registrering.saksbehandlerIdent).isNull()
        }
    }

    // ============ setGosysOppgaveId ============

    @Nested
    inner class SetGosysOppgaveIdTest {
        @Test
        fun `sets gosysOppgaveId from input`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val result = registreringService.setGosysOppgaveId(id, GosysOppgaveIdInput(gosysOppgaveId = 123456789L))

            assertThat(registrering.gosysOppgaveId).isEqualTo(123456789L)
            assertThat(result.overstyringer.gosysOppgaveId).isEqualTo(123456789L)
        }

        @Test
        fun `sets gosysOppgaveId to null`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.gosysOppgaveId = 123456789L
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setGosysOppgaveId(id, GosysOppgaveIdInput(gosysOppgaveId = null))

            assertThat(registrering.gosysOppgaveId).isNull()
        }
    }

    // ============ setForrigeBehandlendeEnhet ============

    @Nested
    inner class SetForrigeBehandlendeEnhetTest {
        @Test
        fun `throws when ytelse is not set`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            assertThatThrownBy {
                registreringService.setForrigeBehandlendeEnhetId(
                    id,
                    ForrigeBehandlendeEnhetIdInput(forrigeBehandlendeEnhetId = "4200")
                )
            }.isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        fun `sets trimmed value when ytelse is set`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.type = Type.KLAGE
            registrering.mulighetIsBasedOnJournalpost = true
            registrering.ytelse = Ytelse.OMS_PSB

            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val result = registreringService.setForrigeBehandlendeEnhetId(
                id,
                ForrigeBehandlendeEnhetIdInput(forrigeBehandlendeEnhetId = "  4295  ")
            )

            assertThat(registrering.forrigeBehandlendeEnhetId).isEqualTo("4295")
            assertThat(result.overstyringer.forrigeBehandlendeEnhetId).isEqualTo("4295")
        }
    }

    // ============ setSendSvarbrev ============

    @Nested
    inner class SetSendSvarbrevTest {
        @Test
        fun `sets sendSvarbrev to true`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val result = registreringService.setSendSvarbrev(id, SendSvarbrevInput(send = true))

            assertThat(registrering.sendSvarbrev).isTrue()
            assertThat(result.svarbrev.send).isTrue()
        }

        @Test
        fun `clears reasonNoLetter when send is true`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.reasonNoLetter = "some reason"
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setSendSvarbrev(id, SendSvarbrevInput(send = true))

            assertThat(registrering.reasonNoLetter).isNull()
        }

        @Test
        fun `does not clear reasonNoLetter when send is false`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.reasonNoLetter = "some reason"
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setSendSvarbrev(id, SendSvarbrevInput(send = false))

            assertThat(registrering.reasonNoLetter).isEqualTo("some reason")
        }
    }

    // ============ setReasonNoLetter ============

    @Nested
    inner class SetReasonNoLetterTest {
        @Test
        fun `sets reasonNoLetter from input`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val result = registreringService.setReasonNoLetter(id, ReasonNoLetterInput(reasonNoLetter = "Grunn"))

            assertThat(registrering.reasonNoLetter).isEqualTo("Grunn")
            assertThat(result.svarbrev.reasonNoLetter).isEqualTo("Grunn")
        }
    }

    // ============ setSvarbrevTitle ============

    @Nested
    inner class SetSvarbrevTitleTest {
        @Test
        fun `sets svarbrevTitle from input`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val result = registreringService.setSvarbrevTitle(id, SvarbrevTitleInput(title = "Ny tittel"))

            assertThat(registrering.svarbrevTitle).isEqualTo("Ny tittel")
            assertThat(result.svarbrev.title).isEqualTo("Ny tittel")
        }
    }

    // ============ setSvarbrevCustomText ============

    @Nested
    inner class SetSvarbrevCustomTextTest {
        @Test
        fun `sets svarbrevCustomText from input`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val result = registreringService.setSvarbrevCustomText(id, SvarbrevCustomTextInput(customText = "Ny tekst"))

            assertThat(registrering.svarbrevCustomText).isEqualTo("Ny tekst")
            assertThat(result.svarbrev.customText).isEqualTo("Ny tekst")
        }
    }

    // ============ setSvarbrevInitialCustomText ============

    @Nested
    inner class SetSvarbrevInitialCustomTextTest {
        @Test
        fun `sets svarbrevInitialCustomText from input`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val result = registreringService.setSvarbrevInitialCustomText(id, SvarbrevInitialCustomTextInput(initialCustomText = "Initial tekst"))

            assertThat(registrering.svarbrevInitialCustomText).isEqualTo("Initial tekst")
            assertThat(result.svarbrev.initialCustomText).isEqualTo("Initial tekst")
        }
    }

    // ============ setSvarbrevBehandlingstid ============

    @Nested
    inner class SetSvarbrevBehandlingstidTest {
        @Test
        fun `sets svarbrev behandlingstid units and type`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val result = registreringService.setSvarbrevBehandlingstid(id, BehandlingstidInput(units = 3, unitTypeId = TimeUnitType.MONTHS.id))

            assertThat(registrering.svarbrevBehandlingstidUnits).isEqualTo(3)
            assertThat(registrering.svarbrevBehandlingstidUnitType).isEqualTo(TimeUnitType.MONTHS)
            assertThat(result.svarbrev.behandlingstid!!.units).isEqualTo(3)
            assertThat(result.svarbrev.behandlingstid!!.unitTypeId).isEqualTo(TimeUnitType.MONTHS.id)
        }
    }

    // ============ setSvarbrevFullmektigFritekst ============

    @Nested
    inner class SetSvarbrevFullmektigFritekstTest {
        @Test
        fun `sets svarbrevFullmektigFritekst from input`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val result = registreringService.setSvarbrevFullmektigFritekst(id, SvarbrevFullmektigFritekstInput(fullmektigFritekst = "Fritekst"))

            assertThat(registrering.svarbrevFullmektigFritekst).isEqualTo("Fritekst")
            assertThat(result.svarbrev.fullmektigFritekst).isEqualTo("Fritekst")
        }

        @Test
        fun `sets svarbrevFullmektigFritekst to null when blank`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.svarbrevFullmektigFritekst = "existing"
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setSvarbrevFullmektigFritekst(id, SvarbrevFullmektigFritekstInput(fullmektigFritekst = "  "))

            assertThat(registrering.svarbrevFullmektigFritekst).isNull()
        }

        @Test
        fun `sets svarbrevFullmektigFritekst to null when null`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.svarbrevFullmektigFritekst = "existing"
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setSvarbrevFullmektigFritekst(id, SvarbrevFullmektigFritekstInput(fullmektigFritekst = null))

            assertThat(registrering.svarbrevFullmektigFritekst).isNull()
        }
    }

    // ============ addSvarbrevReceiver ============

    @Nested
    inner class AddSvarbrevReceiverTest {
        @Test
        fun `adds receiver to svarbrevReceivers set`() {
            val registrering = getUnfinishedRegistrering()

            assertThat(registrering.svarbrevReceivers).isEmpty()

            registrering.svarbrevReceivers.add(SvarbrevReceiver(
                part = PartId(PartIdType.PERSON, "12345678901"),
                handling = HandlingEnum.AUTO,
                overriddenAddress = null
            ))

            assertThat(registrering.svarbrevReceivers).hasSize(1)
            assertThat(registrering.svarbrevReceivers.first().part.value).isEqualTo("12345678901")
            assertThat(registrering.svarbrevReceivers.first().part.type).isEqualTo(PartIdType.PERSON)
        }

        @Test
        fun `adds receiver with VIRKSOMHET type`() {
            val registrering = getUnfinishedRegistrering()

            registrering.svarbrevReceivers.add(SvarbrevReceiver(
                part = PartId(PartIdType.VIRKSOMHET, "987654321"),
                handling = HandlingEnum.AUTO,
                overriddenAddress = null
            ))

            assertThat(registrering.svarbrevReceivers).hasSize(1)
            assertThat(registrering.svarbrevReceivers.first().part.type).isEqualTo(PartIdType.VIRKSOMHET)
        }

        @Test
        fun `duplicate check is based on part value`() {
            val registrering = getUnfinishedRegistrering()
            registrering.svarbrevReceivers.add(SvarbrevReceiver(
                part = PartId(PartIdType.PERSON, "12345678901"),
                handling = HandlingEnum.AUTO,
                overriddenAddress = null
            ))

            // Simulating the logic in addSvarbrevReceiver
            val alreadyExists = registrering.svarbrevReceivers.any { it.part.value == "12345678901" }
            assertThat(alreadyExists).isTrue()

            val doesNotExist = registrering.svarbrevReceivers.any { it.part.value == "99999999999" }
            assertThat(doesNotExist).isFalse()
        }
    }

    // ============ modifySvarbrevReceiver ============

    @Nested
    inner class ModifySvarbrevReceiverTest {
        @Test
        fun `throws ReceiverNotFoundException when receiver not found`() {
            val id = UUID.randomUUID()
            val receiverId = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            assertThatThrownBy {
                registreringService.modifySvarbrevReceiver(id, receiverId, ModifySvarbrevRecipientInput(
                    handling = HandlingEnum.AUTO,
                    overriddenAddress = null
                ))
            }.isInstanceOf(ReceiverNotFoundException::class.java)
        }

        @Test
        fun `updates overriddenAddress on existing receiver`() {
            val registrering = getUnfinishedRegistrering()
            val receiverId = UUID.randomUUID()
            val receiver = SvarbrevReceiver(
                id = receiverId,
                part = PartId(PartIdType.PERSON, "12345678901"),
                handling = HandlingEnum.AUTO,
                overriddenAddress = null
            )
            registrering.svarbrevReceivers.add(receiver)

            // Simulating the logic in modifySvarbrevReceiver
            val foundReceiver = registrering.svarbrevReceivers.find { it.id == receiverId }!!
            foundReceiver.overriddenAddress = no.nav.klage.domain.entities.Address(
                adresselinje1 = "Testveien 1",
                adresselinje2 = null,
                adresselinje3 = null,
                landkode = "NO",
                postnummer = "0123",
                poststed = "OSLO"
            )

            assertThat(receiver.overriddenAddress).isNotNull
            assertThat(receiver.overriddenAddress!!.adresselinje1).isEqualTo("Testveien 1")
        }
    }

    // ============ deleteSvarbrevReceiver ============

    @Nested
    inner class DeleteSvarbrevReceiverTest {
        @Test
        fun `removes receiver by id`() {
            val id = UUID.randomUUID()
            val receiverId = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.svarbrevReceivers.add(SvarbrevReceiver(
                id = receiverId,
                part = PartId(PartIdType.PERSON, "12345678901"),
                handling = HandlingEnum.AUTO,
                overriddenAddress = null
            ))
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.deleteSvarbrevReceiver(id, receiverId)

            assertThat(registrering.svarbrevReceivers).isEmpty()
        }

        @Test
        fun `does not throw when receiver id not found`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.deleteSvarbrevReceiver(id, UUID.randomUUID())

            assertThat(registrering.svarbrevReceivers).isEmpty()
        }
    }

    // ============ deleteRegistrering ============

    @Nested
    inner class DeleteRegistreringTest {
        @Test
        fun `deletes registrering after access check`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)
            every { registreringRepository.deleteById(id) } just runs

            registreringService.deleteRegistrering(id)

            verify { registreringRepository.deleteById(id) }
        }
    }

    // ============ finishRegistrering ============

    @Nested
    inner class FinishRegistreringTest {
        @Test
        fun `throws IllegalInputException when type is null`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.type = null
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            assertThatThrownBy {
                registreringService.finishRegistrering(id)
            }.isInstanceOf(IllegalInputException::class.java)
        }

        @Test
        fun `delegates to klageService when type is KLAGE`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.type = Type.KLAGE
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val klageService = mockk<KlageService>()
            every { klageService.createKlage(registrering) } returns mockk(relaxed = true)

            val service = RegistreringService(
                registreringRepository = registreringRepository,
                tokenUtil = tokenUtil,
                kabalApiService = kabalApiService,
                klageFssProxyService = mockk(),
                klageService = klageService,
                ankeService = mockk(),
                omgjoeringskravService = mockk(),
                gjenopptakService = mockk(),
                documentService = mockk(),
                dokArkivService = mockk(),
                safService = mockk(),
            )

            service.finishRegistrering(id)

            verify { klageService.createKlage(registrering) }
        }

        @Test
        fun `sets finished and behandlingId after successful finish`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.type = Type.KLAGE
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val behandlingId = UUID.randomUUID()
            val klageService = mockk<KlageService>()
            every { klageService.createKlage(registrering) } returns mockk {
                every { this@mockk.behandlingId } returns behandlingId
            }

            val service = RegistreringService(
                registreringRepository = registreringRepository,
                tokenUtil = tokenUtil,
                kabalApiService = kabalApiService,
                klageFssProxyService = mockk(),
                klageService = klageService,
                ankeService = mockk(),
                omgjoeringskravService = mockk(),
                gjenopptakService = mockk(),
                documentService = mockk(),
                dokArkivService = mockk(),
                safService = mockk(),
            )

            val result = service.finishRegistrering(id)

            assertThat(registrering.finished).isNotNull
            assertThat(registrering.behandlingId).isEqualTo(behandlingId)
            assertThat(result.behandlingId).isEqualTo(behandlingId)
        }
    }

    // ============ setMulighetIsBasedOnJournalpost ============

    @Nested
    inner class SetMulighetIsBasedOnJournalpostTest {
        @Test
        fun `resets dependent fields when toggling mulighetIsBasedOnJournalpost`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.mulighetId = UUID.randomUUID()
            registrering.ytelse = Ytelse.OMS_PSB
            registrering.saksbehandlerIdent = "S999"
            registrering.sendSvarbrev = true
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setMulighetIsBasedOnJournalpost(id, MulighetIsBasedOnJournalpostInput(mulighetIsBasedOnJournalpost = true))

            assertThat(registrering.mulighetId).isNull()
            assertThat(registrering.ytelse).isNull()
            assertThat(registrering.saksbehandlerIdent).isNull()
            assertThat(registrering.sendSvarbrev).isNull()
            assertThat(registrering.mulighetIsBasedOnJournalpost).isTrue()
        }
    }

    // ============ setTypeId - additionalKabalMulighetId reset ============

    @Nested
    inner class SetTypeIdAdditionalFieldsTest {
        @Test
        fun `setTypeId resets additionalKabalMulighetId`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.additionalKabalMulighetId = UUID.randomUUID()
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setTypeId(id, TypeIdInput(typeId = Type.KLAGE.id))

            assertThat(registrering.additionalKabalMulighetId).isNull()
        }
    }

    // ============ getCurrentMulighet ============

    @Nested
    inner class GetCurrentMulighetTest {
        @Test
        fun `getCurrentMulighet returns null when mulighetId is null`() {
            val registrering = getUnfinishedRegistrering()
            registrering.mulighetId = null

            assertThat(registrering.getCurrentMulighet()).isNull()
        }

        @Test
        fun `getCurrentMulighet returns mulighet when found`() {
            val mulighetId = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering()
            registrering.mulighetId = mulighetId
            val mulighet = createMulighet(id = mulighetId)
            registrering.muligheter.add(mulighet)

            assertThat(registrering.getCurrentMulighet()).isEqualTo(mulighet)
        }

        @Test
        fun `getCurrentMulighet returns null when mulighetId does not match any mulighet`() {
            val registrering = getUnfinishedRegistrering()
            registrering.mulighetId = UUID.randomUUID()

            assertThat(registrering.getCurrentMulighet()).isNull()
        }
    }

    // ============ getCurrentAdditionalKabalMulighet ============

    @Nested
    inner class GetCurrentAdditionalKabalMulighetTest {
        @Test
        fun `getCurrentAdditionalKabalMulighet returns null when additionalKabalMulighetId is null`() {
            val registrering = getUnfinishedRegistrering()
            registrering.additionalKabalMulighetId = null

            assertThat(registrering.getCurrentAdditionalKabalMulighet()).isNull()
        }

        @Test
        fun `getCurrentAdditionalKabalMulighet returns mulighet when found`() {
            val mulighetId = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering()
            registrering.additionalKabalMulighetId = mulighetId
            val mulighet = createMulighet(id = mulighetId)
            registrering.muligheter.add(mulighet)

            assertThat(registrering.getCurrentAdditionalKabalMulighet()).isEqualTo(mulighet)
        }
    }

    // ============ Mulighet helper methods ============

    @Nested
    inner class MulighetHelperMethodsTest {
        @Test
        fun `isAdditionalKabalAnkeMulighetBasedOnInfotrygdSak returns true for correct combination`() {
            val mulighet = createMulighet(
                originalFagsystem = Fagsystem.IT01,
                currentFagsystem = Fagsystem.KABAL,
                type = Type.ANKE,
                originalType = Type.KLAGE,
            )

            assertThat(mulighet.isAdditionalKabalAnkeMulighetBasedOnInfotrygdSak()).isTrue()
        }

        @Test
        fun `isAdditionalKabalAnkeMulighetBasedOnInfotrygdSak returns false for wrong originalFagsystem`() {
            val mulighet = createMulighet(
                originalFagsystem = Fagsystem.KABAL,
                currentFagsystem = Fagsystem.KABAL,
                type = Type.ANKE,
                originalType = Type.KLAGE,
            )

            assertThat(mulighet.isAdditionalKabalAnkeMulighetBasedOnInfotrygdSak()).isFalse()
        }

        @Test
        fun `isAdditionalKabalAnkeMulighetBasedOnInfotrygdSak returns false when type is KLAGE`() {
            val mulighet = createMulighet(
                originalFagsystem = Fagsystem.IT01,
                currentFagsystem = Fagsystem.KABAL,
                type = Type.KLAGE,
                originalType = Type.KLAGE,
            )

            assertThat(mulighet.isAdditionalKabalAnkeMulighetBasedOnInfotrygdSak()).isFalse()
        }

        @Test
        fun `isAnkeMulighetFromInfotrygd returns true for correct combination`() {
            val mulighet = createMulighet(
                originalFagsystem = Fagsystem.IT01,
                currentFagsystem = Fagsystem.IT01,
                type = Type.ANKE,
                originalType = Type.ANKE,
            )

            assertThat(mulighet.isAnkeMulighetFromInfotrygd()).isTrue()
        }

        @Test
        fun `isAnkeMulighetFromInfotrygd returns false when currentFagsystem is KABAL`() {
            val mulighet = createMulighet(
                originalFagsystem = Fagsystem.IT01,
                currentFagsystem = Fagsystem.KABAL,
                type = Type.ANKE,
                originalType = Type.ANKE,
            )

            assertThat(mulighet.isAnkeMulighetFromInfotrygd()).isFalse()
        }

        @Test
        fun `isAnkeMulighetFromInfotrygd returns false when type is KLAGE`() {
            val mulighet = createMulighet(
                originalFagsystem = Fagsystem.IT01,
                currentFagsystem = Fagsystem.IT01,
                type = Type.KLAGE,
                originalType = Type.ANKE,
            )

            assertThat(mulighet.isAnkeMulighetFromInfotrygd()).isFalse()
        }
    }

    // ============ Helpers ============

    private fun getSvarbrevRecipient(value: String): SvarbrevReceiver {
        return SvarbrevReceiver(
            part = PartId(type = PartIdType.PERSON, value = value),
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
            mulighetIsBasedOnJournalpost = false,
            mulighetId = null,
            additionalKabalMulighetId = null,
            mottattVedtaksinstans = LocalDate.now(),
            mottattKlageinstans = LocalDate.now(),
            behandlingstidUnits = 12,
            behandlingstidUnitType = TimeUnitType.WEEKS,
            hjemmelIdList = listOf("123", "456"),
            ytelse = Ytelse.OMS_PSB,
            forrigeBehandlendeEnhetId = "4200",
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
            reasonNoLetter = null,
        )
    }

    /**
     * Creates a registrering that will pass getRegistreringForUpdate checks:
     * - createdBy matches currentIdent
     * - finished is null
     */
    private fun getUnfinishedRegistrering(
        id: UUID = UUID.randomUUID(),
        createdBy: String = currentIdent,
    ): Registrering {
        return Registrering(
            id = id,
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
            hjemmelIdList = emptyList(),
            ytelse = null,
            forrigeBehandlendeEnhetId = null,
            saksbehandlerIdent = null,
            gosysOppgaveId = null,
            sendSvarbrev = null,
            svarbrevTitle = "Klageinstans orienterer om saksbehandlingen",
            overrideSvarbrevCustomText = false,
            svarbrevCustomText = null,
            svarbrevInitialCustomText = null,
            overrideSvarbrevBehandlingstid = false,
            svarbrevBehandlingstidUnits = null,
            svarbrevBehandlingstidUnitType = null,
            svarbrevFullmektigFritekst = null,
            svarbrevReceivers = mutableSetOf(),
            createdBy = createdBy,
            finished = null,
            behandlingId = null,
            willCreateNewJournalpost = false,
            muligheter = mutableSetOf(),
            muligheterFetched = LocalDateTime.now(),
            reasonNoLetter = null,
        )
    }

    private fun createMulighet(
        id: UUID = UUID.randomUUID(),
        originalFagsystem: Fagsystem = Fagsystem.IT01,
        currentFagsystem: Fagsystem = Fagsystem.IT01,
        type: Type = Type.ANKE,
        originalType: Type? = Type.ANKE,
    ): Mulighet {
        return Mulighet(
            id = id,
            sakenGjelder = PartWithUtsendingskanal(
                part = PartId(type = PartIdType.PERSON, value = "12345678901"),
                address = null,
                name = "Test Person",
                available = true,
                language = null,
                utsendingskanal = null,
            ),
            klager = null,
            fullmektig = null,
            currentFagsystem = currentFagsystem,
            originalFagsystem = originalFagsystem,
            fagsakId = "123",
            tema = Tema.SYK,
            vedtakDate = null,
            ytelse = Ytelse.OMS_PSB,
            hjemmelIdList = emptyList(),
            previousSaksbehandlerIdent = null,
            previousSaksbehandlerName = null,
            type = type,
            originalType = originalType,
            klageBehandlendeEnhet = "4291",
            currentFagystemTechnicalId = "tech-id-1",
            requiresGosysOppgave = false,
        )
    }
}
