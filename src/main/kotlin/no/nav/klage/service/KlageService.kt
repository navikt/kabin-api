package no.nav.klage.service

import no.nav.klage.api.controller.mapper.toReceiptView
import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.kabalapi.toView
import no.nav.klage.domain.CreateKlageInput
import no.nav.klage.kodeverk.*
import no.nav.klage.util.MulighetSource
import no.nav.klage.util.ValidationUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.stereotype.Service
import java.util.*

@Service
class KlageService(
    private val validationUtil: ValidationUtil,
    private val dokArkivService: DokArkivService,
    private val klageFssProxyService: KlageFssProxyService,
    private val kabalApiService: KabalApiService,
    private val oppgaveService: OppgaveService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    fun createKlage(input: CreateKlageInputView): CreatedBehandlingResponse {
        val processedInput = validationUtil.validateCreateKlageInputView(input)
        val journalpostId = dokArkivService.handleJournalpostBasedOnInfotrygdSak(
            journalpostId = processedInput.klageJournalpostId,
            eksternBehandlingId = processedInput.eksternBehandlingId,
            avsender = input.avsender,
            type = Type.KLAGE,
        )

        val finalInput = processedInput.copy(klageJournalpostId = journalpostId)

        return CreatedBehandlingResponse(
            behandlingId = createKlageFromInfotrygdSak(input = finalInput)
        )
    }

    private fun createKlageFromInfotrygdSak(input: CreateKlageInput): UUID {
        val sakFromKlanke = klageFssProxyService.getSak(sakId = input.eksternBehandlingId)
        val frist = when(input.behandlingstidUnitType) {
            TimeUnitType.WEEKS -> input.mottattKlageinstans.plusWeeks(input.behandlingstidUnits.toLong())
            TimeUnitType.MONTHS -> input.mottattKlageinstans.plusMonths(input.behandlingstidUnits.toLong())
        }
        val behandlingId = kabalApiService.createKlageInKabalFromCompleteInput(
            input = input,
            sakFromKlanke = sakFromKlanke,
            frist = frist
        )

        klageFssProxyService.setToHandledInKabal(
            sakId = sakFromKlanke.sakId,
            frist = frist,
        )

        input.oppgaveId?.let {
            logger.debug("Attempting oppgave update")
            oppgaveService.updateOppgave(
                oppgaveId = it,
                frist = frist,
                tildeltSaksbehandlerIdent = input.saksbehandlerIdent,
            )
        }

        return behandlingId
    }

    fun getKlagemuligheter(input: IdnummerInput): List<Klagemulighet> {
        val resultsFromInfotrygd = klageFssProxyService.getKlagemuligheter(input = input)
        return resultsFromInfotrygd
            .filter {
                !kabalApiService.mulighetIsDuplicate(
                    fagsystem = Fagsystem.IT01,
                    kildereferanse = it.sakId,
                    type = Type.KLAGE,
                )
            }
            .map {
                Klagemulighet(
                    id = it.sakId,
                    temaId = Tema.fromNavn(it.tema).id,
                    vedtakDate = it.vedtaksdato,
                    fagsakId = it.fagsakId,
                    //TODO: Tilpass når vi får flere fagsystemer.
                    originalFagsystemId = Fagsystem.IT01.id,
                    klageBehandlendeEnhet = it.enhetsnummer,
                    sakenGjelder = kabalApiService.searchPartWithUtsendingskanal(
                        SearchPartWithUtsendingskanalInput(
                            identifikator = it.fnr,
                            sakenGjelderId = it.fnr,
                            //don't care which ytelse is picked, as long as Tema is correct. Could be prettier.
                            ytelseId = Ytelse.entries.find { y -> y.toTema().navn == it.tema }!!.id,
                        )
                    ).partViewWithUtsendingskanal(),
                    currentFagsystemId = MulighetSource.INFOTRYGD.fagsystem.id,
                )
            }
    }

    fun getCreatedKlageStatus(behandlingId: UUID): CreatedKlagebehandlingStatusView {
        val status = kabalApiService.getCreatedKlageStatus(behandlingId = behandlingId)

        //TODO works only for klager in Infotrygd
        val sakFromKlanke = klageFssProxyService.getSak(status.kildereferanse)

        return CreatedKlagebehandlingStatusView(
            typeId = status.typeId,
            ytelseId = status.ytelseId,
            vedtakDate = sakFromKlanke.vedtaksdato,
            sakenGjelder = status.sakenGjelder.partViewWithUtsendingskanal(),
            klager = status.klager.partViewWithUtsendingskanal(),
            fullmektig = status.fullmektig?.partViewWithUtsendingskanal(),
            mottattVedtaksinstans = status.mottattVedtaksinstans,
            mottattKlageinstans = status.mottattKlageinstans,
            frist = status.frist,
            varsletFrist = status.varsletFrist,
            varsletFristUnits = status.varsletFristUnits,
            varsletFristUnitTypeId = status.varsletFristUnitTypeId,
            fagsakId = status.fagsakId,
            fagsystemId = status.fagsystemId,
            journalpost = status.journalpost.toReceiptView(),
            tildeltSaksbehandler = status.tildeltSaksbehandler?.toView(),
            svarbrev = status.svarbrev?.toView(),
        )
    }
}