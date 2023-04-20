package no.nav.klage.api.controller

import no.nav.klage.api.controller.mapper.toView
import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.kabalapi.CreatedBehandlingResponse
import no.nav.klage.clients.kabalapi.KabalApiClient
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.service.DokArkivService
import no.nav.klage.service.GenericApiService
import no.nav.klage.util.*
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class AnkeBasedOnKabalKlageController(
    private val genericApiService: GenericApiService,
    private val dokArkivService: DokArkivService,
    private val tokenUtil: TokenUtil,
    private val validationUtil: ValidationUtil
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @PostMapping("/createanke", produces = ["application/json"])
    fun createAnke(@RequestBody input: CreateAnkeBasedOnKlagebehandlingView): CreatedBehandlingResponse {
        logMethodDetails(
            methodName = ::createAnke.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )

        secureLogger.debug("createAnke called with: {}", input)

        validationUtil.validateCreateAnkeInput(input)

        val journalpostId = dokArkivService.handleJournalpost(
            journalpostId = input.ankeDocumentJournalpostId,
            klagebehandlingId = input.klagebehandlingId,
            avsender = input.avsender
        )

        return genericApiService.createAnkeInKabal(input.copy(ankeDocumentJournalpostId = journalpostId))
    }

    @PostMapping("/ankemuligheter", produces = ["application/json"])
    fun getCompletedKlagebehandlingerByIdnummer(@RequestBody input: IdnummerInput): List<Ankemulighet> {
        logMethodDetails(
            methodName = ::getCompletedKlagebehandlingerByIdnummer.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )

        return genericApiService.getCompletedKlagebehandlingerByIdnummer(input).map {
            Ankemulighet(
                behandlingId = it.behandlingId,
                ytelseId = it.ytelseId,
                utfallId = it.utfallId,
                vedtakDate = it.vedtakDate,
                sakenGjelder = it.sakenGjelder.toView(),
                klager = it.klager.toView(),
                fullmektig = it.fullmektig?.toView(),
                tilknyttedeDokumenter = it.tilknyttedeDokumenter,
                sakFagsakId = it.fagsakId,
                fagsakId = it.fagsakId,
                sakFagsystem = it.fagsystem,
                fagsystem = it.fagsystem,
                fagsystemId = it.fagsystemId,
                klageBehandlendeEnhet = it.klageBehandlendeEnhet,
            )
        }
    }

    @GetMapping("/anker/{mottakId}/status")
    fun createdAnkeStatus(
        @PathVariable mottakId: UUID,
    ): CreatedAnkebehandlingStatusView {
        logMethodDetails(
            methodName = ::createdAnkeStatus.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )

        val response = genericApiService.getCreatedAnkeStatus(mottakId = mottakId)

        return CreatedAnkebehandlingStatusView(
            typeId = response.typeId,
            behandlingId = response.behandlingId,
            ytelseId = response.ytelseId,
            utfallId = response.utfallId,
            vedtakDate = response.vedtakDate,
            sakenGjelder = response.sakenGjelder.toView(),
            klager = response.klager.toView(),
            fullmektig = response.fullmektig?.toView(),
            tilknyttedeDokumenter = response.tilknyttedeDokumenter,
            mottattNav = response.mottattNav,
            mottattKlageinstans = response.mottattNav,
            frist = response.frist,
            fagsakId = response.fagsakId,
            fagsystemId = response.fagsystemId,
            journalpost = response.journalpost.toView()
        )
    }
}