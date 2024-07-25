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
import no.nav.klage.kodeverk.TimeUnitType
import no.nav.klage.kodeverk.hjemmel.Hjemmel
import no.nav.klage.service.KabalApiService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ValidationUtil(
    private val kabalApiService: KabalApiService
) {
    fun validateCreateAnkeInputView(input: CreateAnkeInputView): CreateAnkeInput {
        val saksdataValidationErrors = mutableListOf<InvalidProperty>()
        val svarbrevValidationErrors = mutableListOf<InvalidProperty>()

        if (input.vedtak == null) {
            saksdataValidationErrors += InvalidProperty(
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
                    saksdataValidationErrors += InvalidProperty(
                        field = CreateAnkeInputView::ytelseId.name,
                        reason = "Velg en ytelse."
                    )
                }
            }
        }

        if (input.hjemmelIdList.isEmpty()) {
            saksdataValidationErrors += InvalidProperty(
                field = CreateKlageInputView::hjemmelIdList.name,
                reason = "Velg minst én hjemmel."
            )
        }

        if (input.hjemmelIdList.isNotEmpty()) {
            try {
                input.hjemmelIdList.forEach { Hjemmel.of(it) }
            } catch (iae: IllegalArgumentException) {
                saksdataValidationErrors += InvalidProperty(
                    field = CreateKlageInputView::hjemmelIdList.name,
                    reason = "Ugyldig hjemmel."
                )
            }
        }

        if (input.mottattKlageinstans == null) {
            saksdataValidationErrors += InvalidProperty(
                field = CreateAnkeInputView::mottattKlageinstans.name,
                reason = "Sett en dato."
            )
        } else if (input.mottattKlageinstans.isAfter(LocalDate.now())) {
            saksdataValidationErrors += InvalidProperty(
                field = CreateAnkeInputView::mottattKlageinstans.name,
                reason = "Sett en dato som ikke er i fremtiden."
            )
        }

        if (!(input.behandlingstidUnits != null && (input.behandlingstidUnitType != null || input.behandlingstidUnitTypeId != null ))) {
            saksdataValidationErrors += InvalidProperty(
                field = CreateAnkeInputView::behandlingstidUnits.name,
                reason = "Sett en frist."
            )
        }

        if (input.klager == null) {
            saksdataValidationErrors += InvalidProperty(
                field = CreateAnkeInputView::klager.name,
                reason = "Velg en ankende part."
            )
        }

        if (input.journalpostId == null) {
            saksdataValidationErrors += InvalidProperty(
                field = CreateAnkeInputView::journalpostId.name,
                reason = "Velg en journalpost."
            )
        }

        if (input.svarbrevInput != null) {
            if (input.svarbrevInput.receivers.isEmpty()) {
                svarbrevValidationErrors += InvalidProperty(
                    field = CreateAnkeInputView::svarbrevInput.name,
                    reason = "Legg til minst én mottaker."
                )
            }
        }

        if (input.oppgaveId != null) {
            if (kabalApiService.oppgaveIsDuplicate(oppgaveId = input.oppgaveId)) {
                saksdataValidationErrors += InvalidProperty(
                    field = CreateAnkeInputView::oppgaveId.name,
                    reason = "Oppgaven er allerede i bruk i en åpen behandling i Kabal."
                )
            }
        }

        val sectionList = mutableListOf<ValidationSection>()

        if (saksdataValidationErrors.isNotEmpty()) {
            sectionList.add(
                ValidationSection(
                    section = "saksdata",
                    properties = saksdataValidationErrors
                )
            )
        }

        if (svarbrevValidationErrors.isNotEmpty()) {
            sectionList.add(
                ValidationSection(
                    section = "svarbrev",
                    properties = svarbrevValidationErrors
                )
            )
        }

        if (sectionList.isNotEmpty()) {
            throw SectionedValidationErrorWithDetailsException(
                title = "Validation error",
                sections = sectionList
            )
        }

        return CreateAnkeInput(
            id = input.vedtak!!.id,
            mottattKlageinstans = input.mottattKlageinstans!!,
            behandlingstidUnits = input.behandlingstidUnits!!,
            behandlingstidUnitType = getTimeUnitType(
                behandlingstidUnitTypeId = input.behandlingstidUnitTypeId,
                behandlingstidUnitType = input.behandlingstidUnitType,
            ),
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
        val saksdataValidationErrors = mutableListOf<InvalidProperty>()
        val svarbrevValidationErrors = mutableListOf<InvalidProperty>()

        if (input.vedtak == null) {
            saksdataValidationErrors += InvalidProperty(
                field = CreateKlageInputView::vedtak.name,
                reason = "Velg et vedtak."
            )
        }

        if (input.mottattKlageinstans == null) {
            saksdataValidationErrors += InvalidProperty(
                field = CreateKlageInputView::mottattKlageinstans.name,
                reason = "Sett en dato."
            )
        } else {
            if (input.mottattKlageinstans.isAfter(LocalDate.now())) {
                saksdataValidationErrors += InvalidProperty(
                    field = CreateKlageInputView::mottattKlageinstans.name,
                    reason = "Sett en dato som ikke er i fremtiden."
                )
            }
        }

        if (input.mottattVedtaksinstans == null) {
            saksdataValidationErrors += InvalidProperty(
                field = CreateKlageInputView::mottattVedtaksinstans.name,
                reason = "Sett en dato."
            )
        } else if (input.mottattVedtaksinstans.isAfter(LocalDate.now())) {
            saksdataValidationErrors += InvalidProperty(
                field = CreateKlageInputView::mottattVedtaksinstans.name,
                reason = "Sett en dato som ikke er i fremtiden."
            )
        }

        if (input.mottattVedtaksinstans != null && input.mottattKlageinstans != null && input.mottattVedtaksinstans.isAfter(
                input.mottattKlageinstans
            )
        ) {
            saksdataValidationErrors += InvalidProperty(
                field = CreateKlageInputView::mottattVedtaksinstans.name,
                reason = "Sett en dato som er før dato for mottatt Klageinstans."
            )
        }

        if (!(input.behandlingstidUnits != null && (input.behandlingstidUnitType != null || input.behandlingstidUnitTypeId != null ))) {
            saksdataValidationErrors += InvalidProperty(
                field = CreateAnkeInputView::behandlingstidUnits.name,
                reason = "Sett en frist."
            )
        }

        if (input.klager == null) {
            saksdataValidationErrors += InvalidProperty(
                field = CreateKlageInputView::klager.name,
                reason = "Velg en klager."
            )
        }

        if (input.journalpostId == null) {
            saksdataValidationErrors += InvalidProperty(
                field = CreateKlageInputView::journalpostId.name,
                reason = "Velg en journalpost."
            )
        }

        if (input.ytelseId == null) {
            saksdataValidationErrors += InvalidProperty(
                field = CreateKlageInputView::ytelseId.name,
                reason = "Velg en ytelse."
            )
        }

        if (input.hjemmelIdList.isEmpty()) {
            saksdataValidationErrors += InvalidProperty(
                field = CreateKlageInputView::hjemmelIdList.name,
                reason = "Velg minst én hjemmel."
            )
        } else {
            try {
                input.hjemmelIdList.forEach { Hjemmel.of(it) }
            } catch (iae: IllegalArgumentException) {
                saksdataValidationErrors += InvalidProperty(
                    field = CreateKlageInputView::hjemmelIdList.name,
                    reason = "Ugyldig hjemmel."
                )
            }
        }

        if (input.oppgaveId != null) {
            if (kabalApiService.oppgaveIsDuplicate(oppgaveId = input.oppgaveId)) {
                saksdataValidationErrors += InvalidProperty(
                    field = CreateAnkeInputView::oppgaveId.name,
                    reason = "Oppgaven er allerede i bruk i en åpen behandling i Kabal."
                )
            }
        }

        if (input.svarbrevInput != null) {
            if (input.svarbrevInput.receivers.isEmpty()) {
                svarbrevValidationErrors += InvalidProperty(
                    field = CreateKlageInputView::svarbrevInput.name,
                    reason = "Legg til minst én mottaker."
                )
            }
        }

        val sectionList = mutableListOf<ValidationSection>()

        if (saksdataValidationErrors.isNotEmpty()) {
            sectionList.add(
                ValidationSection(
                    section = "saksdata",
                    properties = saksdataValidationErrors
                )
            )
        }

        if (svarbrevValidationErrors.isNotEmpty()) {
            sectionList.add(
                ValidationSection(
                    section = "svarbrev",
                    properties = svarbrevValidationErrors
                )
            )
        }

        if (sectionList.isNotEmpty()) {
            throw SectionedValidationErrorWithDetailsException(
                title = "Validation error",
                sections = sectionList
            )
        }

        return CreateKlageInput(
            eksternBehandlingId = input.vedtak!!.id,
            mottattVedtaksinstans = input.mottattVedtaksinstans!!,
            mottattKlageinstans = input.mottattKlageinstans!!,
            behandlingstidUnits = input.behandlingstidUnits!!,
            behandlingstidUnitType = getTimeUnitType(
                behandlingstidUnitTypeId = input.behandlingstidUnitTypeId,
                behandlingstidUnitType = input.behandlingstidUnitType,
            ),
            klager = input.klager!!,
            fullmektig = input.fullmektig,
            klageJournalpostId = input.journalpostId!!,
            ytelseId = input.ytelseId!!,
            hjemmelIdList = input.hjemmelIdList,
            avsender = input.avsender,
            saksbehandlerIdent = input.saksbehandlerIdent,
            oppgaveId = input.oppgaveId,
            svarbrevInput = input.svarbrevInput,
        )
    }

    private fun getTimeUnitType(
        behandlingstidUnitTypeId: String?,
        behandlingstidUnitType: TimeUnitType?
    ): TimeUnitType {
        return if (behandlingstidUnitTypeId != null) {
            TimeUnitType.of(behandlingstidUnitTypeId)
        } else {
            behandlingstidUnitType!!
        }
    }
}