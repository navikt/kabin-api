package no.nav.klage.api.controller

import no.nav.klage.api.controller.view.*
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.service.DokArkivService
import no.nav.klage.service.KabalApiService
import no.nav.klage.service.PDFService
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.klage.util.logMethodDetails
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class CommonController(
    private val tokenUtil: TokenUtil,
    private val kabalApiService: KabalApiService,
    private val dokArkivService: DokArkivService,
    private val pdfService: PDFService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @PostMapping("/searchpart")
    fun searchPart(
        @RequestBody input: SearchPartInput,
    ): PartView {
        logMethodDetails(
            methodName = ::searchPart.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return kabalApiService.searchPart(searchPartInput = input).toView()
    }

    @PostMapping("/calculatefrist")
    fun calculateFrist(
        @RequestBody input: CalculateFristInput,
    ): LocalDate {
        logMethodDetails(
            methodName = ::calculateFrist.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return input.fromDate.plusWeeks(input.fristInWeeks.toLong())
    }

    @PostMapping("/willcreatenewjournalpost")
    fun willCreateNewJournalpost(
        @RequestBody input: WillCreateNewJournalpostInput,
    ): Boolean {
        logMethodDetails(
            methodName = ::calculateFrist.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return dokArkivService.journalpostIsFinalizedAndConnectedToFagsak(
            journalpostId = input.journalpostId,
            fagsakId = input.fagsakId,
            fagsystemId = input.fagsystemId,
        )
    }

    @ResponseBody
    @PostMapping("/svarbrev-preview")
    fun getSvarbrevPreview(
        @RequestBody input: CreateAnkeInputView,
    ): ResponseEntity<ByteArray> {
        logger.debug("Kall mottatt på getSvarbrevPreview")
        secureLogger.debug("Kall mottatt på getSvarbrevPreview med input: {}", input)

        pdfService.getSvarbrevPDF(input).let {
            val responseHeaders = HttpHeaders()
            responseHeaders.contentType = MediaType.APPLICATION_PDF
            responseHeaders.add("Content-Disposition", "inline; filename=svarbrev.pdf")
            return ResponseEntity(
                it,
                responseHeaders,
                HttpStatus.OK
            )
        }
    }
}