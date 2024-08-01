package no.nav.klage.api.controller

import no.nav.klage.api.controller.view.*
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.exceptions.IllegalInputException
import no.nav.klage.exceptions.MulighetNotFoundException
import no.nav.klage.service.AnkeService
import no.nav.klage.service.KlageService
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
    private val klageService: KlageService,
    private val ankeService: AnkeService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @PostMapping
    fun createRegistrering(): FullRegistreringView {
        logMethodDetails(
            methodName = ::createRegistrering.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.createRegistrering()
    }

    @GetMapping("/ferdige")
    fun getRegistreringerFerdige(
        @RequestParam(required = false) sidenDager: Int?,
    ): List<FullRegistreringView> {
        logMethodDetails(
            methodName = ::getRegistreringerFerdige.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return registreringService.getFerdigeRegistreringer(
            sidenDager = sidenDager,
        )
    }

    @GetMapping("/uferdige")
    fun getRegistreringerUferdige(
    ): List<FullRegistreringView> {
        logMethodDetails(
            methodName = ::getRegistreringerUferdige.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return registreringService.getUferdigeRegistreringer()
    }

    @GetMapping("/{id}")
    fun getRegistrering(
        @PathVariable id: UUID,
    ): FullRegistreringView {
        logMethodDetails(
            methodName = ::getRegistrering.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        return registreringService.getRegistrering(
            registreringId = id
        )
    }

    @DeleteMapping("/{id}")
    fun deleteRegistrering(
        @PathVariable id: UUID,
    ) {
        logMethodDetails(
            methodName = ::deleteRegistrering.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        registreringService.deleteRegistrering(
            registreringId = id
        )
    }

    @GetMapping("/{id}/validate")
    fun validateRegistrering(
        @PathVariable id: UUID,
    ) {
        logMethodDetails(
            methodName = ::getRegistrering.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        registreringService.validateRegistrering(
            registreringId = id
        )
    }

    @PutMapping("/{id}/saken-gjelder-value")
    fun updateSakenGjelderValue(
        @PathVariable id: UUID,
        @RequestBody input: SakenGjelderValueInput
    ): FullRegistreringView {
        logMethodDetails(
            methodName = ::updateSakenGjelderValue.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setSakenGjelderValue(registreringId = id, input = input)
    }

    @PutMapping("/{id}/journalpost-id")
    fun updateJournalpostId(
        @PathVariable id: UUID,
        @RequestBody input: JournalpostIdInput
    ): FullRegistreringView {
        logMethodDetails(
            methodName = ::updateJournalpostId.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setJournalpostId(registreringId = id, input = input)
    }

    @PutMapping("/{id}/type-id")
    fun updateTypeId(
        @PathVariable id: UUID,
        @RequestBody input: TypeIdInput
    ): TypeChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateTypeId.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setTypeId(registreringId = id, input = input)
    }

    @PutMapping("/{id}/mulighet")
    fun updateMulighet(
        @PathVariable id: UUID,
        @RequestBody input: MulighetInput
    ): MulighetChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateMulighet.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setMulighet(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/mottatt-vedtaksinstans")
    fun updateMottattVedtaksinstans(
        @PathVariable id: UUID,
        @RequestBody input: MottattVedtaksinstansInput
    ): MottattVedtaksinstansChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateMottattVedtaksinstans.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setMottattVedtaksinstans(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/mottatt-klageinstans")
    fun updateMottattKlageinstans(
        @PathVariable id: UUID,
        @RequestBody input: MottattKlageinstansInput
    ): MottattKlageinstansChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateMottattKlageinstans.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setMottattKlageinstans(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/behandlingstid")
    fun updateBehandlingstid(
        @PathVariable id: UUID,
        @RequestBody input: BehandlingstidInput
    ): BehandlingstidChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateBehandlingstid.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setBehandlingstid(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/hjemmel-id-list")
    fun updateHjemmelIdList(
        @PathVariable id: UUID,
        @RequestBody input: HjemmelIdListInput
    ): HjemmelIdListChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateHjemmelIdList.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setHjemmelIdList(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/ytelse-id")
    fun updateYtelseId(
        @PathVariable id: UUID,
        @RequestBody input: YtelseIdInput
    ): YtelseChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateYtelseId.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setYtelseId(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/fullmektig")
    fun updateFullmektig(
        @PathVariable id: UUID,
        @RequestBody input: PartIdInput?
    ): FullmektigChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateFullmektig.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setFullmektig(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/klager")
    fun updateKlager(
        @PathVariable id: UUID,
        @RequestBody input: PartIdInput?
    ): KlagerChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateKlager.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setKlager(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/avsender")
    fun updateAvsender(
        @PathVariable id: UUID,
        @RequestBody input: PartIdInput?
    ): AvsenderChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateAvsender.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setAvsender(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/saksbehandler-ident")
    fun updateSaksbehandlerIdent(
        @PathVariable id: UUID,
        @RequestBody input: SaksbehandlerIdentInput
    ): SaksbehandlerIdentChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateSaksbehandlerIdent.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setSaksbehandlerIdent(registreringId = id, input = input)
    }

    @PutMapping("/{id}/overstyringer/oppgave-id")
    fun updateOppgaveId(
        @PathVariable id: UUID,
        @RequestBody input: OppgaveIdInput
    ): OppgaveIdChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateOppgaveId.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setOppgaveId(registreringId = id, input = input)
    }

    @PutMapping("/{id}/svarbrev/send")
    fun updateSendSvarbrev(
        @PathVariable id: UUID,
        @RequestBody input: SendSvarbrevInput
    ): SendSvarbrevChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateSendSvarbrev.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setSendSvarbrev(registreringId = id, input = input)
    }

    @PutMapping("/{id}/svarbrev/override-custom-text")
    fun updateOverrideSvarbrevCustomText(
        @PathVariable id: UUID,
        @RequestBody input: SvarbrevOverrideCustomTextInput
    ): SvarbrevOverrideCustomTextChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateOverrideSvarbrevCustomText.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setSvarbrevOverrideCustomText(registreringId = id, input = input)
    }

    @PutMapping("/{id}/svarbrev/override-behandlingstid")
    fun updateOverrideSvarbrevBehandlingstid(
        @PathVariable id: UUID,
        @RequestBody input: SvarbrevOverrideBehandlingstidInput
    ): SvarbrevOverrideBehandlingstidChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateOverrideSvarbrevBehandlingstid.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setSvarbrevOverrideBehandlingstid(registreringId = id, input = input)
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
    ): SvarbrevCustomTextChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateSvarbrevCustomText.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setSvarbrevCustomText(registreringId = id, input = input)
    }

    @PutMapping("/{id}/svarbrev/title")
    fun updateSvarbrevTitle(
        @PathVariable id: UUID,
        @RequestBody input: SvarbrevTitleInput
    ): SvarbrevTitleChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateSvarbrevTitle.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.setSvarbrevTitle(registreringId = id, input = input)
    }

    @PostMapping("/{id}/svarbrev/receivers")
    fun updateSvarbrevReceivers(
        @PathVariable id: UUID,
        @RequestBody input: SvarbrevRecipientInput
    ): SvarbrevReceiverChangeRegistreringView {
        logMethodDetails(
            methodName = ::updateSvarbrevReceivers.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.addSvarbrevReceiver(registreringId = id, input = input)
    }

    @PutMapping("/{id}/svarbrev/receivers/{svarbrevReceiverId}")
    fun updateSvarbrevReceiver(
        @PathVariable id: UUID,
        @PathVariable svarbrevReceiverId: UUID,
        @RequestBody input: SvarbrevRecipientInput
    ) {
        logMethodDetails(
            methodName = ::updateSvarbrevReceiver.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.addSvarbrevReceiver(registreringId = id, input = input)
    }

    @DeleteMapping("/{id}/svarbrev/receivers/{svarbrevReceiverId}")
    fun deleteSvarbrevReceiver(
        @PathVariable id: UUID,
        @PathVariable svarbrevReceiverId: UUID,
    ): SvarbrevReceiverChangeRegistreringView {
        logMethodDetails(
            methodName = ::deleteSvarbrevReceiver.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        return registreringService.deleteSvarbrevReceiver(registreringId = id, svarbrevReceiverId = svarbrevReceiverId)
    }

    @PostMapping("/{id}/ferdigstill")
    fun finishRegistrering(
        @PathVariable id: UUID,
    ): FerdigstiltRegistreringView {
        logMethodDetails(
            methodName = ::finishRegistrering.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        TODO()
    }

    @GetMapping("/{id}/klagemulighet")
    fun getKlagemulighet(
        @PathVariable id: UUID,
    ): Klagemulighet {
        logMethodDetails(
            methodName = ::getKlagemulighet.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        val registrering = registreringService.getRegistrering(id)

        if (registrering.mulighet == null) {
            throw IllegalInputException("Mulighet er ikke satt på registreringen.")
        }

        val input = IdnummerInput(idnummer = registrering.sakenGjelderValue!!)

        return klageService.getKlagemuligheter(input = input).find {
            it.id == registrering.mulighet.id && it.fagsystemId == registrering.mulighet.fagsystemId
        } ?: throw MulighetNotFoundException("Klagemulighet ikke funnet.")
    }

    @GetMapping("/{id}/ankemulighet")
    fun getAnkemulighet(
        @PathVariable id: UUID,
    ): Ankemulighet {
        logMethodDetails(
            methodName = ::getAnkemulighet.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )

        val registrering = registreringService.getRegistrering(id)

        val input = IdnummerInput(idnummer = registrering.sakenGjelderValue!!)

        if (registrering.mulighet == null) {
            throw IllegalInputException("Mulighet er ikke satt på registreringen.")
        }

        return ankeService.getAnkemuligheter(input = input).find {
            it.id == registrering.mulighet.id && it.fagsystemId == registrering.mulighet.fagsystemId
        } ?: throw MulighetNotFoundException("Ankemulighet ikke funnet.")
    }

}