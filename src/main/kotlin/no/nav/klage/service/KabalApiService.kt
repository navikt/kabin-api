package no.nav.klage.service

import no.nav.klage.api.controller.view.IdnummerInput
import no.nav.klage.api.controller.view.SearchPartInput
import no.nav.klage.api.controller.view.SearchPartWithUtsendingskanalInput
import no.nav.klage.clients.kabalapi.*
import no.nav.klage.domain.entities.Mulighet
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.kodeverk.TimeUnitType
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

    fun searchPartWithUtsendingskanal(searchPartInput: SearchPartWithUtsendingskanalInput): PartViewWithUtsendingskanal {
        return kabalApiClient.searchPartWithUtsendingskanal(searchPartInput = searchPartInput)
    }

    fun checkBehandlingDuplicate(input: BehandlingIsDuplicateInput, token: String): Mono<BehandlingIsDuplicateResponse> {
        return kabalApiClient.checkBehandlingDuplicate(input = input, token = token)
    }

    fun getBehandlingStatus(behandlingId: UUID): CreatedBehandlingStatus {
        return kabalApiClient.getBehandlingStatus(behandlingId = behandlingId)
    }

    fun gosysOppgaveIsDuplicate(gosysOppgaveId: Long): Boolean {
        return kabalApiClient.checkGosysOppgaveDuplicate(
            input = GosysOppgaveIsDuplicateInput(gosysOppgaveId = gosysOppgaveId)
        )
    }

    fun searchPart(searchPartInput: SearchPartInput): SearchPartView {
        return kabalApiClient.searchPart(searchPartInput = searchPartInput)
    }

    fun getAnkemuligheterAsMono(input: IdnummerInput, token: String): Mono<List<MulighetFromKabal>> {
        return kabalApiClient.getAnkemuligheterByIdnummer(
            idnummerInput = input,
            token = token,
        )
    }

    fun getOmgjoeringskravmuligheterAsMono(input: IdnummerInput, token: String): Mono<List<MulighetFromKabal>> {
        return kabalApiClient.getOmgjoeringskravmuligheterByIdnummer(
            idnummerInput = input,
            token = token,
        )
    }

    fun getGjenopptaksmuligheterAsMono(input: IdnummerInput, token: String): Mono<List<MulighetFromKabal>> {
        return kabalApiClient.getGjenopptaksmuligheterByIdnummer(
            idnummerInput = input,
            token = token,
        )
    }

    fun createAnkeFromInfotrygdInput(
        registrering: Registrering,
        mulighet: Mulighet,
        frist: LocalDate,
        journalpostId: String,
    ): UUID {
        val svarbrevSettings = getSvarbrevSettings(
            ytelseId = registrering.ytelse!!.id,
            typeId = registrering.type!!.id,
        )
        return kabalApiClient.createAnkeFromInfotrygdInput(
            CreateAnkeBasedOnKabinInput(
                sakenGjelder = OversendtPartId(
                    type = OversendtPartIdType.PERSON,
                    value = mulighet.sakenGjelder.part.value
                ),
                klager = registrering.klager.toOversendtPartId(),
                fullmektig = registrering.fullmektig.toOversendtPartId(),
                fagsakId = mulighet.fagsakId,
                fagsystemId = mulighet.originalFagsystem.id,
                hjemmelIdList = registrering.hjemmelIdList,
                forrigeBehandlendeEnhet = mulighet.klageBehandlendeEnhet,
                ankeJournalpostId = journalpostId,
                mottattNav = registrering.mottattKlageinstans!!,
                frist = frist,
                ytelseId = registrering.ytelse!!.id,
                kildereferanse = mulighet.currentFagystemTechnicalId,
                saksbehandlerIdent = registrering.saksbehandlerIdent,
                svarbrevInput = registrering.toSvarbrevInput(svarbrevSettings),
                gosysOppgaveId = registrering.gosysOppgaveId!!,
            )
        ).behandlingId
    }

    fun createBehandlingBasedOnJournalpost(
        registrering: Registrering,
        mulighet: Mulighet,
        journalpostId: String,
    ): UUID {
        val svarbrevSettings = getSvarbrevSettings(
            ytelseId = registrering.ytelse!!.id,
            typeId = registrering.type!!.id,
        )
        return kabalApiClient.createBehandlingBasedOnJournalpost(
            CreateBehandlingBasedOnJournalpostInput(
                typeId = registrering.type!!.id,
                sakenGjelder = OversendtPartId(
                    type = OversendtPartIdType.PERSON,
                    value = mulighet.sakenGjelder.part.value
                ),
                klager = registrering.klager.toOversendtPartId(),
                fullmektig = registrering.fullmektig.toOversendtPartId(),
                fagsakId = mulighet.fagsakId,
                fagsystemId = mulighet.originalFagsystem.id,
                hjemmelIdList = registrering.hjemmelIdList,
                forrigeBehandlendeEnhet = mulighet.klageBehandlendeEnhet,
                receivedDocumentJournalpostId = journalpostId,
                mottattNav = registrering.mottattKlageinstans!!,
                frist = when (registrering.behandlingstidUnitType) {
                    TimeUnitType.WEEKS -> registrering.mottattKlageinstans!!.plusWeeks(registrering.behandlingstidUnits.toLong())
                    TimeUnitType.MONTHS -> registrering.mottattKlageinstans!!.plusMonths(registrering.behandlingstidUnits.toLong())
                },
                ytelseId = registrering.ytelse!!.id,
                kildereferanse = mulighet.currentFagystemTechnicalId,
                saksbehandlerIdent = registrering.saksbehandlerIdent,
                svarbrevInput = registrering.toSvarbrevInput(svarbrevSettings),
                //Gosys-oppgave is ensured in validation step.
                gosysOppgaveId = registrering.gosysOppgaveId!!,
            )
        ).behandlingId
    }

    fun createBehandlingFromKabalInput(
        journalpostId: String,
        mulighet: Mulighet,
        registrering: Registrering
    ): UUID {
        val svarbrevSettings = getSvarbrevSettings(
                ytelseId = registrering.ytelse!!.id,
                typeId = registrering.type!!.id,
            )

        return kabalApiClient.createBehandlingBasedOnKabal(
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

    fun createKlageFromInfotrygdInput(
        registrering: Registrering,
        frist: LocalDate,
        mulighet: Mulighet,
        journalpostId: String,
    ): UUID {
        val svarbrevSettings = getSvarbrevSettings(
            ytelseId = registrering.ytelse!!.id,
            typeId = registrering.type!!.id,
        )

        return kabalApiClient.createKlage(
            input = CreateKlageBasedOnKabinInput(
                sakenGjelder = OversendtPartId(
                    type = OversendtPartIdType.PERSON,
                    value = mulighet.sakenGjelder.part.value,
                ),
                klager = registrering.klager.toOversendtPartId(),
                fullmektig = registrering.fullmektig.toOversendtPartId(),
                fagsakId = mulighet.fagsakId,
                fagsystemId = mulighet.originalFagsystem.id,
                hjemmelIdList = registrering.hjemmelIdList,
                forrigeBehandlendeEnhet = mulighet.klageBehandlendeEnhet,
                klageJournalpostId = journalpostId,
                brukersHenvendelseMottattNav = registrering.mottattVedtaksinstans!!,
                sakMottattKa = registrering.mottattKlageinstans!!,
                frist = frist,
                ytelseId = registrering.ytelse!!.id,
                kildereferanse = mulighet.currentFagystemTechnicalId,
                saksbehandlerIdent = registrering.saksbehandlerIdent,
                //Gosys-oppgave is ensured in validation step.
                gosysOppgaveId = registrering.gosysOppgaveId!!,
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