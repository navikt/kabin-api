package no.nav.klage.util

import no.nav.klage.api.controller.view.CreateAnkeInputView
import no.nav.klage.api.controller.view.PartId
import no.nav.klage.api.controller.view.PartType
import no.nav.klage.api.controller.view.Vedtak
import no.nav.klage.exceptions.InvalidSourceException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ValidationUtilTest {

    private var validationUtil = ValidationUtil()

    @Test
    fun wrongSourceIdGivesError() {
        val input = CreateAnkeInputView(
            vedtak = Vedtak(
                id = "abc",
                sourceId = "WRONG",
                sakenGjelder = PartId(type = PartType.FNR, id = "12345678901")
            ),
            mottattKlageinstans = null,
            fristInWeeks = null,
            klager = null,
            fullmektig = null,
            journalpostId = null,
            ytelseId = null,
            hjemmelIdList = listOf(),
            avsender = null,
            saksbehandlerIdent = null,
            svarbrevInput = null,
        )

        assertThrows<InvalidSourceException> { validationUtil.validateCreateAnkeInputView(input = input) }
    }
}