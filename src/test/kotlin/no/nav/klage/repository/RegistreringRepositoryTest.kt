package no.nav.klage.repository

import no.nav.klage.db.TestPostgresqlContainer
import no.nav.klage.domain.entities.*
import no.nav.klage.kodeverk.*
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
import java.time.temporal.ChronoUnit

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
        val registrering = testEntityManager.persistAndFlush(
            Registrering(
                sakenGjelder = PartId(type = PartIdType.PERSON, value = "12345678910"),
                klager = PartId(type = PartIdType.PERSON, value = "22345678910"),
                fullmektig = PartId(type = PartIdType.PERSON, value = "32345678910"),
                avsender = PartId(type = PartIdType.PERSON, value = "42345678910"),
                journalpostId = "123456789",
                type = Type.KLAGE,
                mulighetId = "123",
                mulighetFagsystem = Fagsystem.KABAL,
                mottattVedtaksinstans = LocalDate.now(),
                mottattKlageinstans = LocalDate.now(),
                behandlingstidUnits = 12,
                behandlingstidUnitType = TimeUnitType.WEEKS,
                hjemmelIdList = listOf("123", "456"),
                ytelse = Ytelse.OMS_PSB,
                saksbehandlerIdent = "S223456",
                oppgaveId = "923456789",
                sendSvarbrev = true,
                svarbrevTitle = "a title",
                svarbrevCustomText = "custom text",
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
                            addressLine1 = "addressLine1",
                            postnummer = "1234",
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
                            addressLine1 = "addr",
                            postnummer = "0123",
                            landkode = "NO"
                        )
                    )
                ),
                createdBy = "S123456"
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
        assertThat(registreringFromDb.type).isEqualTo(registrering.type)
        assertThat(registreringFromDb.mulighetId).isEqualTo(registrering.mulighetId)
        assertThat(registreringFromDb.mulighetFagsystem).isEqualTo(registrering.mulighetFagsystem)
        assertThat(registreringFromDb.mottattVedtaksinstans).isEqualTo(registrering.mottattVedtaksinstans)
        assertThat(registreringFromDb.mottattKlageinstans).isEqualTo(registrering.mottattKlageinstans)
        assertThat(registreringFromDb.behandlingstidUnits).isEqualTo(registrering.behandlingstidUnits)
        assertThat(registreringFromDb.behandlingstidUnitType).isEqualTo(registrering.behandlingstidUnitType)
        assertThat(registreringFromDb.hjemmelIdList).isEqualTo(registrering.hjemmelIdList)
        assertThat(registreringFromDb.ytelse).isEqualTo(registrering.ytelse)
        assertThat(registreringFromDb.saksbehandlerIdent).isEqualTo(registrering.saksbehandlerIdent)
        assertThat(registreringFromDb.oppgaveId).isEqualTo(registrering.oppgaveId)
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
        assertThat(firstSvarbrevReceiver.overriddenAddress!!.addressLine1).isEqualTo(registrering.svarbrevReceivers.first().overriddenAddress!!.addressLine1)
        assertThat(firstSvarbrevReceiver.overriddenAddress!!.postnummer).isEqualTo(registrering.svarbrevReceivers.first().overriddenAddress!!.postnummer)
        assertThat(firstSvarbrevReceiver.overriddenAddress!!.landkode).isEqualTo(registrering.svarbrevReceivers.first().overriddenAddress!!.landkode)

        assertThat(secondSvarbrevReceiver.id).isEqualTo(registrering.svarbrevReceivers.last().id)
        assertThat(secondSvarbrevReceiver.part).isEqualTo(registrering.svarbrevReceivers.last().part)
        assertThat(secondSvarbrevReceiver.handling).isEqualTo(registrering.svarbrevReceivers.last().handling)
        assertThat(secondSvarbrevReceiver.overriddenAddress!!.addressLine1).isEqualTo(registrering.svarbrevReceivers.last().overriddenAddress!!.addressLine1)
        assertThat(secondSvarbrevReceiver.overriddenAddress!!.postnummer).isEqualTo(registrering.svarbrevReceivers.last().overriddenAddress!!.postnummer)
        assertThat(secondSvarbrevReceiver.overriddenAddress!!.landkode).isEqualTo(registrering.svarbrevReceivers.last().overriddenAddress!!.landkode)
    }

}