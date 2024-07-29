package no.nav.klage.service

import no.nav.klage.api.controller.view.*
import no.nav.klage.clients.kabalapi.KabalApiClient
import no.nav.klage.domain.entities.PartId
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.domain.entities.SvarbrevReceiver
import no.nav.klage.exceptions.RegistreringNotFoundException
import no.nav.klage.kodeverk.*
import no.nav.klage.repository.RegistreringRepository
import no.nav.klage.util.TokenUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class RegistreringService(
    private val registreringRepository: RegistreringRepository,
    private val kabalApiClient: KabalApiClient,
    private val tokenUtil: TokenUtil,
) {

    fun createRegistrering() {
        registreringRepository.save(
            Registrering(
                createdBy = tokenUtil.getCurrentIdent(),
                //defaults
                sakenGjelder = null,
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

    fun setSakenGjelderValue(registreringId: UUID, input: SakenGjelderValueInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                sakenGjelder = input.sakenGjelderValue?.let { sakenGjelderValue ->
                    PartId(
                        value = sakenGjelderValue,
                        type = PartIdType.PERSON
                    )
                }
            }
    }

    fun setJournalpostId(registreringId: UUID, input: JournalpostIdInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                journalpostId = input.journalpostId
            }
    }

    fun setTypeId(registreringId: UUID, input: TypeIdInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                type = input.typeId?.let { typeId ->
                    Type.of(typeId)
                }
            }
    }

    fun setMulighet(registreringId: UUID, input: MulighetInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                mulighetId = input.mulighetId
                mulighetFagsystem = Fagsystem.of(input.fagsystemId)
            }
    }

    fun setMottattVedtaksinstans(registreringId: UUID, input: MottattVedtaksinstansInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                mottattVedtaksinstans = input.mottattVedtaksinstans
            }
    }

    fun setMottattKlageinstans(registreringId: UUID, input: MottattKlageinstansInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                mottattKlageinstans = input.mottattKlageinstans
            }
    }

    fun setBehandlingstid(registreringId: UUID, input: BehandlingstidInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                behandlingstidUnits = input.units
                behandlingstidUnitType = TimeUnitType.of(input.unitTypeId)
            }
    }

    fun setHjemmelIdList(registreringId: UUID, input: HjemmelIdListInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                hjemmelIdList = input.hjemmelIdList
            }
    }

    fun setYtelseId(registreringId: UUID, input: YtelseIdInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                ytelse = input.ytelseId?.let { ytelseId ->
                    Ytelse.of(ytelseId)
                }
            }
    }

    fun setFullmektig(registreringId: UUID, input: PartIdInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                fullmektig = PartId(
                    value = input.id,
                    type = when (input.type) {
                        PartType.FNR -> {
                            PartIdType.PERSON
                        }

                        PartType.ORGNR -> {
                            PartIdType.VIRKSOMHET
                        }
                    }
                )
            }
    }

    fun setKlager(registreringId: UUID, input: PartIdInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                klager = PartId(
                    value = input.id,
                    type = when (input.type) {
                        PartType.FNR -> {
                            PartIdType.PERSON
                        }

                        PartType.ORGNR -> {
                            PartIdType.VIRKSOMHET
                        }
                    }
                )
            }
    }

    fun setAvsender(registreringId: UUID, input: PartIdInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                avsender = PartId(
                    value = input.id,
                    type = when (input.type) {
                        PartType.FNR -> {
                            PartIdType.PERSON
                        }

                        PartType.ORGNR -> {
                            PartIdType.VIRKSOMHET
                        }
                    }
                )
            }
    }

    fun setSaksbehandlerIdent(registreringId: UUID, input: SaksbehandlerIdentInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                saksbehandlerIdent = input.saksbehandlerIdent
            }
    }

    fun setOppgaveId(registreringId: UUID, input: OppgaveIdInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                oppgaveId = input.oppgaveId
            }
    }

    fun setSendSvarbrev(registreringId: UUID, input: SendSvarbrevInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                sendSvarbrev = input.send
            }
    }

    fun setSvarbrevTitle(registreringId: UUID, input: SvarbrevTitleInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                svarbrevTitle = input.title
            }
    }

    fun setSvarbrevCustomText(registreringId: UUID, input: SvarbrevCustomTextInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                svarbrevCustomText = input.customText
            }
    }

    fun setSvarbrevBehandlingstid(registreringId: UUID, input: BehandlingstidInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                svarbrevBehandlingstidUnits = input.units
                svarbrevBehandlingstidUnitType = TimeUnitType.of(input.unitTypeId)
            }
    }

    fun setSvarbrevFullmektigFritekst(registreringId: UUID, input: SvarbrevFullmektigFritekstInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                svarbrevFullmektigFritekst = input.fullmektigFritekst
            }
    }

    fun setSvarbrevReceivers(registreringId: UUID, input: SvarbrevReceiversInput) {
        registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering not found") }
            .apply {
                svarbrevReceivers.clear()
                svarbrevReceivers.addAll(input.receivers.map { receiver ->
                    SvarbrevReceiver(
                        part = PartId(
                            value = receiver.part.id,
                            type = when (receiver.part.type) {
                                PartType.FNR -> {
                                    PartIdType.PERSON
                                }

                                PartType.ORGNR -> {
                                    PartIdType.VIRKSOMHET
                                }
                            }
                        ),
                        handling = receiver.handling,
                        overriddenAddress = receiver.overriddenAddress?.let { address ->
                            no.nav.klage.domain.entities.Address(
                                adresselinje1 = address.adresselinje1,
                                adresselinje2 = address.adresselinje2,
                                adresselinje3 = address.adresselinje3,
                                landkode = address.landkode,
                                postnummer = address.postnummer,
                            )
                        }
                    )
                })
            }
    }

    private fun Registrering.toRegistreringView() = RegistreringView(
        id = id,
        journalpostId = journalpostId,
        sakenGjelderValue = sakenGjelder?.value,
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
        finished = finished,
    )

    private fun Registrering.partViewWithUtsendingskanal(identifikator: String?) =
        if (identifikator != null && ytelse != null) {
            kabalApiClient.searchPartWithUtsendingskanal(
                searchPartInput = SearchPartWithUtsendingskanalInput(
                    identifikator = identifikator,
                    sakenGjelderId = sakenGjelder!!.value,
                    ytelseId = ytelse!!.id
                )
            ).partViewWithUtsendingskanal()
        } else null

}