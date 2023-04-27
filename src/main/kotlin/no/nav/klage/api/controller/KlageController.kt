package no.nav.klage.api.controller

import no.nav.klage.api.controller.mapper.toView
import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.KlageFssProxyClient
import no.nav.klage.clients.KlankeSearchInput
import no.nav.klage.clients.kabalapi.CreatedBehandlingResponse
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.Tema
import no.nav.klage.kodeverk.infotrygdKlageutfallToUtfall
import no.nav.klage.service.DokArkivService
import no.nav.klage.service.GenericApiService
import no.nav.klage.util.*
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class KlageController(
    private val tokenUtil: TokenUtil,
    private val fssProxyClient: KlageFssProxyClient,
    private val genericApiService: GenericApiService,
    private val validationUtil: ValidationUtil,
    private val dokArkivService: DokArkivService
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @PostMapping("/createklage", produces = ["application/json"])
    fun createKlage(@RequestBody input: CreateKlageInputView): CreatedBehandlingResponse {
        logMethodDetails(
            methodName = ::createKlage.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )

        secureLogger.debug("createklage called with: {}", input)

        val processedInput = validationUtil.validateCreateKlageInputView(input)

        val journalpostId = dokArkivService.handleJournalpostBasedOnInfotrygdSak(
            journalpostId = processedInput.klageJournalpostId,
            sakId = processedInput.sakId,
            avsender = input.avsender
        )

        return genericApiService.createKlage(processedInput.copy(klageJournalpostId = journalpostId))
    }

    @PostMapping("/klagemuligheter", produces = ["application/json"])
    fun getCompletedKlagebehandlingerInVedtaksinstansByIdnummer(@RequestBody input: IdnummerInput): List<Klagemulighet> {
        logMethodDetails(
            methodName = ::getCompletedKlagebehandlingerInVedtaksinstansByIdnummer.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )

        return fssProxyClient.searchKlanke(KlankeSearchInput(fnr = input.idnummer, sakstype = "KLAGE"))
            .filter {
                !genericApiService.klagemulighetIsDuplicate(
                    fagsystem = Fagsystem.IT01,
                    kildereferanse = it.sakId
                )
            }
            .map {
                Klagemulighet(
                    sakId = it.sakId,
                    temaId = Tema.fromNavn(it.tema).id,
                    utfall = it.utfall,
                    utfallId = infotrygdKlageutfallToUtfall[it.utfall]!!.id,
                    vedtakDate = it.vedtaksdato,
                    fagsakId = it.fagsakId,
                    //TODO: Tilpass når vi får flere fagsystemer.
                    fagsystemId = Fagsystem.IT01.id,
                    klageBehandlendeEnhet = it.enhetsnummer,
                    sakenGjelder = genericApiService.searchPart(SearchPartInput(identifikator = it.fnr)).toView()
                )
            }
    }

    @GetMapping("/klager/{mottakId}/status")
    fun createdKlageStatus(
        @PathVariable mottakId: UUID,
    ): CreatedKlagebehandlingStatusView {
        logMethodDetails(
            methodName = ::createdKlageStatus.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )

        val status = genericApiService.getCreatedKlageStatus(mottakId = mottakId)

        //TODO works only for klager in Infotrygd
        val sakFromKlanke = fssProxyClient.getSak(status.kildereferanse)

        return CreatedKlagebehandlingStatusView(
            typeId = status.typeId,
            behandlingId = status.behandlingId,
            ytelseId = status.ytelseId,
            utfall = sakFromKlanke.utfall,
            utfallId = infotrygdKlageutfallToUtfall[sakFromKlanke.utfall]!!.id,
            vedtakDate = sakFromKlanke.vedtaksdato,
            sakenGjelder = status.sakenGjelder.toView(),
            klager = status.klager.toView(),
            fullmektig = status.fullmektig?.toView(),
            mottattVedtaksinstans = status.mottattVedtaksinstans,
            mottattKlageinstans = status.mottattKlageinstans,
            frist = status.frist,
            fagsakId = status.fagsakId,
            fagsystemId = status.fagsystemId,
            journalpost = status.journalpost.toView()
        )
    }
}