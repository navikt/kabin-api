package no.nav.klage.util

import io.mockk.every
import io.mockk.mockk
import no.nav.klage.api.controller.view.CreateAnkeInputView
import no.nav.klage.api.controller.view.Vedtak
import no.nav.klage.exceptions.InvalidSourceException
import no.nav.klage.service.KabalApiService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ValidationUtilTest {


    private val kabalApiService: KabalApiService = mockk()

    private var validationUtil = ValidationUtil(kabalApiService = kabalApiService)

    @Test
    fun wrongSourceIdGivesError() {
        every { kabalApiService.oppgaveIsDuplicate(any()) } returns false

        val input = CreateAnkeInputView(
            vedtak = Vedtak(
                id = "abc",
                sourceId = "WRONG",
            ),
            mottattKlageinstans = null,
            behandlingstidUnits = null,
            behandlingstidUnitType = null,
            behandlingstidUnitTypeId = null,
            klager = null,
            fullmektig = null,
            journalpostId = null,
            ytelseId = null,
            hjemmelIdList = listOf(),
            avsender = null,
            saksbehandlerIdent = null,
            svarbrevInput = null,
            oppgaveId = null,
        )

        assertThrows<InvalidSourceException> { validationUtil.validateCreateAnkeInputView(input = input) }
    }
}