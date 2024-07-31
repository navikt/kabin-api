package no.nav.klage.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import no.nav.klage.api.controller.view.*
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.kodeverk.Tema
import no.nav.klage.service.DocumentService
import no.nav.klage.util.AuditLogger
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.logMethodDetails
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class DokumentController(
    private val documentService: DocumentService,
    private val tokenUtil: TokenUtil,
    private val auditLogger: AuditLogger,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Operation(
        summary = "Hent liste med dokumentreferanser for angitt bruker"
    )
    @PostMapping("/arkivertedokumenter", produces = ["application/json"])
    fun fetchDokumenter(
        @RequestBody input: IdnummerInput,
        @RequestParam(required = false, name = "temaer") temaer: List<String>? = emptyList()
    ): DokumenterResponse {
        logMethodDetails(
            methodName = ::fetchDokumenter.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return documentService.fetchDokumentlisteForBruker(
            idnummer = input.idnummer,
            temaer = temaer?.map { Tema.of(it) } ?: emptyList(),
        )
    }

    @Operation(
        summary = "Hent gitt dokument/journalpost"
    )
    @GetMapping("/arkivertedokumenter/{journalpostId}", produces = ["application/json"])
    fun fetchDokument(
        @PathVariable journalpostId: String,
        @RequestParam(required = false, name = "temaer") temaer: List<String>? = emptyList()
    ): DokumentReferanse {
        logMethodDetails(
            methodName = ::fetchDokument.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return documentService.fetchDokument(
            journalpostId = journalpostId,
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
        logMethodDetails(
            methodName = ::getArkivertDokumentPDF.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

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

    @Operation(
        summary = "Oppdaterer filnavn i dokumentarkivet",
        description = "Oppdaterer filnavn i dokumentarkivet"
    )
    @PutMapping("/journalposter/{journalpostId}/dokumenter/{dokumentInfoId}/tittel")
    fun updateTitle(
        @Parameter(description = "Id til journalpost")
        @PathVariable journalpostId: String,
        @Parameter(description = "Id til dokumentInfo")
        @PathVariable dokumentInfoId: String,
        @Parameter(description = "Ny tittel til dokumentet")
        @RequestBody input: UpdateDocumentTitleView
    ): UpdateDocumentTitleView {
        logMethodDetails(
            methodName = ::updateTitle.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        documentService.updateDocumentTitle(
            journalpostId = journalpostId,
            dokumentInfoId = dokumentInfoId,
            title = input.tittel
        )

        return input
    }

    @Operation(
        summary = "Legger logisk vedlegg til dokument",
        description = "Legger logisk vedlegg til dokument"
    )
    @PostMapping("/dokumenter/{dokumentInfoId}/logiskevedlegg")
    @ResponseStatus(HttpStatus.CREATED)
    fun addLogiskVedlegg(
        @Parameter(description = "Id til dokumentInfo")
        @PathVariable dokumentInfoId: String,
        @Parameter(description = "Tittel på nytt logisk vedlegg")
        @RequestBody input: LogiskVedleggInput
    ): LogiskVedleggResponse {
        logMethodDetails(
            methodName = ::addLogiskVedlegg.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

         return documentService.addLogiskVedlegg(
            dokumentInfoId = dokumentInfoId,
            title = input.tittel
        )
    }

    @Operation(
        summary = "Oppdaterer logisk vedlegg",
        description = "Oppdaterer logisk vedlegg"
    )
    @PutMapping("/dokumenter/{dokumentInfoId}/logiskevedlegg/{logiskVedleggId}")
    fun updateLogiskVedlegg(
        @Parameter(description = "Id til dokumentInfo")
        @PathVariable dokumentInfoId: String,
        @Parameter(description = "Id til logisk vedlegg")
        @PathVariable logiskVedleggId: String,
        @Parameter(description = "Ny tittel på logisk vedlegg")
        @RequestBody input: LogiskVedleggInput
    ): LogiskVedleggResponse {
        logMethodDetails(
            methodName = ::updateLogiskVedlegg.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return documentService.updateLogiskVedlegg(
            dokumentInfoId = dokumentInfoId,
            logiskVedleggId = logiskVedleggId,
            title = input.tittel
        )
    }

    @Operation(
        summary = "Sletter logisk vedlegg",
        description = "Sletter logisk vedlegg"
    )
    @DeleteMapping("/dokumenter/{dokumentInfoId}/logiskevedlegg/{logiskVedleggId}")
    fun deleteLogiskVedlegg(
        @Parameter(description = "Id til dokumentInfo")
        @PathVariable dokumentInfoId: String,
        @Parameter(description = "Id til logisk vedlegg")
        @PathVariable logiskVedleggId: String,
    ) {
        logMethodDetails(
            methodName = ::updateLogiskVedlegg.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        documentService.deleteLogiskVedlegg(
            dokumentInfoId = dokumentInfoId,
            logiskVedleggId = logiskVedleggId,
        )
    }
}