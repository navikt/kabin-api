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

        if (input.klagebehandlingId == null && input.behandlingId == null) {
            validationErrors += InvalidProperty(
                field = CreateAnkeInputView::behandlingId.name,
                reason = "Velg et vedtak."
            )
        }

        if (input.mottattKlageinstans == null) {
            validationErrors += InvalidProperty(
                field = CreateAnkeInputView::mottattKlageinstans.name,
                reason = "Dato for mottatt klageinstans må være satt."
            )
        } else if (input.mottattKlageinstans.isAfter(LocalDate.now())) {
            validationErrors += InvalidProperty(
                field = CreateAnkeInputView::mottattKlageinstans.name,
                reason = "Dato kan ikke være i fremtiden."
            )
        }

        if (input.fristInWeeks == null) {
            validationErrors += InvalidProperty(
                field = CreateAnkeInputView::fristInWeeks.name,
                reason = "Frist i uker må være satt."
            )
        }

        if (input.klager == null) {
            validationErrors += InvalidProperty(
                field = CreateAnkeInputView::klager.name,
                reason = "Klager må være satt."
            )
        }

        if (input.ankeDocumentJournalpostId == null && input.journalpostId == null) {
            validationErrors += InvalidProperty(
                field = CreateAnkeInputView::ankeDocumentJournalpostId.name,
                reason = "Journalpost må være valgt."
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

        return CreateAnkeInput(
            klagebehandlingId = input.behandlingId ?: input.klagebehandlingId!!,
            mottattKlageinstans = input.mottattKlageinstans!!,
            fristInWeeks = input.fristInWeeks!!,
            klager = input.klager!!,
            fullmektig = input.fullmektig,
            ankeDocumentJournalpostId = input.journalpostId ?: input.ankeDocumentJournalpostId!!,
            avsender = input.avsender
        )
    }

    fun validateCreateKlageInputView(input: CreateKlageInputView): CreateKlageInput {
        val validationErrors = mutableListOf<InvalidProperty>()

        if (input.sakId == null && input.behandlingId == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::behandlingId.name,
                reason = "Velg et vedtak."
            )
        }

        if (input.mottattKlageinstans == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::mottattKlageinstans.name,
                reason = "Dato for mottatt klageinstans må være satt."
            )
        } else {
            if (input.mottattKlageinstans.isAfter(LocalDate.now())) {
                validationErrors += InvalidProperty(
                    field = CreateKlageInputView::mottattKlageinstans.name,
                    reason = "Dato kan ikke være i fremtiden."
                )
            }
        }

        if (input.mottattVedtaksinstans == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::mottattVedtaksinstans.name,
                reason = "Dato for mottatt vedtaksInstans må være satt."
            )
        } else if (input.mottattVedtaksinstans.isAfter(LocalDate.now())) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::mottattVedtaksinstans.name,
                reason = "Dato kan ikke være i fremtiden."
            )
        }

        if (input.mottattVedtaksinstans != null && input.mottattKlageinstans != null && input.mottattVedtaksinstans.isAfter(input.mottattKlageinstans)) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::mottattVedtaksinstans.name,
                reason = "Mottatt NAV kan ikke være etter mottatt KA."
            )
        }

        if (input.fristInWeeks == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::fristInWeeks.name,
                reason = "Frist i uker må være satt."
            )
        }

        if (input.klager == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::klager.name,
                reason = "Klager må være satt."
            )
        }

        if (input.klageJournalpostId == null && input.journalpostId == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::journalpostId.name,
                reason = "Journalpost må være valgt."
            )
        }

        if (input.ytelseId == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::ytelseId.name,
                reason = "Ytelse må være valgt."
            )
        } else {
            if (Ytelse.values().firstOrNull { it.id == input.ytelseId } == null) {
                validationErrors += InvalidProperty(
                    field = CreateKlageInputView::ytelseId.name,
                    reason = "Ugyldig ytelse."
                )
            }
        }

        if (input.hjemmelIdList.isNullOrEmpty()) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::hjemmelIdList.name,
                reason = "Hjemmellisten kan ikke være tom."
            )
        } else {
            input.hjemmelIdList.map{ hjemmelId ->
                if (Hjemmel.values().firstOrNull { it.id == hjemmelId } == null) {
                    validationErrors += InvalidProperty(
                        field = CreateKlageInputView::hjemmelIdList.name,
                        reason = "Hjemmel med id $hjemmelId er ugyldig."
                    )
                }
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

        return CreateKlageInput(
            sakId = input.behandlingId ?: input.sakId!!,
            mottattVedtaksinstans = input.mottattVedtaksinstans!!,
            mottattKlageinstans = input.mottattKlageinstans!!,
            fristInWeeks = input.fristInWeeks!!,
            klager = input.klager!!,
            fullmektig = input.fullmektig,
            klageJournalpostId = input.journalpostId ?: input.klageJournalpostId!!,
            ytelseId = input.ytelseId!!,
            hjemmelIdList = input.hjemmelIdList!!,
            avsender = input.avsender
        )
    }
}