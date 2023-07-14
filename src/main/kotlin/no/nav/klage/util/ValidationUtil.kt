package no.nav.klage.util

import no.nav.klage.api.controller.view.*
import no.nav.klage.exceptions.InvalidProperty
import no.nav.klage.exceptions.SectionedValidationErrorWithDetailsException
import no.nav.klage.exceptions.ValidationSection
import no.nav.klage.kodeverk.Ytelse
import no.nav.klage.kodeverk.hjemmel.Hjemmel
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ValidationUtil {
    fun validateCreateAnkeInputView(input: CreateAnkeInputView): CreateAnkeInput {
        val validationErrors = mutableListOf<InvalidProperty>()

        if (input.behandlingId == null && input.eksternBehandlingId == null) {
            validationErrors += InvalidProperty(
                field = CreateAnkeInputView::behandlingId.name,
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

        if (input.ytelseId == null) {
            validationErrors += InvalidProperty(
                field = CreateAnkeInputView::ytelseId.name,
                reason = "Velg en ytelse."
            )
        } else {
            if (Ytelse.values().firstOrNull { it.id == input.ytelseId } == null) {
                validationErrors += InvalidProperty(
                    field = CreateAnkeInputView::ytelseId.name,
                    reason = "Ugyldig ytelse."
                )
            }
        }

        if (input.hjemmelId.isNullOrEmpty()) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::hjemmelId.name,
                reason = "Velg minst én hjemmel."
            )
        } else {
            if (Hjemmel.values().firstOrNull { it.id == input.hjemmelId } == null) {
                validationErrors += InvalidProperty(
                    field = CreateKlageInputView::hjemmelId.name,
                    reason = "Hjemmel med id ${input.hjemmelId} er ugyldig."
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
            klagebehandlingId = input.behandlingId,
            eksternBehandlingId = input.eksternBehandlingId,
            mottattKlageinstans = input.mottattKlageinstans!!,
            fristInWeeks = input.fristInWeeks!!,
            klager = input.klager!!,
            fullmektig = input.fullmektig,
            ankeDocumentJournalpostId = input.journalpostId!!,
            ytelseId = input.ytelseId!!,
            hjemmelId = input.hjemmelId!!,
            avsender = input.avsender,
            saksbehandlerIdent = input.saksbehandlerIdent
        )
    }

    fun validateCreateKlageInputView(input: CreateKlageInputView): CreateKlageInput {
        val validationErrors = mutableListOf<InvalidProperty>()

        if (input.behandlingId == null && input.eksternBehandlingId == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::behandlingId.name,
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
        } else {
            if (Ytelse.values().firstOrNull { it.id == input.ytelseId } == null) {
                validationErrors += InvalidProperty(
                    field = CreateKlageInputView::ytelseId.name,
                    reason = "Ugyldig ytelse."
                )
            }
        }

        if (input.hjemmelIdList.isNullOrEmpty() && input.hjemmelId == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::hjemmelId.name,
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
            eksternBehandlingId = input.eksternBehandlingId ?: input.behandlingId!!,
            mottattVedtaksinstans = input.mottattVedtaksinstans!!,
            mottattKlageinstans = input.mottattKlageinstans!!,
            fristInWeeks = input.fristInWeeks!!,
            klager = input.klager!!,
            fullmektig = input.fullmektig,
            klageJournalpostId = input.journalpostId!!,
            ytelseId = input.ytelseId!!,
            hjemmelId = input.hjemmelId ?: input.hjemmelIdList!!.first(),
            avsender = input.avsender,
            saksbehandlerIdent = input.saksbehandlerIdent,
        )
    }
}