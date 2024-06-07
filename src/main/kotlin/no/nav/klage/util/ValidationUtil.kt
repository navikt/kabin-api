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
import no.nav.klage.kodeverk.hjemmel.Hjemmel
import no.nav.klage.service.KabalApiService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ValidationUtil(
    private val kabalApiService: KabalApiService
) {
    fun validateCreateAnkeInputView(input: CreateAnkeInputView): CreateAnkeInput {
        val validationErrors = mutableListOf<InvalidProperty>()

        if (input.vedtak == null) {
            validationErrors += InvalidProperty(
                field = CreateAnkeInputView::vedtak.name,
                reason = "Velg en mulighet/vedtak."
            )
        } else {
            val mulighetSource =
                try {
                    MulighetSource.of(Fagsystem.of(input.vedtak.sourceId))
                } catch (exception: Exception) {
                    throw InvalidSourceException(
                        message = "Ugyldig sourceId."
                    )
                }
            if (mulighetSource == MulighetSource.INFOTRYGD) {
                if (input.ytelseId == null) {
                    validationErrors += InvalidProperty(
                        field = CreateAnkeInputView::ytelseId.name,
                        reason = "Velg en ytelse."
                    )
                }
            }
        }

        if (input.hjemmelIdList.isEmpty()) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::hjemmelIdList.name,
                reason = "Velg minst én hjemmel."
            )
        }

        if (input.hjemmelIdList.isNotEmpty()) {
            try {
                input.hjemmelIdList.forEach { Hjemmel.of(it) }
            } catch (iae: IllegalArgumentException) {
                validationErrors += InvalidProperty(
                    field = CreateKlageInputView::hjemmelIdList.name,
                    reason = "Ugyldig hjemmel."
                )
            }
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

        if (input.svarbrevInput != null) {
            if (input.svarbrevInput.receivers.isEmpty()) {
                validationErrors += InvalidProperty(
                    field = CreateAnkeInputView::svarbrevInput.name,
                    reason = "Legg til minst én mottaker."
                )
            }
        }

        if (input.oppgaveId != null) {
            if (kabalApiService.oppgaveIsDuplicate(oppgaveId = input.oppgaveId)) {
                validationErrors += InvalidProperty(
                    field = CreateAnkeInputView::oppgaveId.name,
                    reason = "Oppgaven er allerede i bruk i en åpen behandling i Kabal."
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
            id = input.vedtak!!.id,
            mottattKlageinstans = input.mottattKlageinstans!!,
            fristInWeeks = input.fristInWeeks!!,
            klager = input.klager!!,
            fullmektig = input.fullmektig,
            ankeDocumentJournalpostId = input.journalpostId!!,
            ytelseId = input.ytelseId,
            hjemmelIdList = input.hjemmelIdList,
            avsender = input.avsender,
            saksbehandlerIdent = input.saksbehandlerIdent,
            mulighetSource = MulighetSource.of(Fagsystem.of(input.vedtak.sourceId)),
            svarbrevInput = input.svarbrevInput,
            oppgaveId = input.oppgaveId,
        )
    }

    fun validateCreateKlageInputView(input: CreateKlageInputView): CreateKlageInput {
        val validationErrors = mutableListOf<InvalidProperty>()

        if (input.vedtak == null) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::vedtak.name,
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

        if (input.hjemmelIdList.isEmpty()) {
            validationErrors += InvalidProperty(
                field = CreateKlageInputView::hjemmelIdList.name,
                reason = "Velg minst én hjemmel."
            )
        } else {
            try {
                input.hjemmelIdList.forEach { Hjemmel.of(it) }
            } catch (iae: IllegalArgumentException) {
                validationErrors += InvalidProperty(
                    field = CreateKlageInputView::hjemmelIdList.name,
                    reason = "Ugyldig hjemmel."
                )
            }
        }

        if (input.oppgaveId != null) {
            if (kabalApiService.oppgaveIsDuplicate(oppgaveId = input.oppgaveId)) {
                validationErrors += InvalidProperty(
                    field = CreateAnkeInputView::oppgaveId.name,
                    reason = "Oppgaven er allerede i bruk i en åpen behandling i Kabal."
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

        return CreateKlageInput(
            eksternBehandlingId = input.vedtak!!.id,
            mottattVedtaksinstans = input.mottattVedtaksinstans!!,
            mottattKlageinstans = input.mottattKlageinstans!!,
            fristInWeeks = input.fristInWeeks!!,
            klager = input.klager!!,
            fullmektig = input.fullmektig,
            klageJournalpostId = input.journalpostId!!,
            ytelseId = input.ytelseId!!,
            hjemmelIdList = input.hjemmelIdList,
            avsender = input.avsender,
            saksbehandlerIdent = input.saksbehandlerIdent,
            oppgaveId = input.oppgaveId
        )
    }
}