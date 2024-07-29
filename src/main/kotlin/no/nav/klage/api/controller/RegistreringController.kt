package no.nav.klage.api.controller

import no.nav.klage.api.controller.view.*
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.service.RegistreringService
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.klage.util.logMethodDetails
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
@RequestMapping("/registreringer")
class RegistreringController(
    private val registreringService: RegistreringService,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @PostMapping
    fun createRegistrering() {
        logMethodDetails(
            methodName = ::createRegistrering.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.createRegistrering(
            createdBy = tokenUtil.getCurrentIdent(),
        )
    }

    @GetMapping
    fun getRegistreringer(
        @RequestParam navIdent: String,
        @RequestParam fullfoert: Boolean,
        @RequestParam(required = false) sidenDager: Int?,
    ): List<RegistreringView> {
        logMethodDetails(
            methodName = ::getRegistreringer.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return registreringService.getRegistreringer(
            navIdent = navIdent,
            fullfoert = fullfoert,
            sidenDager = sidenDager,
        )
    }

    @GetMapping("/{id}")
    fun getRegistrering(
        @PathVariable id: UUID,
    ): RegistreringView {
        logMethodDetails(
            methodName = ::getRegistrering.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return registreringService.getRegistrering(
            registreringId = id
        )
    }

    @PutMapping("/{id}/journalpost-id")
    fun updateJournalpostId(
        @PathVariable id: UUID,
        @RequestBody input: JournalpostIdInput
    ) {
        logMethodDetails(
            methodName = ::updateJournalpostId.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setJournalpostId(registreringId = id, input = input)
    }

    @PutMapping("/{id}/type-id")
    fun updateTypeId(
        @PathVariable id: UUID,
        @RequestBody input: TypeIdInput
    ) {
        logMethodDetails(
            methodName = ::updateTypeId.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setTypeId(registreringId = id, input = input)
    }

    @PutMapping("/{id}/mulighet")
    fun updateMulighet(
        @PathVariable id: UUID,
        @RequestBody input: MulighetInput
    ) {
        logMethodDetails(
            methodName = ::updateMulighet.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setMulighet(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/mottatt-vedtaksinstans")
    fun updateMottattVedtaksinstans(
        @PathVariable id: UUID,
        @RequestBody input: MottattVedtaksinstansInput
    ) {
        logMethodDetails(
            methodName = ::updateMottattVedtaksinstans.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setMottattVedtaksinstans(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/mottatt-klageinstans")
    fun updateMottattKlageinstans(
        @PathVariable id: UUID,
        @RequestBody input: MottattKlageinstansInput
    ) {
        logMethodDetails(
            methodName = ::updateMottattKlageinstans.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setMottattKlageinstans(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/behandlingstid")
    fun updateBehandlingstid(
        @PathVariable id: UUID,
        @RequestBody input: BehandlingstidInput
    ) {
        logMethodDetails(
            methodName = ::updateBehandlingstid.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setBehandlingstid(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/hjemmel-id-list")
    fun updateHjemmelIdList(
        @PathVariable id: UUID,
        @RequestBody input: HjemmelIdListInput
    ) {
        logMethodDetails(
            methodName = ::updateHjemmelIdList.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setHjemmelIdList(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/ytelse-id")
    fun updateYtelseId(
        @PathVariable id: UUID,
        @RequestBody input: YtelseIdInput
    ) {
        logMethodDetails(
            methodName = ::updateYtelseId.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setYtelseId(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/fullmektig")
    fun updateFullmektig(
        @PathVariable id: UUID,
        @RequestBody input: PartIdInput
    ) {
        logMethodDetails(
            methodName = ::updateFullmektig.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setFullmektig(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/klager")
    fun updateKlager(
        @PathVariable id: UUID,
        @RequestBody input: PartIdInput
    ) {
        logMethodDetails(
            methodName = ::updateKlager.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setKlager(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/avsender")
    fun updateAvsender(
        @PathVariable id: UUID,
        @RequestBody input: PartIdInput
    ) {
        logMethodDetails(
            methodName = ::updateAvsender.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setAvsender(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/saksbehandler-ident")
    fun updateSaksbehandlerIdent(
        @PathVariable id: UUID,
        @RequestBody input: SaksbehandlerIdentInput
    ) {
        logMethodDetails(
            methodName = ::updateSaksbehandlerIdent.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setSaksbehandlerIdent(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/oppgave-id")
    fun updateOppgaveId(
        @PathVariable id: UUID,
        @RequestBody input: OppgaveIdInput
    ) {
        logMethodDetails(
            methodName = ::updateOppgaveId.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setOppgaveId(registreringId = id, input = input)
    }

    @PutMapping("/{id}/svarbrev/send")
    fun updateSendSvarbrev(
        @PathVariable id: UUID,
        @RequestBody input: SendSvarbrevInput
    ) {
        logMethodDetails(
            methodName = ::updateSendSvarbrev.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setSendSvarbrev(registreringId = id, input = input)
    }

    @PutMapping("/{id}/svarbrev/behandlingstid")
    fun updateSvarbrevBehandlingstid(
        @PathVariable id: UUID,
        @RequestBody input: BehandlingstidInput
    ) {
        logMethodDetails(
            methodName = ::updateSvarbrevBehandlingstid.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setSvarbrevBehandlingstid(registreringId = id, input = input)
    }

    @PutMapping("/{id}/svarbrev/fullmektig-fritekst")
    fun updateSvarbrevFullmektigFritekst(
        @PathVariable id: UUID,
        @RequestBody input: SvarbrevFullmektigFritekstInput
    ) {
        logMethodDetails(
            methodName = ::updateSvarbrevFullmektigFritekst.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setSvarbrevFullmektigFritekst(registreringId = id, input = input)
    }

    @PutMapping("/{id}/svarbrev/custom-text")
    fun updateSvarbrevCustomText(
        @PathVariable id: UUID,
        @RequestBody input: SvarbrevCustomTextInput
    ) {
        logMethodDetails(
            methodName = ::updateSvarbrevCustomText.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setSvarbrevCustomText(registreringId = id, input = input)
    }

    @PutMapping("/{id}/svarbrev/title")
    fun updateSvarbrevTitle(
        @PathVariable id: UUID,
        @RequestBody input: SvarbrevTitleInput
    ) {
        logMethodDetails(
            methodName = ::updateSvarbrevTitle.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setSvarbrevTitle(registreringId = id, input = input)
    }

    @PutMapping("/{id}/svarbrev/receivers")
    fun updateSvarbrevReceivers(
        @PathVariable id: UUID,
        @RequestBody input: SvarbrevReceiversInput
    ) {
        logMethodDetails(
            methodName = ::updateSvarbrevReceivers.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.setSvarbrevReceivers(registreringId = id, input = input)
    }

}