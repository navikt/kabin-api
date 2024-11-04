package no.nav.klage.service

import no.nav.klage.api.controller.view.IdnummerInput
import no.nav.klage.api.controller.view.SearchPartInput
import no.nav.klage.clients.kabalapi.*
import no.nav.klage.domain.entities.Mulighet
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.TimeUnitType
import no.nav.klage.kodeverk.Type
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.*

@Service
class KabalApiService(
    private val kabalApiClient: KabalApiClient,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun gosysOppgaveIsDuplicate(gosysOppgaveId: Long): Boolean {
        return kabalApiClient.checkGosysOppgaveDuplicateInKabal(
            input = GosysOppgaveIsDuplicateInput(gosysOppgaveId = gosysOppgaveId)
        )
    }

    fun searchPart(searchPartInput: SearchPartInput): PartView {
        return kabalApiClient.searchPart(searchPartInput = searchPartInput)
    }

    fun getAnkemuligheterAsMono(input: IdnummerInput): Mono<List<MulighetFromKabal>> {
        return kabalApiClient.getAnkemuligheterByIdnummer(input)
    }

    fun getOmgjoeringskravmuligheterAsMono(input: IdnummerInput): Mono<List<MulighetFromKabal>> {
        return kabalApiClient.getOmgjoeringskravmuligheterByIdnummer(input)
    }

    fun createAnkeInKabalFromInfotrygdInput(
        registrering: Registrering,
        mulighet: Mulighet,
        frist: LocalDate,
        journalpostId: String,
    ): UUID {
        val svarbrevSettings = getSvarbrevSettings(
            ytelseId = registrering.ytelse!!.id,
            typeId = registrering.type!!.id,
        )
        return kabalApiClient.createAnkeFromInfotrygdInputInKabal(
            CreateAnkeBasedOnKabinInput(
                sakenGjelder = OversendtPartId(
                    type = OversendtPartIdType.PERSON,
                    value = mulighet.sakenGjelder.part.value
                ),
                klager = registrering.klager.toOversendtPartId(),
                fullmektig = registrering.fullmektig.toOversendtPartId(),
                fagsakId = mulighet.fagsakId,
                //TODO: Tilpass når vi får flere fagsystemer.
                fagsystemId = Fagsystem.IT01.id,
                hjemmelIdList = registrering.hjemmelIdList,
                forrigeBehandlendeEnhet = mulighet.klageBehandlendeEnhet,
                ankeJournalpostId = journalpostId,
                mottattNav = registrering.mottattKlageinstans!!,
                frist = frist,
                ytelseId = registrering.ytelse!!.id,
                kildereferanse = mulighet.currentFagystemTechnicalId,
                saksbehandlerIdent = registrering.saksbehandlerIdent,
                svarbrevInput = registrering.toSvarbrevInput(svarbrevSettings),
                gosysOppgaveId = registrering.gosysOppgaveId,
            )
        ).behandlingId
    }

    fun createBehandlingInKabalFromKabalInput(
        journalpostId: String,
        mulighet: Mulighet,
        registrering: Registrering
    ): UUID {
        val svarbrevSettings = if (registrering.type != Type.OMGJOERINGSKRAV) {
            getSvarbrevSettings(
                ytelseId = registrering.ytelse!!.id,
                typeId = registrering.type!!.id,
            )
        } else null

        return kabalApiClient.createBehandlingInKabal(
            CreateBehandlingBasedOnKabalInput(
                typeId = mulighet.type.id,
                sourceBehandlingId = UUID.fromString(mulighet.currentFagystemTechnicalId),
                mottattNav = registrering.mottattKlageinstans!!,
                frist = when (registrering.behandlingstidUnitType) {
                    TimeUnitType.WEEKS -> registrering.mottattKlageinstans!!.plusWeeks(registrering.behandlingstidUnits.toLong())
                    TimeUnitType.MONTHS -> registrering.mottattKlageinstans!!.plusMonths(registrering.behandlingstidUnits.toLong())
                },
                klager = registrering.klager.toOversendtPartId(),
                fullmektig = registrering.fullmektig.toOversendtPartId(),
                receivedDocumentJournalpostId = journalpostId,
                saksbehandlerIdent = registrering.saksbehandlerIdent,
                svarbrevInput = registrering.toSvarbrevInput(svarbrevSettings),
                hjemmelIdList = registrering.hjemmelIdList,
                gosysOppgaveId = registrering.gosysOppgaveId,
            )
        ).behandlingId
    }

    fun createKlageInKabalFromInfotrygdInput(
        registrering: Registrering,
        frist: LocalDate,
        mulighet: Mulighet,
        journalpostId: String,
    ): UUID {
        val svarbrevSettings = getSvarbrevSettings(
            ytelseId = registrering.ytelse!!.id,
            typeId = registrering.type!!.id,
        )

        return kabalApiClient.createKlageInKabal(
            input = CreateKlageBasedOnKabinInput(
                sakenGjelder = OversendtPartId(
                    type = OversendtPartIdType.PERSON,
                    value = mulighet.sakenGjelder.part.value,
                ),
                klager = registrering.klager.toOversendtPartId(),
                fullmektig = registrering.fullmektig.toOversendtPartId(),
                fagsakId = mulighet.fagsakId,
                //TODO: Tilpass når vi får flere fagsystemer.
                fagsystemId = Fagsystem.IT01.id,
                hjemmelIdList = registrering.hjemmelIdList,
                forrigeBehandlendeEnhet = mulighet.klageBehandlendeEnhet,
                klageJournalpostId = journalpostId,
                brukersHenvendelseMottattNav = registrering.mottattVedtaksinstans!!,
                sakMottattKa = registrering.mottattKlageinstans!!,
                frist = frist,
                ytelseId = registrering.ytelse!!.id,
                kildereferanse = mulighet.currentFagystemTechnicalId,
                saksbehandlerIdent = registrering.saksbehandlerIdent,
                gosysOppgaveId = registrering.gosysOppgaveId,
                svarbrevInput = registrering.toSvarbrevInput(svarbrevSettings),
            )
        ).behandlingId
    }

    fun getUsedJournalpostIdListForPerson(fnr: String): List<String> {
        return kabalApiClient.getUsedJournalpostIdListForPerson(fnr = fnr)
    }

    fun getSvarbrevSettings(ytelseId: String, typeId: String): SvarbrevSettingsView {
        return kabalApiClient.getSvarbrevSettings(ytelseId = ytelseId, typeId = typeId)
    }

}