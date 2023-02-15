package no.nav.klage.service

import no.nav.klage.api.controller.view.CreateAnkeBasedOnKlagebehandling
import no.nav.klage.api.controller.view.IdnummerInput
import no.nav.klage.api.controller.view.SearchPartInput
import no.nav.klage.clients.KabalApiClient
import no.nav.klage.exceptions.InvalidProperty
import no.nav.klage.exceptions.SectionedValidationErrorWithDetailsException
import no.nav.klage.exceptions.ValidationSection
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class KabalApiService(
    private val kabalApiClient: KabalApiClient
) {

    fun getCompletedKlagebehandlingerByIdnummer(idnummerInput: IdnummerInput): List<KabalApiClient.CompletedKlagebehandling> {
        return kabalApiClient.getCompletedKlagebehandlingerByIdnummer(idnummerInput)
    }

    fun getCompletedKlagebehandling(klagebehandlingId: UUID): KabalApiClient.CompletedKlagebehandling {
        return kabalApiClient.getCompletedKlagebehandling(klagebehandlingId)
    }

    fun createAnkeInKabal(input: CreateAnkeBasedOnKlagebehandling) {
        validate(input)
        kabalApiClient.createAnkeInKabal(input)
    }

    fun searchPart(searchPartInput: SearchPartInput): KabalApiClient.PartView {
        return kabalApiClient.searchPart(searchPartInput = searchPartInput)
    }

    private fun validate(input: CreateAnkeBasedOnKlagebehandling) {
        val validationErrors = mutableListOf<InvalidProperty>()

        if (input.mottattNav.isAfter(LocalDate.now())) {
            validationErrors += InvalidProperty(
                field = CreateAnkeBasedOnKlagebehandling::mottattNav.name,
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
}