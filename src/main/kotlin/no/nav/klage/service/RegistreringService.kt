package no.nav.klage.service

import no.nav.klage.api.controller.view.JournalpostIdInput
import no.nav.klage.api.controller.view.RegistreringView
import no.nav.klage.api.controller.view.SearchPartWithUtsendingskanalInput
import no.nav.klage.clients.kabalapi.KabalApiClient
import no.nav.klage.domain.entities.PartId
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.exceptions.RegistreringNotFoundException
import no.nav.klage.kodeverk.PartIdType
import no.nav.klage.repository.RegistreringRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class RegistreringService(
    private val registreringRepository: RegistreringRepository,
    private val kabalApiClient: KabalApiClient,
) {

    fun createRegistrering(
        sakenGjelderValue: String,
        createdBy: String,
    ) {
        registreringRepository.save(
            Registrering(
                sakenGjelder = PartId(
                    value = sakenGjelderValue,
                    type = PartIdType.PERSON,
                ),
                createdBy = createdBy,
                //defaults
                klager = null,
                fullmektig = null,
                avsender = null,
                journalpostId = null,
                type = null,
                mulighetId = null,
                mulighetFagsystem = null,
                mottattVedtaksinstans = null,
                mottattKlageinstans = null,
                behandlingstidUnits = null,
                behandlingstidUnitType = null,
                hjemmelIdList = listOf(),
                ytelse = null,
                saksbehandlerIdent = null,
                oppgaveId = null,
                sendSvarbrev = null,
                svarbrevTitle = null,
                svarbrevCustomText = null,
                svarbrevBehandlingstidUnits = null,
                svarbrevBehandlingstidUnitType = null,
                svarbrevFullmektigFritekst = null,
                svarbrevReceivers = mutableSetOf(),
                finished = null,
            )
        )
    }

    fun getRegistrering(registreringId: UUID): RegistreringView {
        return registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .toRegistreringView()
    }

    fun getRegistreringer(
        navIdent: String,
        fullfoert: Boolean,
        sidenDager: Int?,
    ): List<RegistreringView> {
        //TODO: Implement filtering
        return registreringRepository.findAll().map { it.toRegistreringView() }
    }

    fun setJournalpostId(registreringId: UUID, input: JournalpostIdInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                journalpostId = input.journalpostId
            }
    }

    private fun Registrering.toRegistreringView() = RegistreringView(
        id = id,
        journalpostId = journalpostId,
        sakenGjelderValue = sakenGjelder.value,
        typeId = type?.id,
        mulighet = if (mulighetId != null) {
            RegistreringView.MulighetView(
                id = mulighetId!!,
                fagsystemId = mulighetFagsystem!!.id
            )
        } else null,
        overstyringer = RegistreringView.OverstyringerView(
            mottattVedtaksinstans = mottattVedtaksinstans?.toString(),
            mottattKlageinstans = mottattKlageinstans?.toString(),
            behandlingstid = if (behandlingstidUnits != null) {
                RegistreringView.BehandlingstidView(
                    unitTypeId = behandlingstidUnitType!!.id,
                    units = behandlingstidUnits!!
                )
            } else null,
            hjemmelIdList = hjemmelIdList,
            ytelseId = ytelse?.id,
            fullmektig = partViewWithUtsendingskanal(identifikator = fullmektig?.value),
            klager = partViewWithUtsendingskanal(identifikator = klager?.value),
            avsender = partViewWithUtsendingskanal(identifikator = avsender?.value),
            saksbehandlerIdent = saksbehandlerIdent,
            oppgaveId = oppgaveId,
        ),
        svarbrev = RegistreringView.SvarbrevView(
            send = sendSvarbrev,
            behandlingstid = if (svarbrevBehandlingstidUnits != null) {
                RegistreringView.BehandlingstidView(
                    unitTypeId = svarbrevBehandlingstidUnitType!!.id,
                    units = svarbrevBehandlingstidUnits!!
                )
            } else null,
            fullmektigFritekst = svarbrevFullmektigFritekst,
            receivers = svarbrevReceivers.map { receiver ->
                RegistreringView.SvarbrevView.RecipientView(
                    part = partViewWithUtsendingskanal(identifikator = receiver.part!!.value)!!,
                    handling = receiver.handling,
                    overriddenAddress = receiver.overriddenAddress?.let { address ->
                        RegistreringView.SvarbrevView.RecipientView.AddressView(
                            adresselinje1 = address.adresselinje1,
                            adresselinje2 = address.adresselinje2,
                            adresselinje3 = address.adresselinje3,
                            landkode = address.landkode,
                            postnummer = address.postnummer,
                        )
                    }
                )
            },
            title = svarbrevTitle,
            customText = svarbrevCustomText,
        ),
        created = created,
        modified = modified,
        createdBy = createdBy,
    )

    private fun Registrering.partViewWithUtsendingskanal(identifikator: String?) =
        if (identifikator != null && ytelse != null) {
            kabalApiClient.searchPartWithUtsendingskanal(
                searchPartInput = SearchPartWithUtsendingskanalInput(
                    identifikator = identifikator,
                    sakenGjelderId = sakenGjelder.value,
                    ytelseId = ytelse!!.id
                )
            ).partViewWithUtsendingskanal()
        } else null

}