package no.nav.klage.util

import no.nav.klage.api.controller.view.CreateAnkeInputView
import no.nav.klage.exceptions.InvalidSourceException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ValidationUtilTest {

    var validationUtil = ValidationUtil()

    @Test
    fun wrongSourceIdGivesError() {
        val input = CreateAnkeInputView(
            behandlingId = null,
            id = null,
            sourceId = "",
            mottattKlageinstans = null,
            fristInWeeks = null,
            klager = null,
            fullmektig = null,
            journalpostId = null,
            ytelseId = null,
            hjemmelId = null,
            avsender = null,
            saksbehandlerIdent = null
        )

        assertThrows<InvalidSourceException> { validationUtil.validateCreateAnkeInputView(input = input) }
    }
}