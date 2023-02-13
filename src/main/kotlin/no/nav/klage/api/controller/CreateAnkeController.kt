package no.nav.klage.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import no.nav.klage.api.controller.view.CreateAnkeBasedOnKlagebehandling
import no.nav.klage.api.controller.view.DokumenterResponse
import no.nav.klage.api.controller.view.IdnummerInput
import no.nav.klage.clients.KabalApiClient
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.kodeverk.Tema
import no.nav.klage.service.DocumentService
import no.nav.klage.service.DokArkivService
import no.nav.klage.service.KabalApiService
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class CreateAnkeController(
    private val kabalApiService: KabalApiService,
    private val documentService: DocumentService,
    private val dokArkivService: DokArkivService
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @PostMapping("/createanke", produces = ["application/json"])
    fun createAnke(@RequestBody input: CreateAnkeBasedOnKlagebehandling) {
        secureLogger.debug("createAnke called with: {}", input)
        //TODO: Sjekk om vi må fullføre journalposten osv
        dokArkivService.updateSaksIdInJournalpost(
            journalpostId = input.ankeDocumentJournalpostId,
            sakFagsakId = input.sakFagsakId,
            sakFagsystem = input.sakFagsystem
        )
        kabalApiService.createAnkeInKabal(input)
    }

    @PostMapping("/ankemuligheter", produces = ["application/json"])
    fun getCompletedKlagebehandlingerByIdnummer(@RequestBody input: IdnummerInput): List<KabalApiClient.CompletedKlagebehandling> {
        secureLogger.debug("getCompletedKlagebehandlingerByIdnummer called for {}", input.idnummer)
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
        return documentService.fetchDokumentlisteForBruker(
            idnummer = input.idnummer,
            temaer = temaer?.map { Tema.of(it) } ?: emptyList(),
            pageSize = pageSize,
            previousPageRef = previousPageRef
        )
    }

    @Operation(
        summary = "Henter fil fra dokumentarkivet",
        description = "Henter fil fra dokumentarkivet som pdf gitt at saksbehandler har tilgang"
    )
    @ResponseBody
    @GetMapping("/journalposter/{journalpostId}/dokumenter/{dokumentInfoId}/pdf")
    fun getArkivertDokumentPDF(
        @Parameter(description = "Id til journalpost")
        @PathVariable journalpostId: String,
        @Parameter(description = "Id til dokumentInfo")
        @PathVariable dokumentInfoId: String,
    ): ResponseEntity<ByteArray> {
        val arkivertDokument = documentService.getArkivertDokument(
            journalpostId = journalpostId,
            dokumentInfoId = dokumentInfoId
        )

        val responseHeaders = HttpHeaders()
        responseHeaders.contentType = arkivertDokument.contentType
        responseHeaders.add("Content-Disposition", "inline")
        return ResponseEntity(
            arkivertDokument.bytes,
            responseHeaders,
            HttpStatus.OK
        )
    }

}