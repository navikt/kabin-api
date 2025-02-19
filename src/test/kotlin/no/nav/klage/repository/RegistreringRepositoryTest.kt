package no.nav.klage.repository

import no.nav.klage.db.TestPostgresqlContainer
import no.nav.klage.domain.entities.*
import no.nav.klage.kodeverk.*
import no.nav.klage.kodeverk.ytelse.Ytelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@ActiveProfiles("local")
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RegistreringRepositoryTest {

    companion object {
        @Container
        @JvmField
        val postgreSQLContainer: TestPostgresqlContainer = TestPostgresqlContainer.instance
    }

    @Autowired
    lateinit var testEntityManager: TestEntityManager

    @Autowired
    lateinit var registreringRepository: RegistreringRepository

    @Test
    fun `store all values in registrering works`() {
        val klagemulighet = Mulighet(
            sakenGjelder = PartWithUtsendingskanal(
                part = PartId(
                    type = PartIdType.PERSON,
                    value = "12345678910"
                ),
                address = Address(
                    adresselinje1 = "addressLine1",
                    adresselinje2 = "addressLine2",
                    adresselinje3 = "addressLine3",
                    postnummer = "1234",
                    poststed = "Oslo",
                    landkode = "NO",
                ),
                name = "Ollie Walters",
                available = true,
                language = "NO",
                utsendingskanal = PartWithUtsendingskanal.Utsendingskanal.NAV_NO
            ),
            klager = PartWithUtsendingskanal(
                part = PartId(
                    type = PartIdType.PERSON,
                    value = "22345678911"
                ),
                address = Address(
                    adresselinje1 = "addressLine1",
                    adresselinje2 = "addressLine2",
                    adresselinje3 = "addressLine3",
                    postnummer = "1234",
                    poststed = "Oslo",
                    landkode = "NO",
                ),
                name = "Ollie Walters Klager",
                available = true,
                language = "NO",
                utsendingskanal = PartWithUtsendingskanal.Utsendingskanal.NAV_NO
            ),
            fullmektig = PartWithUtsendingskanal(
                part = PartId(
                    type = PartIdType.PERSON,
                    value = "32345678912"
                ),
                address = Address(
                    adresselinje1 = "addressLine1",
                    adresselinje2 = "addressLine2",
                    adresselinje3 = "addressLine3",
                    postnummer = "1234",
                    poststed = "Oslo",
                    landkode = "NO",
                ),
                name = "Ollie Walters Fullmektig",
                available = true,
                language = "NO",
                utsendingskanal = PartWithUtsendingskanal.Utsendingskanal.NAV_NO
            ),
            currentFagsystem = Fagsystem.KABAL,
            originalFagsystem = Fagsystem.FS36,
            fagsakId = "ceteros",
            tema = Tema.FOR,
            vedtakDate = null,
            ytelse = Ytelse.FOR_FOR,
            hjemmelIdList = listOf("123", "456"),
            previousSaksbehandlerIdent = "S123456",
            previousSaksbehandlerName = "Sakbehandler Navn",
            type = Type.ANKE,
            originalType = Type.ANKE,
            klageBehandlendeEnhet = "4200",
            currentFagystemTechnicalId = UUID.randomUUID().toString(),
            sourceOfExistingAnkebehandling = mutableSetOf(
                ExistingAnkebehandling(
                    ankebehandlingId = UUID.randomUUID(),
                    created = LocalDateTime.now(),
                    completed = LocalDateTime.now().plusDays(1)
                )
            ),
            sakenGjelderStatusList = mutableSetOf(
                PartStatus(
                    status = PartStatus.Status.FORTROLIG,
                    date = LocalDate.now()
                )
            ),
            klagerStatusList = mutableSetOf(
                PartStatus(
                    status = PartStatus.Status.DELETED,
                    date = LocalDate.now()
                )
            ),
            fullmektigStatusList = mutableSetOf(
                PartStatus(
                    status = PartStatus.Status.DEAD,
                    date = LocalDate.now()
                )
            )

        )

        val registrering = testEntityManager.persistAndFlush(
            Registrering(
                sakenGjelder = PartId(type = PartIdType.PERSON, value = "12345678910"),
                klager = PartId(type = PartIdType.PERSON, value = "22345678910"),
                fullmektig = PartId(type = PartIdType.PERSON, value = "32345678910"),
                avsender = PartId(type = PartIdType.PERSON, value = "42345678910"),
                journalpostId = "123456789",
                journalpostDatoOpprettet = LocalDate.now(),
                type = Type.KLAGE,
                mulighetId = klagemulighet.id,
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
                svarbrevReceivers = mutableSetOf(
                    SvarbrevReceiver(
                        part = PartId(
                            type = PartIdType.PERSON,
                            value = "52345678910"
                        ),
                        handling = HandlingEnum.AUTO,
                        overriddenAddress = Address(
                            adresselinje1 = "addressLine1",
                            adresselinje2 = "addressLine2",
                            adresselinje3 = "addressLine3",
                            postnummer = "1234",
                            poststed = "Oslo",
                            landkode = "NO"
                        )
                    ),
                    SvarbrevReceiver(
                        part = PartId(
                            type = PartIdType.VIRKSOMHET,
                            value = "123456789"
                        ),
                        handling = HandlingEnum.AUTO,
                        overriddenAddress = Address(
                            adresselinje1 = "addr",
                            adresselinje2 = "rsdtdstst",
                            adresselinje3 = "addressdthdthdthsLine3",
                            postnummer = "0123",
                            poststed = "Oslo",
                            landkode = "NO"
                        )
                    )
                ),
                createdBy = "S123456",
                finished = LocalDateTime.now(),
                behandlingId = UUID.randomUUID(),
                willCreateNewJournalpost = false,
                muligheterFetched = LocalDateTime.now(),
                muligheter = mutableSetOf(klagemulighet)
            )
        )

        testEntityManager.clear()

        val registreringFromDb = registreringRepository.findById(registrering.id).get()

        // Check that all values are stored correctly
        assertThat(registreringFromDb.id).isEqualTo(registrering.id)
        assertThat(registreringFromDb.sakenGjelder).isEqualTo(registrering.sakenGjelder)
        assertThat(registreringFromDb.klager).isEqualTo(registrering.klager)
        assertThat(registreringFromDb.fullmektig).isEqualTo(registrering.fullmektig)
        assertThat(registreringFromDb.avsender).isEqualTo(registrering.avsender)
        assertThat(registreringFromDb.journalpostId).isEqualTo(registrering.journalpostId)
        assertThat(registreringFromDb.journalpostDatoOpprettet).isEqualTo(registrering.journalpostDatoOpprettet)
        assertThat(registreringFromDb.type).isEqualTo(registrering.type)
        assertThat(registreringFromDb.mulighetId).isEqualTo(registrering.mulighetId)
        assertThat(registreringFromDb.mottattVedtaksinstans).isEqualTo(registrering.mottattVedtaksinstans)
        assertThat(registreringFromDb.mottattKlageinstans).isEqualTo(registrering.mottattKlageinstans)
        assertThat(registreringFromDb.behandlingstidUnits).isEqualTo(registrering.behandlingstidUnits)
        assertThat(registreringFromDb.behandlingstidUnitType).isEqualTo(registrering.behandlingstidUnitType)
        assertThat(registreringFromDb.hjemmelIdList).isEqualTo(registrering.hjemmelIdList)
        assertThat(registreringFromDb.ytelse).isEqualTo(registrering.ytelse)
        assertThat(registreringFromDb.saksbehandlerIdent).isEqualTo(registrering.saksbehandlerIdent)
        assertThat(registreringFromDb.gosysOppgaveId).isEqualTo(registrering.gosysOppgaveId)
        assertThat(registreringFromDb.sendSvarbrev).isEqualTo(registrering.sendSvarbrev)
        assertThat(registreringFromDb.svarbrevTitle).isEqualTo(registrering.svarbrevTitle)
        assertThat(registreringFromDb.svarbrevCustomText).isEqualTo(registrering.svarbrevCustomText)
        assertThat(registreringFromDb.svarbrevBehandlingstidUnits).isEqualTo(registrering.svarbrevBehandlingstidUnits)
        assertThat(registreringFromDb.svarbrevBehandlingstidUnitType).isEqualTo(registrering.svarbrevBehandlingstidUnitType)
        assertThat(registreringFromDb.svarbrevFullmektigFritekst).isEqualTo(registrering.svarbrevFullmektigFritekst)
        assertThat(registreringFromDb.created.truncatedTo(ChronoUnit.MILLIS)).isEqualTo(
            registrering.created.truncatedTo(
                ChronoUnit.MILLIS
            )
        )
        assertThat(registreringFromDb.modified.truncatedTo(ChronoUnit.MILLIS)).isEqualTo(
            registrering.modified.truncatedTo(
                ChronoUnit.MILLIS
            )
        )
        assertThat(registreringFromDb.createdBy).isEqualTo(registrering.createdBy)

        val firstSvarbrevReceiver =
            registreringFromDb.svarbrevReceivers.find { it.id == registrering.svarbrevReceivers.first().id }!!
        val secondSvarbrevReceiver =
            registreringFromDb.svarbrevReceivers.find { it.id == registrering.svarbrevReceivers.last().id }!!

        assertThat(firstSvarbrevReceiver.id).isEqualTo(registrering.svarbrevReceivers.first().id)
        assertThat(firstSvarbrevReceiver.part).isEqualTo(registrering.svarbrevReceivers.first().part)
        assertThat(firstSvarbrevReceiver.handling).isEqualTo(registrering.svarbrevReceivers.first().handling)
        assertThat(firstSvarbrevReceiver.overriddenAddress!!.adresselinje1).isEqualTo(registrering.svarbrevReceivers.first().overriddenAddress!!.adresselinje1)
        assertThat(firstSvarbrevReceiver.overriddenAddress!!.postnummer).isEqualTo(registrering.svarbrevReceivers.first().overriddenAddress!!.postnummer)
        assertThat(firstSvarbrevReceiver.overriddenAddress!!.landkode).isEqualTo(registrering.svarbrevReceivers.first().overriddenAddress!!.landkode)

        assertThat(secondSvarbrevReceiver.id).isEqualTo(registrering.svarbrevReceivers.last().id)
        assertThat(secondSvarbrevReceiver.part).isEqualTo(registrering.svarbrevReceivers.last().part)
        assertThat(secondSvarbrevReceiver.handling).isEqualTo(registrering.svarbrevReceivers.last().handling)
        assertThat(secondSvarbrevReceiver.overriddenAddress!!.adresselinje1).isEqualTo(registrering.svarbrevReceivers.last().overriddenAddress!!.adresselinje1)
        assertThat(secondSvarbrevReceiver.overriddenAddress!!.postnummer).isEqualTo(registrering.svarbrevReceivers.last().overriddenAddress!!.postnummer)
        assertThat(secondSvarbrevReceiver.overriddenAddress!!.landkode).isEqualTo(registrering.svarbrevReceivers.last().overriddenAddress!!.landkode)

        //assert muligheter
        assertThat(registreringFromDb.muligheterFetched?.truncatedTo(ChronoUnit.MILLIS)!!).isEqualTo(
            registrering.muligheterFetched?.truncatedTo(
                ChronoUnit.MILLIS
            )
        )
        assertThat(registreringFromDb.muligheter).hasSize(1)
        assertThat(registreringFromDb.muligheter.first().id).isEqualTo(klagemulighet.id)
    }

    @Test
    fun `ferdige registreringer works`() {
        testEntityManager.persistAndFlush(
            Registrering(
                sakenGjelder = PartId(type = PartIdType.PERSON, value = "12345678910"),
                klager = PartId(type = PartIdType.PERSON, value = "22345678910"),
                fullmektig = PartId(type = PartIdType.PERSON, value = "32345678910"),
                avsender = PartId(type = PartIdType.PERSON, value = "42345678910"),
                journalpostId = "123456789",
                journalpostDatoOpprettet = LocalDate.now(),
                type = Type.KLAGE,
                mulighetId = UUID.randomUUID(),
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
                svarbrevReceivers = mutableSetOf(
                    SvarbrevReceiver(
                        part = PartId(
                            type = PartIdType.PERSON,
                            value = "52345678910"
                        ),
                        handling = HandlingEnum.AUTO,
                        overriddenAddress = Address(
                            adresselinje1 = "addressLine1",
                            adresselinje2 = "addressLine2",
                            adresselinje3 = "addressLine3",
                            postnummer = "1234",
                            poststed = "Oslo",
                            landkode = "NO"
                        )
                    ),
                    SvarbrevReceiver(
                        part = PartId(
                            type = PartIdType.VIRKSOMHET,
                            value = "123456789"
                        ),
                        handling = HandlingEnum.AUTO,
                        overriddenAddress = Address(
                            adresselinje1 = "addr",
                            adresselinje2 = "rsdtdstst",
                            adresselinje3 = "addressdthdthdthsLine3",
                            postnummer = "0123",
                            poststed = "Oslo",
                            landkode = "NO"
                        )
                    )
                ),
                createdBy = "S223456",
                finished = LocalDateTime.now(),
                behandlingId = UUID.randomUUID(),
                willCreateNewJournalpost = false,
                muligheterFetched = LocalDateTime.now(),
            )
        )

        testEntityManager.clear()

        val registreringerFromDb = registreringRepository.findFerdigeRegistreringer(
            navIdent = "S223456",
            finishedFrom = LocalDateTime.now().minusDays(1)
        )

        assertThat(registreringerFromDb).hasSize(1)
    }

    @Test
    fun `uferdige registreringer works`() {
        testEntityManager.persistAndFlush(
            Registrering(
                sakenGjelder = PartId(type = PartIdType.PERSON, value = "12345678910"),
                klager = PartId(type = PartIdType.PERSON, value = "22345678910"),
                fullmektig = PartId(type = PartIdType.PERSON, value = "32345678910"),
                avsender = PartId(type = PartIdType.PERSON, value = "42345678910"),
                journalpostId = "123456789",
                journalpostDatoOpprettet = LocalDate.now(),
                type = Type.KLAGE,
                mulighetId = UUID.randomUUID(),
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
                svarbrevReceivers = mutableSetOf(
                    SvarbrevReceiver(
                        part = PartId(
                            type = PartIdType.PERSON,
                            value = "52345678910"
                        ),
                        handling = HandlingEnum.AUTO,
                        overriddenAddress = Address(
                            adresselinje1 = "addressLine1",
                            adresselinje2 = "addressLine2",
                            adresselinje3 = "addressLine3",
                            postnummer = "1234",
                            poststed = "Oslo",
                            landkode = "NO"
                        )
                    ),
                    SvarbrevReceiver(
                        part = PartId(
                            type = PartIdType.VIRKSOMHET,
                            value = "123456789"
                        ),
                        handling = HandlingEnum.AUTO,
                        overriddenAddress = Address(
                            adresselinje1 = "addr",
                            adresselinje2 = "rsdtdstst",
                            adresselinje3 = "addressdthdthdthsLine3",
                            postnummer = "0123",
                            poststed = "Oslo",
                            landkode = "NO"
                        )
                    )
                ),
                createdBy = "S223456",
                finished = null,
                behandlingId = null,
                willCreateNewJournalpost = false,
                muligheterFetched = LocalDateTime.now(),
            )
        )

        testEntityManager.clear()

        val registreringerFromDb = registreringRepository.findUferdigeRegistreringer(navIdent = "S223456")

        assertThat(registreringerFromDb).hasSize(1)
    }


}