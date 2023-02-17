package no.nav.klage.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import no.nav.klage.api.controller.view.*
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.service.DocumentService
import no.nav.klage.service.DokArkivService
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.logMethodDetails
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
@RequestMapping("/journalposter")
class JournalposterController(
    private val documentService: DocumentService,
    private val dokArkivService: DokArkivService,
    private val tokenUtil: TokenUtil
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Operation(
        summary = "Henter fil fra dokumentarkivet",
        description = "Henter fil fra dokumentarkivet som pdf gitt at saksbehandler har tilgang"
    )
    @ResponseBody
    @GetMapping("/{journalpostId}/dokumenter/{dokumentInfoId}/pdf")
    fun getArkivertDokumentPDF(
        @Parameter(description = "Id til journalpost")
        @PathVariable journalpostId: String,
        @Parameter(description = "Id til dokumentInfo")
        @PathVariable dokumentInfoId: String,
    ): ResponseEntity<ByteArray> {
        logMethodDetails(
            methodName = ::getArkivertDokumentPDF.name,
            innloggetIdent = tokenUtil.getIdent(),
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
    @PutMapping("/{journalpostId}/dokumenter/{dokumentInfoId}/tittel")
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
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )

        dokArkivService.updateDocumentTitle(
            journalpostId = journalpostId,
            dokumentInfoId = dokumentInfoId,
            title = input.tittel
        )

        return input
    }
}