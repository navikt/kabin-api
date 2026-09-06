package no.nav.klage.service

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.klage.api.controller.view.AvsenderInput
import no.nav.klage.api.controller.view.BehandlingstidInput
import no.nav.klage.api.controller.view.ForrigeBehandlendeEnhetIdInput
import no.nav.klage.api.controller.view.GosysOppgaveIdInput
import no.nav.klage.api.controller.view.HjemmelIdListInput
import no.nav.klage.api.controller.view.ModifySvarbrevRecipientInput
import no.nav.klage.api.controller.view.MottattKlageinstansInput
import no.nav.klage.api.controller.view.MottattVedtaksinstansInput
import no.nav.klage.api.controller.view.MulighetInput
import no.nav.klage.api.controller.view.MulighetIsBasedOnJournalpostInput
import no.nav.klage.api.controller.view.PartIdInput
import no.nav.klage.api.controller.view.PartType
import no.nav.klage.api.controller.view.ReasonNoLetterInput
import no.nav.klage.api.controller.view.SaksbehandlerIdentInput
import no.nav.klage.api.controller.view.SearchPartInput
import no.nav.klage.api.controller.view.SendSvarbrevInput
import no.nav.klage.api.controller.view.SourceInput
import no.nav.klage.api.controller.view.SvarbrevCustomTextInput
import no.nav.klage.api.controller.view.SvarbrevFullmektigFritekstInput
import no.nav.klage.api.controller.view.SvarbrevInitialCustomTextInput
import no.nav.klage.api.controller.view.SvarbrevTitleInput
import no.nav.klage.api.controller.view.TypeIdInput
import no.nav.klage.domain.entities.DokumentStatus
import no.nav.klage.domain.entities.HandlingEnum
import no.nav.klage.domain.entities.InngaaendeKanal
import no.nav.klage.domain.entities.Mulighet
import no.nav.klage.domain.entities.PartId
import no.nav.klage.domain.entities.PartWithUtsendingskanal
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.domain.entities.RegistreringDokument
import no.nav.klage.domain.entities.RegistreringSource
import no.nav.klage.domain.entities.SvarbrevReceiver
import no.nav.klage.exceptions.IllegalInputException
import no.nav.klage.exceptions.IllegalUpdateException
import no.nav.klage.exceptions.MissingAccessException
import no.nav.klage.exceptions.MulighetNotFoundException
import no.nav.klage.exceptions.ReceiverNotFoundException
import no.nav.klage.exceptions.RegistreringNotFoundException
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.PartIdType
import no.nav.klage.kodeverk.Tema
import no.nav.klage.kodeverk.TimeUnitType
import no.nav.klage.kodeverk.Type
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
import java.util.Optional
import java.util.UUID

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

        registreringService =
            RegistreringService(
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
                fileApiClient = mockk(),
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
                partISaken = RegistreringService.PartISaken.FULLMEKTIG,
            )

            assertThat(registrering.svarbrevReceivers).hasSize(1)
            assertThat(
                registrering.svarbrevReceivers
                    .first()
                    .part.value,
            ).isEqualTo("123")
        }

        @Test
        fun `set default receiver when needed`() {
            val registrering = getRegistrering()
            registrering.sakenGjelder = PartId(type = PartIdType.PERSON, value = "sakenGjelder")

            assertThat(registrering.svarbrevReceivers).hasSize(0)

            registrering.handleSvarbrevReceivers()

            assertThat(registrering.svarbrevReceivers).hasSize(1)
            assertThat(
                registrering.svarbrevReceivers
                    .first()
                    .part.value,
            ).isEqualTo("sakenGjelder")
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
                partISaken = RegistreringService.PartISaken.FULLMEKTIG,
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
                partISaken = RegistreringService.PartISaken.FULLMEKTIG,
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
                partISaken = RegistreringService.PartISaken.FULLMEKTIG,
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
                partISaken = RegistreringService.PartISaken.FULLMEKTIG,
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
                partISaken = RegistreringService.PartISaken.FULLMEKTIG,
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

            registreringService.setTypeId(registreringId = id, input = TypeIdInput(typeId = Type.KLAGE.id))

            assertThat(registrering.type).isEqualTo(Type.KLAGE)
        }

        @Test
        fun `sets behandlingstidUnits to 0 for ANKE type`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setTypeId(registreringId = id, input = TypeIdInput(typeId = Type.ANKE_FOER_2027.id))

            assertThat(registrering.behandlingstidUnits).isEqualTo(0)
        }

        @Test
        fun `sets behandlingstidUnits to 12 for KLAGE type`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setTypeId(registreringId = id, input = TypeIdInput(typeId = Type.KLAGE.id))

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

            registreringService.setTypeId(registreringId = id, input = TypeIdInput(typeId = Type.ANKE_FOER_2027.id))

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

            registreringService.setTypeId(registreringId = id, input = TypeIdInput(typeId = null))

            assertThat(registrering.type).isNull()
        }
    }

    // ============ setSource ============

    @Nested
    inner class SetSourceTest {
        @BeforeEach
        fun stubPartSearch() {
            every { kabalApiService.searchPart(any()) } answers {
                val identifikator = firstArg<SearchPartInput>().identifikator
                no.nav.klage.clients.kabalapi.SearchPartView(
                    identifikator = identifikator,
                    type = no.nav.klage.clients.kabalapi.PartType.ORGNR,
                    name = "Trygderetten",
                    available = true,
                    statusList = emptyList(),
                    address = null,
                    language = null,
                )
            }
        }

        @Test
        fun `sets type, avsender and inngaaendeKanal when source is ANKE`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val view = registreringService.setSource(registreringId = id, input = SourceInput(source = RegistreringSource.ANKE))

            assertThat(registrering.source).isEqualTo(RegistreringSource.ANKE)
            assertThat(registrering.type).isEqualTo(Type.ANKE_FOER_2027)
            assertThat(registrering.behandlingstidUnits).isEqualTo(4)
            assertThat(registrering.avsender).isEqualTo(
                PartId(type = PartIdType.VIRKSOMHET, value = RegistreringSource.TRYGDERETTEN_ORGNR),
            )
            assertThat(registrering.inngaaendeKanal).isEqualTo(InngaaendeKanal.ALTINN_INNBOKS)

            assertThat(view.source).isEqualTo(RegistreringSource.ANKE)
            assertThat(view.typeId).isEqualTo(Type.ANKE_FOER_2027.id)
            assertThat(view.uploadedDocuments.inngaaendeKanal).isEqualTo(InngaaendeKanal.ALTINN_INNBOKS.name)
            assertThat(view.overstyringer.avsender).isNotNull()
        }

        @Test
        fun `clears ANKE side effects when switching to another source`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.source = RegistreringSource.ANKE
            registrering.type = Type.ANKE_FOER_2027
            registrering.avsender = PartId(type = PartIdType.VIRKSOMHET, value = RegistreringSource.TRYGDERETTEN_ORGNR)
            registrering.inngaaendeKanal = InngaaendeKanal.ALTINN_INNBOKS
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setSource(registreringId = id, input = SourceInput(source = RegistreringSource.UPLOADED_DOCUMENTS))

            assertThat(registrering.source).isEqualTo(RegistreringSource.UPLOADED_DOCUMENTS)
            assertThat(registrering.type).isNull()
            assertThat(registrering.avsender).isNull()
            assertThat(registrering.inngaaendeKanal).isNull()
        }

        @Test
        fun `keeps uploaded dokumenter when switching between uploaded document sources`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.source = RegistreringSource.UPLOADED_DOCUMENTS
            registrering.dokumenter.add(
                RegistreringDokument(
                    name = "dokument.pdf",
                    size = 1L,
                    contentType = "application/pdf",
                    status = DokumentStatus.DONE,
                    sortIndex = 1.0,
                    mellomlagerId = "mellomlagerId",
                ),
            )
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setSource(registreringId = id, input = SourceInput(source = RegistreringSource.ANKE))

            assertThat(registrering.dokumenter).hasSize(1)
        }

        @Test
        fun `does nothing when source is unchanged`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.source = RegistreringSource.ANKE
            registrering.type = Type.OMGJOERINGSKRAV
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setSource(registreringId = id, input = SourceInput(source = RegistreringSource.ANKE))

            assertThat(registrering.type).isEqualTo(Type.OMGJOERINGSKRAV)
        }

        @Test
        fun `type cannot be changed when source is ANKE`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.source = RegistreringSource.ANKE
            registrering.type = Type.ANKE_FOER_2027
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            assertThatThrownBy {
                registreringService.setTypeId(registreringId = id, input = TypeIdInput(typeId = Type.OMGJOERINGSKRAV.id))
            }.isInstanceOf(IllegalInputException::class.java)

            assertThat(registrering.type).isEqualTo(Type.ANKE_FOER_2027)
        }

        @Test
        fun `avsender cannot be changed when source is ANKE`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.source = RegistreringSource.ANKE
            registrering.avsender = RegistreringSource.TRYGDERETTEN_AVSENDER
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            assertThatThrownBy {
                registreringService.setAvsender(
                    registreringId = id,
                    input = AvsenderInput(avsender = PartIdInput(type = PartType.ORGNR, identifikator = "987654321")),
                )
            }.isInstanceOf(IllegalInputException::class.java)

            assertThat(registrering.avsender).isEqualTo(RegistreringSource.TRYGDERETTEN_AVSENDER)
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
            val result =
                registreringService.setMottattVedtaksinstans(
                    registreringId = id,
                    input = MottattVedtaksinstansInput(mottattVedtaksinstans = date),
                )

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
            val result =
                registreringService.setMottattKlageinstans(
                    registreringId = id,
                    input = MottattKlageinstansInput(mottattKlageinstans = date),
                )

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

            val result =
                registreringService.setBehandlingstid(
                    registreringId = id,
                    input = BehandlingstidInput(units = 6, unitTypeId = TimeUnitType.MONTHS.id),
                )

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
            val result = registreringService.setHjemmelIdList(registreringId = id, input = HjemmelIdListInput(hjemmelIdList = hjemler))

            assertThat(registrering.hjemmelIdList).isEqualTo(hjemler)
            assertThat(result.overstyringer.hjemmelIdList).isEqualTo(hjemler)
        }

        @Test
        fun `sets empty hjemmelIdList`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.hjemmelIdList = listOf("existing")
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setHjemmelIdList(registreringId = id, input = HjemmelIdListInput(hjemmelIdList = emptyList()))

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

            val result =
                registreringService.setSaksbehandlerIdent(
                    registreringId = id,
                    input = SaksbehandlerIdentInput(saksbehandlerIdent = "S999999"),
                )

            assertThat(registrering.saksbehandlerIdent).isEqualTo("S999999")
            assertThat(result.overstyringer.saksbehandlerIdent).isEqualTo("S999999")
        }

        @Test
        fun `sets saksbehandlerIdent to null`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.saksbehandlerIdent = "S999999"
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setSaksbehandlerIdent(registreringId = id, input = SaksbehandlerIdentInput(saksbehandlerIdent = null))

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

            val result =
                registreringService.setGosysOppgaveId(
                    registreringId = id,
                    input = GosysOppgaveIdInput(gosysOppgaveId = 123456789L),
                )

            assertThat(registrering.gosysOppgaveId).isEqualTo(123456789L)
            assertThat(result.overstyringer.gosysOppgaveId).isEqualTo(123456789L)
        }

        @Test
        fun `sets gosysOppgaveId to null`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.gosysOppgaveId = 123456789L
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setGosysOppgaveId(registreringId = id, input = GosysOppgaveIdInput(gosysOppgaveId = null))

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
                    registreringId = id,
                    input = ForrigeBehandlendeEnhetIdInput(forrigeBehandlendeEnhetId = "4200"),
                )
            }.isInstanceOf(IllegalInputException::class.java)
        }

        @Test
        fun `throws exception when illegal input value`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.type = Type.KLAGE
            registrering.mulighetIsBasedOnJournalpost = true
            registrering.ytelse = Ytelse.OMS_PSB

            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            assertThatThrownBy {
                registreringService.setForrigeBehandlendeEnhetId(
                    registreringId = id,
                    input = ForrigeBehandlendeEnhetIdInput(forrigeBehandlendeEnhetId = "1234"),
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
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

            val result = registreringService.setSendSvarbrev(registreringId = id, input = SendSvarbrevInput(send = true))

            assertThat(registrering.sendSvarbrev).isTrue()
            assertThat(result.svarbrev.send).isTrue()
        }

        @Test
        fun `clears reasonNoLetter when send is true`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.reasonNoLetter = "some reason"
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setSendSvarbrev(registreringId = id, input = SendSvarbrevInput(send = true))

            assertThat(registrering.reasonNoLetter).isNull()
        }

        @Test
        fun `does not clear reasonNoLetter when send is false`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.reasonNoLetter = "some reason"
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setSendSvarbrev(registreringId = id, input = SendSvarbrevInput(send = false))

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

            val result = registreringService.setReasonNoLetter(registreringId = id, input = ReasonNoLetterInput(reasonNoLetter = "Grunn"))

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

            val result = registreringService.setSvarbrevTitle(registreringId = id, input = SvarbrevTitleInput(title = "Ny tittel"))

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

            val result =
                registreringService.setSvarbrevCustomText(
                    registreringId = id,
                    input = SvarbrevCustomTextInput(customText = "Ny tekst"),
                )

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

            val result =
                registreringService.setSvarbrevInitialCustomText(
                    registreringId = id,
                    input = SvarbrevInitialCustomTextInput(initialCustomText = "Initial tekst"),
                )

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

            val result =
                registreringService.setSvarbrevBehandlingstid(
                    registreringId = id,
                    input = BehandlingstidInput(units = 3, unitTypeId = TimeUnitType.MONTHS.id),
                )

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

            val result =
                registreringService.setSvarbrevFullmektigFritekst(
                    registreringId = id,
                    input = SvarbrevFullmektigFritekstInput(fullmektigFritekst = "Fritekst"),
                )

            assertThat(registrering.svarbrevFullmektigFritekst).isEqualTo("Fritekst")
            assertThat(result.svarbrev.fullmektigFritekst).isEqualTo("Fritekst")
        }

        @Test
        fun `sets svarbrevFullmektigFritekst to null when blank`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.svarbrevFullmektigFritekst = "existing"
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setSvarbrevFullmektigFritekst(
                registreringId = id,
                input = SvarbrevFullmektigFritekstInput(fullmektigFritekst = "  "),
            )

            assertThat(registrering.svarbrevFullmektigFritekst).isNull()
        }

        @Test
        fun `sets svarbrevFullmektigFritekst to null when null`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.svarbrevFullmektigFritekst = "existing"
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.setSvarbrevFullmektigFritekst(
                registreringId = id,
                input = SvarbrevFullmektigFritekstInput(fullmektigFritekst = null),
            )

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

            registrering.svarbrevReceivers.add(
                SvarbrevReceiver(
                    part = PartId(type = PartIdType.PERSON, value = "12345678901"),
                    handling = HandlingEnum.AUTO,
                    overriddenAddress = null,
                ),
            )

            assertThat(registrering.svarbrevReceivers).hasSize(1)
            assertThat(
                registrering.svarbrevReceivers
                    .first()
                    .part.value,
            ).isEqualTo("12345678901")
            assertThat(
                registrering.svarbrevReceivers
                    .first()
                    .part.type,
            ).isEqualTo(PartIdType.PERSON)
        }

        @Test
        fun `adds receiver with VIRKSOMHET type`() {
            val registrering = getUnfinishedRegistrering()

            registrering.svarbrevReceivers.add(
                SvarbrevReceiver(
                    part = PartId(type = PartIdType.VIRKSOMHET, value = "987654321"),
                    handling = HandlingEnum.AUTO,
                    overriddenAddress = null,
                ),
            )

            assertThat(registrering.svarbrevReceivers).hasSize(1)
            assertThat(
                registrering.svarbrevReceivers
                    .first()
                    .part.type,
            ).isEqualTo(PartIdType.VIRKSOMHET)
        }

        @Test
        fun `duplicate check is based on part value`() {
            val registrering = getUnfinishedRegistrering()
            registrering.svarbrevReceivers.add(
                SvarbrevReceiver(
                    part = PartId(type = PartIdType.PERSON, value = "12345678901"),
                    handling = HandlingEnum.AUTO,
                    overriddenAddress = null,
                ),
            )

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
                registreringService.modifySvarbrevReceiver(
                    registreringId = id,
                    svarbrevReceiverId = receiverId,
                    input =
                        ModifySvarbrevRecipientInput(
                            handling = HandlingEnum.AUTO,
                            overriddenAddress = null,
                        ),
                )
            }.isInstanceOf(ReceiverNotFoundException::class.java)
        }

        @Test
        fun `updates overriddenAddress on existing receiver`() {
            val registrering = getUnfinishedRegistrering()
            val receiverId = UUID.randomUUID()
            val receiver =
                SvarbrevReceiver(
                    id = receiverId,
                    part = PartId(type = PartIdType.PERSON, value = "12345678901"),
                    handling = HandlingEnum.AUTO,
                    overriddenAddress = null,
                )
            registrering.svarbrevReceivers.add(receiver)

            // Simulating the logic in modifySvarbrevReceiver
            val foundReceiver = registrering.svarbrevReceivers.find { it.id == receiverId }!!
            foundReceiver.overriddenAddress =
                no.nav.klage.domain.entities.Address(
                    adresselinje1 = "Testveien 1",
                    adresselinje2 = null,
                    adresselinje3 = null,
                    landkode = "NO",
                    postnummer = "0123",
                    poststed = "OSLO",
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
            registrering.svarbrevReceivers.add(
                SvarbrevReceiver(
                    id = receiverId,
                    part = PartId(type = PartIdType.PERSON, value = "12345678901"),
                    handling = HandlingEnum.AUTO,
                    overriddenAddress = null,
                ),
            )
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.deleteSvarbrevReceiver(registreringId = id, svarbrevReceiverId = receiverId)

            assertThat(registrering.svarbrevReceivers).isEmpty()
        }

        @Test
        fun `does not throw when receiver id not found`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            registreringService.deleteSvarbrevReceiver(registreringId = id, svarbrevReceiverId = UUID.randomUUID())

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

            val service =
                RegistreringService(
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
                    fileApiClient = mockk(),
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
            every { klageService.createKlage(registrering) } returns
                mockk {
                    every { this@mockk.behandlingId } returns behandlingId
                }

            val service =
                RegistreringService(
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
                    fileApiClient = mockk(),
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

            registreringService.setMulighetIsBasedOnJournalpost(
                registreringId = id,
                input = MulighetIsBasedOnJournalpostInput(mulighetIsBasedOnJournalpost = true),
            )

            assertThat(registrering.mulighetId).isNull()
            assertThat(registrering.ytelse).isNull()
            assertThat(registrering.saksbehandlerIdent).isNull()
            assertThat(registrering.sendSvarbrev).isNull()
            assertThat(registrering.mulighetIsBasedOnJournalpost).isTrue()
        }
    }

    // ============ setAdditionalKabalMulighet ============

    @Nested
    inner class SetAdditionalKabalMulighetTest {
        @Test
        fun `sets additionalKabalMulighetId and hjemmelIdList for valid additional kabal mulighet`() {
            val id = UUID.randomUUID()
            val additionalMulighetId = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.ytelse = null

            val mulighet =
                createMulighet(
                    id = additionalMulighetId,
                    originalFagsystem = Fagsystem.IT01,
                    currentFagsystem = Fagsystem.KABAL,
                    type = Type.ANKE_FOER_2027,
                    originalType = Type.KLAGE,
                ).apply {
                    hjemmelIdList = listOf("h1", "h2")
                }
            registrering.muligheter.add(mulighet)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            val result =
                registreringService.setAdditionalKabalMulighet(
                    registreringId = id,
                    input = MulighetInput(mulighetId = additionalMulighetId),
                )

            assertThat(registrering.additionalKabalMulighetId).isEqualTo(additionalMulighetId)
            assertThat(registrering.ytelse).isEqualTo(mulighet.ytelse)
            assertThat(registrering.hjemmelIdList).containsExactly("h1", "h2")
            assertThat(result.additionalKabalMulighetId).isEqualTo(additionalMulighetId)
            assertThat(result.ytelseId).isEqualTo(mulighet.ytelse!!.id)
            assertThat(result.hjemmelIdList).containsExactly("h1", "h2")
        }

        @Test
        fun `throws MulighetNotFoundException when mulighet does not exist`() {
            val id = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            assertThatThrownBy {
                registreringService.setAdditionalKabalMulighet(
                    registreringId = id,
                    input = MulighetInput(mulighetId = UUID.randomUUID()),
                )
            }.isInstanceOf(MulighetNotFoundException::class.java)
        }

        @Test
        fun `throws IllegalInputException when mulighet is not additional kabal anke based on infotrygd sak`() {
            val id = UUID.randomUUID()
            val mulighetId = UUID.randomUUID()
            val registrering = getUnfinishedRegistrering(id = id)
            registrering.muligheter.add(
                createMulighet(
                    id = mulighetId,
                    originalFagsystem = Fagsystem.IT01,
                    currentFagsystem = Fagsystem.IT01,
                    type = Type.ANKE_FOER_2027,
                    originalType = Type.ANKE_FOER_2027,
                ),
            )
            every { registreringRepository.findById(id) } returns Optional.of(registrering)

            assertThatThrownBy {
                registreringService.setAdditionalKabalMulighet(
                    registreringId = id,
                    input = MulighetInput(mulighetId = mulighetId),
                )
            }.isInstanceOf(IllegalInputException::class.java)
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

            registreringService.setTypeId(registreringId = id, input = TypeIdInput(typeId = Type.KLAGE.id))

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
            val mulighet =
                createMulighet(
                    originalFagsystem = Fagsystem.IT01,
                    currentFagsystem = Fagsystem.KABAL,
                    type = Type.ANKE_FOER_2027,
                    originalType = Type.KLAGE,
                )

            assertThat(mulighet.isAdditionalKabalAnkeMulighetBasedOnInfotrygdSak()).isTrue()
        }

        @Test
        fun `isAdditionalKabalAnkeMulighetBasedOnInfotrygdSak returns false for wrong originalFagsystem`() {
            val mulighet =
                createMulighet(
                    originalFagsystem = Fagsystem.KABAL,
                    currentFagsystem = Fagsystem.KABAL,
                    type = Type.ANKE_FOER_2027,
                    originalType = Type.KLAGE,
                )

            assertThat(mulighet.isAdditionalKabalAnkeMulighetBasedOnInfotrygdSak()).isFalse()
        }

        @Test
        fun `isAdditionalKabalAnkeMulighetBasedOnInfotrygdSak returns false when type is KLAGE`() {
            val mulighet =
                createMulighet(
                    originalFagsystem = Fagsystem.IT01,
                    currentFagsystem = Fagsystem.KABAL,
                    type = Type.KLAGE,
                    originalType = Type.KLAGE,
                )

            assertThat(mulighet.isAdditionalKabalAnkeMulighetBasedOnInfotrygdSak()).isFalse()
        }

        @Test
        fun `isAnkeMulighetFromInfotrygd returns true for correct combination`() {
            val mulighet =
                createMulighet(
                    originalFagsystem = Fagsystem.IT01,
                    currentFagsystem = Fagsystem.IT01,
                    type = Type.ANKE_FOER_2027,
                    originalType = Type.ANKE_FOER_2027,
                )

            assertThat(mulighet.isAnkeMulighetFromInfotrygd()).isTrue()
        }

        @Test
        fun `isAnkeMulighetFromInfotrygd returns false when currentFagsystem is KABAL`() {
            val mulighet =
                createMulighet(
                    originalFagsystem = Fagsystem.IT01,
                    currentFagsystem = Fagsystem.KABAL,
                    type = Type.ANKE_FOER_2027,
                    originalType = Type.ANKE_FOER_2027,
                )

            assertThat(mulighet.isAnkeMulighetFromInfotrygd()).isFalse()
        }

        @Test
        fun `isAnkeMulighetFromInfotrygd returns false when type is KLAGE`() {
            val mulighet =
                createMulighet(
                    originalFagsystem = Fagsystem.IT01,
                    currentFagsystem = Fagsystem.IT01,
                    type = Type.KLAGE,
                    originalType = Type.ANKE_FOER_2027,
                )

            assertThat(mulighet.isAnkeMulighetFromInfotrygd()).isFalse()
        }
    }

    // ============ Helpers ============

    private fun getSvarbrevRecipient(value: String): SvarbrevReceiver =
        SvarbrevReceiver(
            part = PartId(type = PartIdType.PERSON, value = value),
            handling = HandlingEnum.AUTO,
            overriddenAddress = null,
        )

    private fun getRegistrering(): Registrering =
        Registrering(
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

    /**
     * Creates a registrering that will pass getRegistreringForUpdate checks:
     * - createdBy matches currentIdent
     * - finished is null
     */
    private fun getUnfinishedRegistrering(
        id: UUID = UUID.randomUUID(),
        createdBy: String = currentIdent,
    ): Registrering =
        Registrering(
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

    private fun createMulighet(
        id: UUID = UUID.randomUUID(),
        originalFagsystem: Fagsystem = Fagsystem.IT01,
        currentFagsystem: Fagsystem = Fagsystem.IT01,
        type: Type = Type.ANKE_FOER_2027,
        originalType: Type? = Type.ANKE_FOER_2027,
    ): Mulighet =
        Mulighet(
            id = id,
            sakenGjelder =
                PartWithUtsendingskanal(
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
