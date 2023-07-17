package no.nav.klage.api.controller

import no.nav.klage.api.controller.mapper.toView
import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.kabalapi.CreatedBehandlingResponse
import no.nav.klage.clients.kabalapi.toView
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.service.DokArkivService
import no.nav.klage.service.GenericApiService
import no.nav.klage.util.*
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class AnkeController(
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
    fun createAnke(@RequestBody input: CreateAnkeInputView): CreatedBehandlingResponse {
        logMethodDetails(
            methodName = ::createAnke.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )

        secureLogger.debug("createAnke called with: {}", input)

        val processedInput = validationUtil.validateCreateAnkeInputView(input)

        val journalpostId = dokArkivService.handleJournalpostBasedOnAnkeInput(processedInput)

        return genericApiService.createAnkeInKabal(processedInput.copy(ankeDocumentJournalpostId = journalpostId))
    }

    @PostMapping("/ankemuligheter", produces = ["application/json"])
    fun getAnkemuligheterByIdnummer(@RequestBody input: IdnummerInput): List<Ankemulighet> {
        logMethodDetails(
            methodName = ::getAnkemuligheterByIdnummer.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )

        return genericApiService.getAnkemuligheter(input = input)
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
            ytelseId = response.ytelseId,
            utfallId = response.utfallId,
            vedtakDate = response.vedtakDate,
            sakenGjelder = response.sakenGjelder.toView(),
            klager = response.klager.toView(),
            fullmektig = response.fullmektig?.toView(),
            mottattNav = response.mottattNav,
            mottattKlageinstans = response.mottattNav,
            frist = response.frist,
            fagsakId = response.fagsakId,
            fagsystemId = response.fagsystemId,
            journalpost = response.journalpost.toView(),
            tildeltSaksbehandler = response.tildeltSaksbehandler?.toView(),
        )
    }
}