package no.nav.klage.api.controller

import no.nav.klage.api.controller.view.*
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.domain.BehandlingstidUnitType
import no.nav.klage.kodeverk.Tema
import no.nav.klage.service.DokArkivService
import no.nav.klage.service.KabalApiService
import no.nav.klage.service.OppgaveService
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.klage.util.logMethodDetails
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class CommonController(
    private val tokenUtil: TokenUtil,
    private val kabalApiService: KabalApiService,
    private val dokArkivService: DokArkivService,
    private val oppgaveService: OppgaveService,
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
        return kabalApiService.searchPart(searchPartInput = input).partView()
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
        return when(input.varsletBehandlingstidUnitType) {
            BehandlingstidUnitType.WEEKS -> input.fromDate.plusWeeks(input.varsletBehandlingstidUnits.toLong())
            BehandlingstidUnitType.MONTHS -> input.fromDate.plusMonths(input.varsletBehandlingstidUnits.toLong())
        }
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

    @PostMapping("/searchoppgave")
    fun searchOppgaveList(
        @RequestBody input: GetOppgaveListInput,
    ): List<OppgaveView> {
        logMethodDetails(
            methodName = ::searchOppgaveList.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return oppgaveService.getOppgaveList(
            fnr = input.identifikator,
            tema = input.temaId?.let { Tema.of(it) }
        )
    }
}