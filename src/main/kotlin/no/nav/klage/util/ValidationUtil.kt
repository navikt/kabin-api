package no.nav.klage.util

import no.nav.klage.api.controller.view.CreateAnkeBasedOnKlagebehandlingView
import no.nav.klage.api.controller.view.CreateKlageInput
import no.nav.klage.exceptions.InvalidProperty
import no.nav.klage.exceptions.SectionedValidationErrorWithDetailsException
import no.nav.klage.exceptions.ValidationSection
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ValidationUtil {
    fun validateCreateAnkeInput(input: CreateAnkeBasedOnKlagebehandlingView) {
        val validationErrors = mutableListOf<InvalidProperty>()

        if (input.mottattKlageinstans.isAfter(LocalDate.now())) {
            validationErrors += InvalidProperty(
                field = CreateAnkeBasedOnKlagebehandlingView::mottattKlageinstans.name,
                reason = "Dato kan ikke være i fremtiden"
            )
        }

        val sectionList = mutableListOf<ValidationSection>()

        if (validationErrors.isNotEmpty()) {
            sectionList.add(
                ValidationSection(
                    section = "saksdata",
                    properties = validationErrors
                )
            )

            throw SectionedValidationErrorWithDetailsException(
                title = "Validation error",
                sections = sectionList
            )
        }
    }

    fun validateCreateKlageInput(input: CreateKlageInput) {
        val validationErrors = mutableListOf<InvalidProperty>()

        if (input.mottattVedtaksinstans.isAfter(LocalDate.now())) {
            validationErrors += InvalidProperty(
                field = CreateKlageInput::mottattVedtaksinstans.name,
                reason = "Dato kan ikke være i fremtiden"
            )
        }

        if (input.mottattKlageinstans.isAfter(LocalDate.now())) {
            validationErrors += InvalidProperty(
                field = CreateKlageInput::mottattKlageinstans.name,
                reason = "Dato kan ikke være i fremtiden"
            )
        }

        if (input.mottattVedtaksinstans.isAfter(input.mottattKlageinstans)) {
            validationErrors += InvalidProperty(
                field = CreateKlageInput::mottattVedtaksinstans.name,
                reason = "Mottatt NAV kan ikke være etter mottatt KA"
            )
        }

        val sectionList = mutableListOf<ValidationSection>()

        if (validationErrors.isNotEmpty()) {
            sectionList.add(
                ValidationSection(
                    section = "saksdata",
                    properties = validationErrors
                )
            )

            throw SectionedValidationErrorWithDetailsException(
                title = "Validation error",
                sections = sectionList
            )
        }
    }
}