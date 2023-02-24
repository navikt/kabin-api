package no.nav.klage.api.controller

import io.swagger.v3.oas.annotations.Operation
import no.nav.klage.api.controller.view.CreateAnkeBasedOnKlagebehandling
import no.nav.klage.api.controller.view.DokumenterResponse
import no.nav.klage.api.controller.view.IdnummerInput
import no.nav.klage.api.controller.view.SearchPartInput
import no.nav.klage.clients.KabalApiClient
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.kodeverk.Tema
import no.nav.klage.service.DocumentService
import no.nav.klage.service.DokArkivService
import no.nav.klage.service.KabalApiService
import no.nav.klage.util.*
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class Controller(
    private val kabalApiService: KabalApiService,
    private val documentService: DocumentService,
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
        )
        return kabalApiService.createAnkeInKabal(input)
    }

    @PostMapping("/ankemuligheter", produces = ["application/json"])
    fun getCompletedKlagebehandlingerByIdnummer(@RequestBody input: IdnummerInput): List<KabalApiClient.CompletedKlagebehandling> {
        logMethodDetails(
            methodName = ::getCompletedKlagebehandlingerByIdnummer.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )

        return kabalApiService.getCompletedKlagebehandlingerByIdnummer(input)
    }

    @Operation(
        summary = "Hent metadata om dokumenter for brukeren som saken gjelder"
    )
    @PostMapping("/arkivertedokumenter", produces = ["application/json"])
    fun fetchDokumenter(
        @RequestBody input: IdnummerInput,
        @RequestParam(required = false, name = "antall", defaultValue = "10") pageSize: Int,
        @RequestParam(required = false, name = "forrigeSide") previousPageRef: String? = null,
        @RequestParam(required = false, name = "temaer") temaer: List<String>? = emptyList()
    ): DokumenterResponse {
        logMethodDetails(
            methodName = ::fetchDokumenter.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )

        return documentService.fetchDokumentlisteForBruker(
            idnummer = input.idnummer,
            temaer = temaer?.map { Tema.of(it) } ?: emptyList(),
            pageSize = pageSize,
            previousPageRef = previousPageRef
        )
    }

    @PostMapping("/searchpart")
    fun searchPart(
        @RequestBody input: SearchPartInput,
    ): KabalApiClient.PartView {
        logMethodDetails(
            methodName = ::searchPart.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )
        return kabalApiService.searchPart(searchPartInput = input)
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