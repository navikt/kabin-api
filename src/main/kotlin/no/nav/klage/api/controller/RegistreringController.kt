package no.nav.klage.api.controller

import no.nav.klage.api.controller.view.CreateRegistrering
import no.nav.klage.api.controller.view.JournalpostIdInput
import no.nav.klage.api.controller.view.RegistreringView
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.service.RegistreringService
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.klage.util.logMethodDetails
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.util.*

/*
// PUT /registreringer/{id}/journalpost-id
interface UpdateJournalpostIdPayload {
  journalpostId: string;
}

// PUT /registreringer/{id}/type-id
interface UpdateTypeIdPayload {
  typeId: Type;
}

// PUT /registreringer/{id}/mulighet
type UpdateMulighetPayload = Mulighet;

// PUT /registreringer/{id}/overstyringer/mottatt-vedtaksinstans
interface UpdateMottattVedtaksinstansPayload {
  mottattVedtaksinstans: string;
}

// PUT /registreringer/{id}/overstyringer/mottatt-klageinstans
interface UpdateMottattKlageinstansPayload {
  mottattKlageinstans: string;
}

// PUT /registreringer/{id}/overstyringer/behandlingstid
type UpdateBehandlingstidPayload = Behandlingstid;

// PUT /registreringer/{id}/overstyringer/hjemmel-id-list
interface UpdateHjemmelIdPayload {
  hjemmelIdList: string[];
}

// PUT /registreringer/{id}/overstyringer/ytelse-id
interface UpdateYtelseIdPayload {
  ytelseId: string;
}

// PUT /registreringer/{id}/overstyringer/fullmektig
interface UpdatePartPayload {
  id: string;
  type: IdType.FNR;
}
 */

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
    fun createRegistrering(
        @RequestBody input: CreateRegistrering
    ) {
        logMethodDetails(
            methodName = ::createRegistrering.name,
            innloggetIdent = tokenUtil.getCurrentIdent(),
            logger = logger,
        )
        registreringService.createRegistrering(
            sakenGjelderValue = input.sakenGjelderValue,
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

}