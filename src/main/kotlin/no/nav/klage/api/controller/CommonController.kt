package no.nav.klage.api.controller

import no.nav.klage.api.controller.view.CalculateFristInput
import no.nav.klage.api.controller.view.CreatedBehandlingStatusView
import no.nav.klage.api.controller.view.GetGosysOppgaveListInput
import no.nav.klage.api.controller.view.GosysOppgaveView
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.kodeverk.Tema
import no.nav.klage.kodeverk.TimeUnitType
import no.nav.klage.service.GosysOppgaveService
import no.nav.klage.service.RegistreringService
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.logMethodDetails
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class CommonController(
    private val tokenUtil: TokenUtil,
    private val gosysOppgaveService: GosysOppgaveService,
    private val registreringService: RegistreringService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
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

        val inputType = if (input.varsletBehandlingstidUnitTypeId != null) {
            TimeUnitType.of(input.varsletBehandlingstidUnitTypeId)
        } else {
            input.varsletBehandlingstidUnitType!!
        }

        return no.nav.klage.util.calculateFrist(
            fromDate = input.fromDate,
            units = input.varsletBehandlingstidUnits.toLong(),
            unitType = inputType,
        )
    }

    //TODO: Gjør dette søket basert på registreringen, på samme måte som i Kabal
    @PostMapping("/searchgosysoppgave")
    fun searchGosysOppgaveList(
        @RequestBody input: GetGosysOppgaveListInput,
    ): List<GosysOppgaveView> {
        logMethodDetails(
            methodName = ::searchGosysOppgaveList.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return gosysOppgaveService.getGosysOppgaveList(
            fnr = input.identifikator,
            tema = input.temaId?.let { Tema.of(it) }
        )
    }

    @GetMapping("/behandlinger/{behandlingId}/status", "/anker/{behandlingId}/status", "/klager/{behandlingId}/status")
    fun getCreatedBehandlingStatus(
        @PathVariable behandlingId: UUID,
    ): CreatedBehandlingStatusView {
        logMethodDetails(
            methodName = ::getCreatedBehandlingStatus.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return registreringService.getCreatedBehandlingStatus(behandlingId)
    }
}