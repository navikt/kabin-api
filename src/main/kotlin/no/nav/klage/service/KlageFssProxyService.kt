package no.nav.klage.service

import no.nav.klage.api.controller.view.IdnummerInput
import no.nav.klage.clients.klanke.HandledInKabalInput
import no.nav.klage.clients.klanke.KlankeSearchInput
import no.nav.klage.clients.klanke.SakFromKlanke
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class KlageFssProxyService(
    private val klankeService: KlankeService,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getAnkemuligheterAsMono(input: IdnummerInput, token: String): Mono<List<SakFromKlanke>> {
        return if (klankeService.checkAccess().access) {
            klankeService.searchKlanke(
                input = KlankeSearchInput(fnr = input.idnummer, sakstype = "ANKE"),
                token = token,
            )
        } else Mono.empty()
    }

    fun getKlagemuligheterAsMono(input: IdnummerInput, token: String): Mono<List<SakFromKlanke>> {
        //Deliberately fail if missing access.
        val klageSaker = klankeService.searchKlanke(
            input = KlankeSearchInput(fnr = input.idnummer, sakstype = "KLAGE"),
            token = token,
        )
        return klageSaker
    }

    fun getKlageTilbakebetalingMuligheterAsMono(input: IdnummerInput, token: String): Mono<List<SakFromKlanke>> {
        //Deliberately fail if missing access.
        val klageTilbakebetalingSaker = klankeService.searchKlanke(
            input = KlankeSearchInput(
                fnr = input.idnummer,
                sakstype = "KLAGE_TILBAKEBETALING"
            ),
            token = token,
        )
        return klageTilbakebetalingSaker
    }

    fun setToHandledInKabal(sakId: String, frist: LocalDate) {
        klankeService.setToHandledInKabal(
            sakId = sakId,
            input = HandledInKabalInput(
                fristAsString = frist.format(DateTimeFormatter.BASIC_ISO_DATE)
            ),
        )
    }
}