package no.nav.klage.util

import no.nav.klage.api.controller.view.SearchPartWithUtsendingskanalInput
import no.nav.klage.api.controller.view.Utsendingskanal
import no.nav.klage.clients.kabalapi.GosysOppgaveIsDuplicateInput
import no.nav.klage.clients.kabalapi.KabalApiClient
import no.nav.klage.domain.entities.HandlingEnum
import no.nav.klage.domain.entities.Mulighet
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.exceptions.InvalidProperty
import no.nav.klage.exceptions.SectionedValidationErrorWithDetailsException
import no.nav.klage.exceptions.ValidationSection
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.hjemmel.Hjemmel
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ValidationUtil(
    private val kabalApiClient: KabalApiClient
) {
    fun validateRegistrering(registrering: Registrering, mulighet: Mulighet) {
        val saksdataValidationErrors = mutableListOf<InvalidProperty>()
        val svarbrevValidationErrors = mutableListOf<InvalidProperty>()

        if (registrering.ytelse == null) {
            saksdataValidationErrors += InvalidProperty(
                field = Registrering::ytelse.name,
                reason = "Velg en ytelse."
            )
        }

        if (registrering.type == null) {
            saksdataValidationErrors += InvalidProperty(
                field = Registrering::type.name,
                reason = "Velg en type."
            )
        }

        if (mulighet.requiresGosysOppgave) {
            if (registrering.gosysOppgaveId == null) {
                saksdataValidationErrors += InvalidProperty(
                    field = Registrering::gosysOppgaveId.name,
                    reason = "Velg en Gosys-oppgave."
                )
            }
        }

        if (registrering.hjemmelIdList.isEmpty()) {
            saksdataValidationErrors += InvalidProperty(
                field = Registrering::hjemmelIdList.name,
                reason = "Velg minst én hjemmel."
            )
        }

        if (registrering.hjemmelIdList.isNotEmpty()) {
            try {
                registrering.hjemmelIdList.forEach { Hjemmel.of(it) }
            } catch (iae: IllegalArgumentException) {
                saksdataValidationErrors += InvalidProperty(
                    field = Registrering::hjemmelIdList.name,
                    reason = "Ugyldig hjemmel."
                )
            }
        }

        if (registrering.mottattKlageinstans == null) {
            saksdataValidationErrors += InvalidProperty(
                field = Registrering::mottattKlageinstans.name,
                reason = "Sett en dato."
            )
        } else if (registrering.mottattKlageinstans!!.isAfter(LocalDate.now())) {
            saksdataValidationErrors += InvalidProperty(
                field = Registrering::mottattKlageinstans.name,
                reason = "Sett en dato som ikke er i fremtiden."
            )
        }

        if (registrering.type == Type.KLAGE) {
            if (registrering.mottattVedtaksinstans == null) {
                saksdataValidationErrors += InvalidProperty(
                    field = Registrering::mottattVedtaksinstans.name,
                    reason = "Sett en dato."
                )
            } else if (registrering.mottattVedtaksinstans!!.isAfter(LocalDate.now())) {
                saksdataValidationErrors += InvalidProperty(
                    field = Registrering::mottattVedtaksinstans.name,
                    reason = "Sett en dato som ikke er i fremtiden."
                )
            }

            if (registrering.mottattVedtaksinstans != null && registrering.mottattKlageinstans != null && registrering.mottattVedtaksinstans!!.isAfter(
                    registrering.mottattKlageinstans
                )
            ) {
                saksdataValidationErrors += InvalidProperty(
                    field = Registrering::mottattVedtaksinstans.name,
                    reason = "Sett en dato som er før dato for mottatt Klageinstans."
                )
            }
        }

        if (registrering.klager == null) {
            val errorMessage = when (registrering.type) {
                Type.KLAGE -> "Velg en klager."
                Type.ANKE -> "Velg en ankende part."
                Type.OMGJOERINGSKRAV -> "Velg den som krever omgjøring."
                Type.BEGJAERING_OM_GJENOPPTAK -> "Velg den som begjærer gjenopptak."
                else -> error("Unsupported type")
            }

            saksdataValidationErrors += InvalidProperty(
                field = Registrering::klager.name,
                reason = errorMessage
            )
        }

        if (registrering.journalpostId == null) {
            saksdataValidationErrors += InvalidProperty(
                field = Registrering::journalpostId.name,
                reason = "Velg en journalpost."
            )
        }

        if (registrering.sendSvarbrev == true) {
            //Skal ikke inntreffe.
            if (!registrering.reasonNoLetter.isNullOrEmpty()) {
                svarbrevValidationErrors += InvalidProperty(
                    field = Registrering::reasonNoLetter.name,
                    reason = "Kan ikke oppgi grunn til manglende svarbrev når brev skal sendes."
                )
            }

            if (registrering.svarbrevReceivers.isEmpty()) {
                svarbrevValidationErrors += InvalidProperty(
                    field = Registrering::svarbrevReceivers.name,
                    reason = "Legg til minst én mottaker."
                )
            }

            //TODO: Validering på mottakere
            registrering.svarbrevReceivers.forEach { mottaker ->
                val part = kabalApiClient.searchPartWithUtsendingskanal(
                    searchPartInput = SearchPartWithUtsendingskanalInput(
                        identifikator = mottaker.part.value,
                        sakenGjelderId = registrering.sakenGjelder!!.value,
                        ytelseId = registrering.ytelse!!.id
                    )
                )

                if (documentWillGoToCentralPrint(
                    handling = mottaker.handling,
                    defaultUtsendingskanal = part.utsendingskanal
                    )
                ) {
                    if (mottaker.overriddenAddress == null && part.address == null) {
                        svarbrevValidationErrors += InvalidProperty(
                            field = Registrering::svarbrevReceivers.name,
                            reason = "Mottaker mangler gyldig addresse."
                        )
                    }
                }
            }
        }
        //TODO: Introduce after client changes.
//        else {
//            if (registrering.reasonNoLetter.isNullOrEmpty()) {
//                svarbrevValidationErrors += InvalidProperty(
//                    field = Registrering::reasonNoLetter.name,
//                    reason = "Oppgi hvorfor det ikke skal sendes noe svarbrev."
//                )
//            }
//        }

        if (registrering.gosysOppgaveId != null) {
            if (kabalApiClient.checkGosysOppgaveDuplicate(
                    input = GosysOppgaveIsDuplicateInput(
                        gosysOppgaveId = registrering.gosysOppgaveId!!
                    )
                )
            ) {
                saksdataValidationErrors += InvalidProperty(
                    field = Registrering::gosysOppgaveId.name,
                    reason = "Gosys-oppgaven er allerede i bruk i en åpen behandling i Kabal."
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
    }

    private fun documentWillGoToCentralPrint(
        handling: HandlingEnum,
        defaultUtsendingskanal: Utsendingskanal,
    ): Boolean {
        return handling == HandlingEnum.CENTRAL_PRINT ||
                (handling == HandlingEnum.AUTO && defaultUtsendingskanal == Utsendingskanal.SENTRAL_UTSKRIFT)
    }
}