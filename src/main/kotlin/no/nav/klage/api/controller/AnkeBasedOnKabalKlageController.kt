package no.nav.klage.api.controller

import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.KabalApiClient
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.service.DokArkivService
import no.nav.klage.service.KabalApiService
import no.nav.klage.util.*
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class AnkeBasedOnKabalKlageController(
    private val kabalApiService: KabalApiService,
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
    fun createAnke(@RequestBody input: CreateAnkeBasedOnKlagebehandling): KabalApiClient.CreatedAnkeResponse {
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

        return kabalApiService.createAnkeInKabal(input.copy(ankeDocumentJournalpostId = journalpostId))
    }

    @PostMapping("/ankemuligheter", produces = ["application/json"])
    fun getCompletedKlagebehandlingerByIdnummer(@RequestBody input: IdnummerInput): List<Ankemulighet> {
        logMethodDetails(
            methodName = ::getCompletedKlagebehandlingerByIdnummer.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )

        return kabalApiService.getCompletedKlagebehandlingerByIdnummer(input).map {
            Ankemulighet(
                behandlingId = it.behandlingId,
                ytelseId = it.ytelseId,
                utfallId = it.utfallId,
                vedtakDate = it.vedtakDate,
                sakenGjelder = it.sakenGjelder,
                klager = it.klager,
                fullmektig = it.fullmektig,
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
    ): KabalApiClient.CreatedBehandlingStatus {
        logMethodDetails(
            methodName = ::createdAnkeStatus.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )
        return kabalApiService.getCreatedAnkeStatus(mottakId = mottakId)
    }
}