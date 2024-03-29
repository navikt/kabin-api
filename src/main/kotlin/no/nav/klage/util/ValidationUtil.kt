package no.nav.klage.util

import no.nav.klage.api.controller.view.CreateAnkeInputView
import no.nav.klage.api.controller.view.CreateKlageInputView
import no.nav.klage.domain.CreateAnkeInput
import no.nav.klage.domain.CreateKlageInput
import no.nav.klage.exceptions.InvalidProperty
import no.nav.klage.exceptions.InvalidSourceException
import no.nav.klage.exceptions.SectionedValidationErrorWithDetailsException
import no.nav.klage.exceptions.ValidationSection
import no.nav.klage.kodeverk.Fagsystem
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ValidationUtil {
    fun validateCreateAnkeInputView(input: CreateAnkeInputView): CreateAnkeInput {
        val validationErrors = mutableListOf<InvalidProperty>()

        val ankemulighetSource =
            try {
                AnkemulighetSource.of(Fagsystem.of(input.sourceId))
            } catch (exception: Exception) {
                throw InvalidSourceException(
                    message = "Ugyldig sourceId."
                )
            }

        if (input.id == null) {
            validationErrors += InvalidProperty(
                field = CreateAnkeInputView::id.name,
                reason = "Velg et vedtak."
            )
        }

        if (input.mottattKlageinstans == null) {
            validationErrors += InvalidProperty(
                field = CreateAnkeInputView::mottattKlageinstans.name,
                reason = "Sett en dato."
            )
        } else if (input.mottattKlageinstans.isAfter(LocalDate.now())) {
            validationErrors += InvalidProperty(
                field = CreateAnkeInputView::mottattKlageinstans.name,
                reason = "Sett en dato som ikke er i fremtiden."
            )
        }

        if (input.fristInWeeks == null) {
            validationErrors += InvalidProperty(
                field = CreateAnkeInputView::fristInWeeks.name,
                reason = "Sett en frist."
            )
        }

        if (input.klager == null) {
            validationErrors += InvalidProperty(
                field = CreateAnkeInputView::klager.name,
                reason = "Velg en klager."
            )
        }

        if (input.journalpostId == null) {
            validationErrors += InvalidProperty(
                field = CreateAnkeInputView::journalpostId.name,
                reason = "Velg en journalpost."
            )
        }

        if (ankemulighetSource == AnkemulighetSource.INFOTRYGD) {
            if (input.hjemmelIdList == null) {
                validationErrors += InvalidProperty(
                    field = CreateKlageInputView::hjemmelIdList.name,
                    reason = "Velg minst én hjemmel."
                )
            }

            if (input.ytelseId == null) {
                validationErrors += InvalidProperty(
                    field = CreateAnkeInputView::ytelseId.name,
                    reason = "Velg en ytelse."
                )
            }
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

        return CreateAnkeInput(
            id = input.id!!,
            mottattKlageinstans = input.mottattKlageinstans!!,
            fristInWeeks = input.fristInWeeks!!,
            klager = input.klager!!,
            fullmektig = input.fullmektig,
            ankeDocumentJournalpostId = input.journalpostId!!,
            ytelseId = input.ytelseId,
            hjemmelIdList = input.hjemmelIdList,
            avsender = input.avsender,
            saksbehandlerIdent = input.saksbehandlerIdent,
            ankemulighetSource = ankemulighetSource,
        )
    }

    fun validateCreateKlageInputView(input: CreateKlageInputView): CreateKlageInput {
        val validationErrors = mutableListOf<InvalidProperty>()

        if (input.id == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::id.name,
                reason = "Velg et vedtak."
            )
        }

        if (input.mottattKlageinstans == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::mottattKlageinstans.name,
                reason = "Sett en dato."
            )
        } else {
            if (input.mottattKlageinstans.isAfter(LocalDate.now())) {
                validationErrors += InvalidProperty(
                    field = CreateKlageInputView::mottattKlageinstans.name,
                    reason = "Sett en dato som ikke er i fremtiden."
                )
            }
        }

        if (input.mottattVedtaksinstans == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::mottattVedtaksinstans.name,
                reason = "Sett en dato."
            )
        } else if (input.mottattVedtaksinstans.isAfter(LocalDate.now())) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::mottattVedtaksinstans.name,
                reason = "Sett en dato som ikke er i fremtiden."
            )
        }

        if (input.mottattVedtaksinstans != null && input.mottattKlageinstans != null && input.mottattVedtaksinstans.isAfter(
                input.mottattKlageinstans
            )
        ) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::mottattVedtaksinstans.name,
                reason = "Sett en dato som er før dato for mottatt Klageinstans."
            )
        }

        if (input.fristInWeeks == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::fristInWeeks.name,
                reason = "Sett en frist."
            )
        }

        if (input.klager == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::klager.name,
                reason = "Velg en klager."
            )
        }

        if (input.journalpostId == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::journalpostId.name,
                reason = "Velg en journalpost."
            )
        }

        if (input.ytelseId == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::ytelseId.name,
                reason = "Velg en ytelse."
            )
        }

        if (input.hjemmelIdList == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::hjemmelIdList.name,
                reason = "Velg minst én hjemmel."
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

        return CreateKlageInput(
            eksternBehandlingId = input.id!!,
            mottattVedtaksinstans = input.mottattVedtaksinstans!!,
            mottattKlageinstans = input.mottattKlageinstans!!,
            fristInWeeks = input.fristInWeeks!!,
            klager = input.klager!!,
            fullmektig = input.fullmektig,
            klageJournalpostId = input.journalpostId!!,
            ytelseId = input.ytelseId!!,
            hjemmelIdList = input.hjemmelIdList!!,
            avsender = input.avsender,
            saksbehandlerIdent = input.saksbehandlerIdent,
        )
    }
}