package no.nav.klage.service

import no.nav.klage.api.controller.view.IdnummerInput
import no.nav.klage.clients.HandledInKabalInput
import no.nav.klage.clients.KlageFssProxyClient
import no.nav.klage.clients.KlankeSearchInput
import no.nav.klage.clients.SakFromKlanke
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class KlageFssProxyService(
    private val klageFssProxyClient: KlageFssProxyClient,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getAnkemuligheterAsMono(input: IdnummerInput, token: String): Mono<List<SakFromKlanke>> {
        return if (klageFssProxyClient.checkAccess().access) {
            klageFssProxyClient.searchKlanke(
                input = KlankeSearchInput(fnr = input.idnummer, sakstype = "ANKE"),
                token = token,
            )
        } else Mono.empty()
    }

    fun getKlagemuligheterAsMono(input: IdnummerInput, token: String): Mono<List<SakFromKlanke>> {
        //Deliberately fail if missing access.
        val klageSaker = klageFssProxyClient.searchKlanke(
            input = KlankeSearchInput(fnr = input.idnummer, sakstype = "KLAGE"),
            token = token,
        )
        return klageSaker
    }

    fun getKlageTilbakebetalingMuligheterAsMono(input: IdnummerInput, token: String): Mono<List<SakFromKlanke>> {
        //Deliberately fail if missing access.
        val klageTilbakebetalingSaker = klageFssProxyClient.searchKlanke(
            input = KlankeSearchInput(
                fnr = input.idnummer,
                sakstype = "KLAGE_TILBAKEBETALING"
            ),
            token = token,
        )
        return klageTilbakebetalingSaker
    }

    fun setToHandledInKabal(sakId: String, frist: LocalDate) {
        klageFssProxyClient.setToHandledInKabal(
            sakId = sakId,
            input = HandledInKabalInput(
                fristAsString = frist.format(DateTimeFormatter.BASIC_ISO_DATE)
            ),
        )
    }
}