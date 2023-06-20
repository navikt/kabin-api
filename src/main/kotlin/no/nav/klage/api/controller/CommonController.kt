package no.nav.klage.api.controller

import io.swagger.v3.oas.annotations.Operation
import no.nav.klage.api.controller.view.*
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.kodeverk.Tema
import no.nav.klage.service.DocumentService
import no.nav.klage.service.GenericApiService
import no.nav.klage.util.*
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class CommonController(
    private val genericApiService: GenericApiService,
    private val documentService: DocumentService,
    private val tokenUtil: TokenUtil,
    private val auditLogger: AuditLogger,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
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
        ).also {
            auditLogger.log(
                AuditLogEvent(
                    navIdent = tokenUtil.getIdent(),
                    personFnr = input.idnummer,
                    message = "Søkt opp person for å opprette klage/anke."
                )
            )
        }
    }

    @PostMapping("/searchpart")
    fun searchPart(
        @RequestBody input: SearchPartInput,
    ): PartView {
        logMethodDetails(
            methodName = ::searchPart.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )
        return genericApiService.searchPart(searchPartInput = input).toView()
    }

    @PostMapping("/calculatefrist")
    fun calculateFrist(
        @RequestBody input: CalculateFristInput,
    ): LocalDate {
        logMethodDetails(
            methodName = ::calculateFrist.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )
        return input.fromDate.plusWeeks(input.fristInWeeks.toLong())
    }
}