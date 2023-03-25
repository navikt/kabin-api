package no.nav.klage.api.controller

import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.KlageFssProxyClient
import no.nav.klage.clients.KlankeSearchInput
import no.nav.klage.config.SecurityConfiguration
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.util.*
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class KlageController(
    private val tokenUtil: TokenUtil,
    private val fssProxyClient: KlageFssProxyClient,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @PostMapping("/createklage", produces = ["application/json"])
    fun createKlage() {
        logMethodDetails(
            methodName = ::createKlage.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )
        TODO()
    }

    @PostMapping("/klagemuligheter", produces = ["application/json"])
    fun getCompletedKlagebehandlingerInVedtaksinstansByIdnummer(@RequestBody input: IdnummerInput): List<Klagemulighet> {
        logMethodDetails(
            methodName = ::getCompletedKlagebehandlingerInVedtaksinstansByIdnummer.name,
            innloggetIdent = tokenUtil.getIdent(),
            logger = logger,
        )

        return fssProxyClient.searchKlanke(KlankeSearchInput(fnr = input.idnummer, sakstype = "KLAGE")).map {
            Klagemulighet(
                sakId = it.sakId,
                tema = it.tema,
                utfall = it.utfall,
                vedtakDate = it.vedtaksdato,
                sakFagsakId = it.fagsakId,
                sakFagsystem = Fagsystem.IT01,
                klageBehandlendeEnhet = it.enhetsnummer,
            )
        }
    }

}