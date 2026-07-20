package no.nav.klage.service

import no.nav.klage.clients.klagefssproxy.KlageFssProxyClient
import no.nav.klage.clients.klageunleashproxy.KlageUnleashProxyClient
import no.nav.klage.clients.klanke.*
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class KlankeService(
    private val klankeClient: KlankeClient,
    private val klageFssProxyClient: KlageFssProxyClient,
    private val klageUnleashProxyClient: KlageUnleashProxyClient,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)

        private const val USE_NEW_KLANKE = "use-new-klanke"
    }

    private fun useNewKlanke(navIdent: String): Boolean {
        val enabled = klageUnleashProxyClient.isEnabled(feature = USE_NEW_KLANKE, navIdent = navIdent)
        if (enabled) {
            logger.debug("Using new-klanke for navident {}", navIdent)
        }
        return enabled
    }

    fun searchKlanke(input: KlankeSearchInput, token: String): Mono<List<SakFromKlanke>> {
        return if (useNewKlanke(navIdent = tokenUtil.getCurrentIdent())) {
            klankeClient.searchKlanke(input = input)
        } else {
            klageFssProxyClient.searchKlanke(input = input, token = token)
        }
    }

    fun getSakAppAccess(sakId: String, saksbehandlerIdent: String): SakFromKlanke {
        return if (useNewKlanke(navIdent = saksbehandlerIdent)) {
            klankeClient.getSakAppAccess(sakId = sakId, saksbehandlerIdent = saksbehandlerIdent)
        } else {
            klageFssProxyClient.getSakAppAccess(sakId = sakId, saksbehandlerIdent = saksbehandlerIdent)
        }
    }

    fun checkAccess(): Access {
        return if (useNewKlanke(navIdent = tokenUtil.getCurrentIdent())) {
            klankeClient.checkAccess()
        } else {
            klageFssProxyClient.checkAccess()
        }
    }

    fun setToHandledInKabal(sakId: String, input: HandledInKabalInput) {
        if (useNewKlanke(navIdent = tokenUtil.getCurrentIdent())) {
            klankeClient.setToHandledInKabal(sakId = sakId, input = input)
        } else {
            klageFssProxyClient.setToHandledInKabal(sakId = sakId, input = input)
        }
    }
}
