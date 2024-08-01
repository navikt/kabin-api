package no.nav.klage.service

import no.nav.klage.api.controller.view.*
import no.nav.klage.api.controller.view.BehandlingstidChangeRegistreringView.BehandlingstidChangeRegistreringOverstyringerView
import no.nav.klage.api.controller.view.MottattVedtaksinstansChangeRegistreringView.MottattVedtaksinstansChangeRegistreringOverstyringerView
import no.nav.klage.clients.kabalapi.KabalApiClient
import no.nav.klage.domain.entities.PartId
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.domain.entities.SvarbrevReceiver
import no.nav.klage.exceptions.IllegalUpdateException
import no.nav.klage.exceptions.MissingAccessException
import no.nav.klage.exceptions.RegistreringNotFoundException
import no.nav.klage.kodeverk.*
import no.nav.klage.repository.RegistreringRepository
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.calculateFrist
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class RegistreringService(
    private val registreringRepository: RegistreringRepository,
    private val kabalApiClient: KabalApiClient,
    private val tokenUtil: TokenUtil,
) {

    fun createRegistrering(): FullRegistreringView {
        val registrering = registreringRepository.save(
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
                behandlingstidUnits = 12,
                behandlingstidUnitType = TimeUnitType.WEEKS,
                hjemmelIdList = listOf(),
                ytelse = null,
                saksbehandlerIdent = null,
                oppgaveId = null,
                sendSvarbrev = null,
                svarbrevTitle = "NAV orienterer om saksbehandlingen",
                svarbrevCustomText = null,
                svarbrevBehandlingstidUnits = null,
                svarbrevBehandlingstidUnitType = null,
                svarbrevFullmektigFritekst = null,
                svarbrevReceivers = mutableSetOf(),
                overrideSvarbrevCustomText = null,
                overrideSvarbrevBehandlingstid = null,
                finished = null,
                behandlingId = null,
            )
        )
        return registrering.toRegistreringView()
    }

    fun getRegistrering(registreringId: UUID): FullRegistreringView {
        return registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering ikke funnet.") }
            .toRegistreringView()
    }

    fun getFerdigeRegistreringer(
        sidenDager: Int?,
    ): List<FullRegistreringView> {
        return registreringRepository.findFerdigeRegistreringer(
            navIdent = tokenUtil.getCurrentIdent(),
            finishedFrom = LocalDateTime.now().minusDays(sidenDager?.toLong() ?: 31)
        ).map { it.toRegistreringView() }.sortedByDescending { it.finished }
    }

    fun getUferdigeRegistreringer(
    ): List<FullRegistreringView> {
        return registreringRepository.findUferdigeRegistreringer(
            navIdent = tokenUtil.getCurrentIdent(),
        ).map { it.toRegistreringView() }.sortedByDescending { it.created }
    }

    fun setSakenGjelderValue(registreringId: UUID, input: SakenGjelderValueInput): FullRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                sakenGjelder = input.sakenGjelderValue?.let { sakenGjelderValue ->
                    PartId(
                        value = sakenGjelderValue,
                        type = PartIdType.PERSON
                    )
                }
                modified = LocalDateTime.now()
                //empty the properties that no longer make sense if sakenGjelder changes.
                journalpostId = null
                ytelse = null
                type = null
                mulighetId = null
                mulighetFagsystem = null
                mottattVedtaksinstans = null
                mottattKlageinstans = null
                hjemmelIdList = listOf()
                klager = null
                fullmektig = null
                avsender = null
                saksbehandlerIdent = null
                oppgaveId = null
                sendSvarbrev = null
                overrideSvarbrevBehandlingstid = null
                overrideSvarbrevCustomText = null
                svarbrevCustomText = null
                svarbrevBehandlingstidUnits = null
                svarbrevBehandlingstidUnitType = null
                svarbrevFullmektigFritekst = null
                svarbrevReceivers.clear()
            }
        return registrering.toRegistreringView()
    }

    fun setJournalpostId(registreringId: UUID, input: JournalpostIdInput): FullRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                journalpostId = input.journalpostId
                modified = LocalDateTime.now()
                //empty the properties that no longer make sense if journalpostId changes.
                ytelse = null
                type = null
                mulighetId = null
                mulighetFagsystem = null
                mottattVedtaksinstans = null
                mottattKlageinstans = null
                hjemmelIdList = listOf()
                klager = null
                fullmektig = null
                avsender = null
                saksbehandlerIdent = null
                oppgaveId = null
                sendSvarbrev = null
                overrideSvarbrevBehandlingstid = null
                overrideSvarbrevCustomText = null
                svarbrevCustomText = null
                svarbrevBehandlingstidUnits = null
                svarbrevBehandlingstidUnitType = null
                svarbrevFullmektigFritekst = null
                svarbrevReceivers.clear()
            }

        return registrering.toRegistreringView()
    }

    fun setTypeId(registreringId: UUID, input: TypeIdInput): TypeChangeRegistreringView {
        return getRegistreringForUpdate(registreringId)
            .apply {
                type = input.typeId?.let { typeId ->
                    Type.of(typeId)
                }
                modified = LocalDateTime.now()
                //empty the properties that no longer make sense if typeId changes.
                mulighetId = null
                mulighetFagsystem = null
                mottattVedtaksinstans = null
                mottattKlageinstans = null
                hjemmelIdList = listOf()
                klager = null
                fullmektig = null
                avsender = null
                saksbehandlerIdent = null
                oppgaveId = null
                sendSvarbrev = null
                overrideSvarbrevBehandlingstid = null
                overrideSvarbrevCustomText = null
                svarbrevCustomText = null
                svarbrevBehandlingstidUnits = null
                svarbrevBehandlingstidUnitType = null
                svarbrevFullmektigFritekst = null
                svarbrevReceivers.clear()

            }.toTypeChangeRegistreringView()
    }

    fun setMulighet(registreringId: UUID, input: MulighetInput): MulighetChangeRegistreringView {
        return getRegistreringForUpdate(registreringId)
            .apply {
                mulighetId = input.mulighetId
                mulighetFagsystem = Fagsystem.of(input.fagsystemId)
                modified = LocalDateTime.now()

                //empty the properties that no longer make sense if mulighet changes.
                ytelse = null
                mottattVedtaksinstans = null
                mottattKlageinstans = null
                hjemmelIdList = listOf()
                klager = null
                fullmektig = null
                avsender = null
                saksbehandlerIdent = null
                oppgaveId = null
                sendSvarbrev = null
                overrideSvarbrevBehandlingstid = null
                overrideSvarbrevCustomText = null
                svarbrevCustomText = null
                svarbrevBehandlingstidUnits = null
                svarbrevBehandlingstidUnitType = null
                svarbrevFullmektigFritekst = null
                svarbrevReceivers.clear()
            }.toMulighetChangeRegistreringView()
    }

    fun setMottattVedtaksinstans(
        registreringId: UUID,
        input: MottattVedtaksinstansInput
    ): MottattVedtaksinstansChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                mottattVedtaksinstans = input.mottattVedtaksinstans
                modified = LocalDateTime.now()
            }
        return MottattVedtaksinstansChangeRegistreringView(
            id = registrering.id,
            overstyringer = MottattVedtaksinstansChangeRegistreringOverstyringerView(
                mottattVedtaksinstans = registrering.mottattVedtaksinstans,
            ),
            modified = registrering.modified,
        )
    }

    fun setMottattKlageinstans(
        registreringId: UUID,
        input: MottattKlageinstansInput
    ): MottattKlageinstansChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                mottattKlageinstans = input.mottattKlageinstans
                modified = LocalDateTime.now()
            }
        return MottattKlageinstansChangeRegistreringView(
            id = registrering.id,
            overstyringer = MottattKlageinstansChangeRegistreringView.MottattKlageinstansChangeRegistreringOverstyringerView(
                mottattKlageinstans = registrering.mottattKlageinstans,
                calculatedFrist = if (registrering.mottattKlageinstans != null) {
                    calculateFrist(
                        fromDate = registrering.mottattKlageinstans!!,
                        units = registrering.behandlingstidUnits.toLong(),
                        unitType = registrering.behandlingstidUnitType,
                    )
                } else null
            ),
            modified = registrering.modified,
        )
    }

    fun setBehandlingstid(registreringId: UUID, input: BehandlingstidInput): BehandlingstidChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                behandlingstidUnits = input.units
                behandlingstidUnitType = TimeUnitType.of(input.unitTypeId)
                modified = LocalDateTime.now()
            }
        return BehandlingstidChangeRegistreringView(
            id = registrering.id,
            overstyringer = BehandlingstidChangeRegistreringOverstyringerView(
                behandlingstid = BehandlingstidView(
                    unitTypeId = registrering.behandlingstidUnitType.id,
                    units = registrering.behandlingstidUnits
                ),
                calculatedFrist = if (registrering.mottattKlageinstans != null) {
                    calculateFrist(
                        fromDate = registrering.mottattKlageinstans!!,
                        units = registrering.behandlingstidUnits.toLong(),
                        unitType = registrering.behandlingstidUnitType
                    )
                } else null
            ),
            modified = registrering.modified,
        )
    }

    fun setHjemmelIdList(registreringId: UUID, input: HjemmelIdListInput) {
        getRegistreringForUpdate(registreringId)
            .apply {
                hjemmelIdList = input.hjemmelIdList
                modified = LocalDateTime.now()
            }
    }

    fun setYtelseId(registreringId: UUID, input: YtelseIdInput): YtelseChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                ytelse = input.ytelseId?.let { ytelseId ->
                    Ytelse.of(ytelseId)
                }
                modified = LocalDateTime.now()
                //TODO: hjemler are affected, maybe saksbehandler?
                //For now, just empty them.
                hjemmelIdList = listOf()
                saksbehandlerIdent = null
            }
        return YtelseChangeRegistreringView(
            id = registrering.id,
            overstyringer = YtelseChangeRegistreringView.YtelseChangeRegistreringOverstyringerView(
                ytelseId = registrering.ytelse?.id,
                saksbehandlerIdent = registrering.saksbehandlerIdent,
            ),
            modified = registrering.modified,
        )
    }

    fun setFullmektig(registreringId: UUID, input: PartIdInput?): FullmektigChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                //cases
                //1. fullmektig is set to the same value as before
                if (fullmektig?.value == input?.id) {
                    return@apply
                }
                //handle receivers for all cases
                handleReceiversWhenAddingPart(
                    unchangedRegistrering = this,
                    partIdInput = input,
                    partISaken = PartISaken.FULLMEKTIG
                )

                //2. fullmektig is set to null
                if (input == null) {
                    fullmektig = null
                    svarbrevFullmektigFritekst = null
                } else {
                    //3. fullmektig is set to a new value
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
                    val part = kabalApiClient.searchPart(SearchPartInput(identifikator = input.id))
                    svarbrevFullmektigFritekst = part.name
                }
                modified = LocalDateTime.now()
            }
        return FullmektigChangeRegistreringView(
            id = registrering.id,
            svarbrev = FullmektigChangeRegistreringView.FullmektigChangeSvarbrevView(
                fullmektigFritekst = registrering.svarbrevFullmektigFritekst,
                receivers = registrering.svarbrevReceivers.map { receiver ->
                    RecipientView(
                        id = receiver.id,
                        part = registrering.partViewWithUtsendingskanal(identifikator = receiver.part.value)!!,
                        handling = receiver.handling,
                        overriddenAddress = receiver.overriddenAddress?.let { address ->
                            RecipientView.AddressView(
                                adresselinje1 = address.adresselinje1,
                                adresselinje2 = address.adresselinje2,
                                adresselinje3 = address.adresselinje3,
                                landkode = address.landkode,
                                postnummer = address.postnummer,
                            )
                        }
                    )
                }
            ),
            overstyringer = FullmektigChangeRegistreringView.FullmektigChangeRegistreringOverstyringerView(
                fullmektig = registrering.fullmektig?.let { registrering.partViewWithUtsendingskanal(identifikator = it.value) }
            ),
            modified = registrering.modified,
        )
    }

    fun handleReceiversWhenAddingPart(unchangedRegistrering: Registrering, partIdInput: PartIdInput?, partISaken: PartISaken) {
        val svarbrevReceivers = unchangedRegistrering.svarbrevReceivers
        if (partIdInput != null) {
            if (svarbrevReceivers.any { it.part.value == partIdInput.id }) {
                //if the receiver is already in the list, we don't need to do anything.
                return
            }
        } else {
            val existingParts = listOf(
                unchangedRegistrering.sakenGjelder?.value,
                unchangedRegistrering.klager?.value,
                unchangedRegistrering.avsender?.value,
                unchangedRegistrering.fullmektig?.value
            )

            //if the receiver is in the list, we remove it.
            when {
                partISaken == PartISaken.FULLMEKTIG && existingParts.count { it == unchangedRegistrering.fullmektig?.value } == 1 -> {
                    svarbrevReceivers.removeIf { it.part.value == unchangedRegistrering.fullmektig?.value }
                }
                partISaken == PartISaken.KLAGER && existingParts.count { it == unchangedRegistrering.klager?.value } == 1 -> {
                    svarbrevReceivers.removeIf { it.part.value == unchangedRegistrering.klager?.value }
                }
                partISaken == PartISaken.AVSENDER && existingParts.count { it == unchangedRegistrering.avsender?.value } == 1 -> {
                    svarbrevReceivers.removeIf { it.part.value == unchangedRegistrering.avsender?.value }
                }
            }
        }
    }

    enum class PartISaken {
        SAKEN_GJELDER,
        KLAGER,
        AVSENDER,
        FULLMEKTIG,
    }

    fun setKlager(registreringId: UUID, input: PartIdInput?) {
        getRegistreringForUpdate(registreringId)
            .apply {
                klager = input?.let {
                    PartId(
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
                modified = LocalDateTime.now()

                //if they are receivers of svarbrev, they will be affected
            }
    }

    fun setAvsender(registreringId: UUID, input: PartIdInput?) {
        getRegistreringForUpdate(registreringId)
            .apply {
                avsender = input?.let {
                    PartId(
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
                modified = LocalDateTime.now()

                //if they are receivers of svarbrev, they will be affected
            }
    }

    fun setSaksbehandlerIdent(registreringId: UUID, input: SaksbehandlerIdentInput) {
        getRegistreringForUpdate(registreringId)
            .apply {
                saksbehandlerIdent = input.saksbehandlerIdent
                modified = LocalDateTime.now()
            }
    }

    fun setOppgaveId(registreringId: UUID, input: OppgaveIdInput) {
        getRegistreringForUpdate(registreringId)
            .apply {
                oppgaveId = input.oppgaveId
                modified = LocalDateTime.now()
            }
    }

    fun setSendSvarbrev(registreringId: UUID, input: SendSvarbrevInput) {
        getRegistreringForUpdate(registreringId)
            .apply {
                sendSvarbrev = input.send
                modified = LocalDateTime.now()
            }
    }

    fun setSvarbrevOverrideCustomText(registreringId: UUID, input: SvarbrevOverrideCustomTextInput) {
        getRegistreringForUpdate(registreringId)
            .apply {
                overrideSvarbrevCustomText = input.overrideCustomText
                modified = LocalDateTime.now()
            }
    }

    fun setSvarbrevOverrideBehandlingstid(registreringId: UUID, input: SvarbrevOverrideBehandlingstidInput) {
        getRegistreringForUpdate(registreringId)
            .apply {
                overrideSvarbrevBehandlingstid = input.overrideBehandlingstid
                modified = LocalDateTime.now()
            }
    }

    fun setSvarbrevTitle(registreringId: UUID, input: SvarbrevTitleInput) {
        getRegistreringForUpdate(registreringId)
            .apply {
                svarbrevTitle = input.title
                modified = LocalDateTime.now()
            }
    }

    fun setSvarbrevCustomText(registreringId: UUID, input: SvarbrevCustomTextInput) {
        getRegistreringForUpdate(registreringId)
            .apply {
                svarbrevCustomText = input.customText
                modified = LocalDateTime.now()
            }
    }

    fun setSvarbrevBehandlingstid(registreringId: UUID, input: BehandlingstidInput): SvarbrevBehandlingstidChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                svarbrevBehandlingstidUnits = input.units
                svarbrevBehandlingstidUnitType = TimeUnitType.of(input.unitTypeId)
                modified = LocalDateTime.now()
            }
        return SvarbrevBehandlingstidChangeRegistreringView(
            id = registrering.id,
            svarbrev = SvarbrevBehandlingstidChangeRegistreringView.SvarbrevBehandlingstidChangeRegistreringSvarbrevView(
                behandlingstid = BehandlingstidView(
                    unitTypeId = registrering.svarbrevBehandlingstidUnitType!!.id,
                    units = registrering.svarbrevBehandlingstidUnits!!
                ),
                calculatedFrist = if (registrering.mottattKlageinstans != null) {
                    calculateFrist(
                        fromDate = registrering.mottattKlageinstans!!,
                        units = registrering.svarbrevBehandlingstidUnits!!.toLong(),
                        unitType = registrering.svarbrevBehandlingstidUnitType!!
                    )
                } else null
            ),
            modified = registrering.modified,
        )
    }

    fun setSvarbrevFullmektigFritekst(registreringId: UUID, input: SvarbrevFullmektigFritekstInput) {
        getRegistreringForUpdate(registreringId)
            .apply {
                svarbrevFullmektigFritekst = input.fullmektigFritekst
                modified = LocalDateTime.now()
            }
    }

    //TODO: add one for each receiver, crud.
    //use internal id for update/delete
    fun setSvarbrevReceivers(registreringId: UUID, input: SvarbrevReceiversInput) {
        getRegistreringForUpdate(registreringId)
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
                modified = LocalDateTime.now()
            }
    }

    private fun Registrering.toTypeChangeRegistreringView(): TypeChangeRegistreringView {
        return TypeChangeRegistreringView(
            id = id,
            typeId = type?.id,
            overstyringer = TypeChangeRegistreringView.TypeChangeRegistreringOverstyringerView(),
            svarbrev = TypeChangeRegistreringView.TypeChangeRegistreringSvarbrevView(),
            modified = modified,
        )
    }

    private fun Registrering.toMulighetChangeRegistreringView(): MulighetChangeRegistreringView {
        return MulighetChangeRegistreringView(
            id = id,
            mulighet = if (mulighetId != null) {
                MulighetView(
                    id = mulighetId!!,
                    fagsystemId = mulighetFagsystem!!.id
                )
            } else null,
            overstyringer = MulighetChangeRegistreringView.MulighetChangeRegistreringOverstyringerView(),
            svarbrev = MulighetChangeRegistreringView.MulighetChangeRegistreringSvarbrevView(),
            modified = modified,
        )
    }

    private fun Registrering.toRegistreringView() = FullRegistreringView(
        id = id,
        journalpostId = journalpostId,
        sakenGjelderValue = sakenGjelder?.value,
        typeId = type?.id,
        mulighet = if (mulighetId != null) {
            MulighetView(
                id = mulighetId!!,
                fagsystemId = mulighetFagsystem!!.id
            )
        } else null,
        overstyringer = FullRegistreringView.FullRegistreringOverstyringerView(
            mottattVedtaksinstans = mottattVedtaksinstans,
            mottattKlageinstans = mottattKlageinstans,
            behandlingstid =
            BehandlingstidView(
                unitTypeId = behandlingstidUnitType.id,
                units = behandlingstidUnits
            ),
            calculatedFrist = if (mottattKlageinstans != null) {
                calculateFrist(
                    fromDate = mottattKlageinstans!!,
                    units = behandlingstidUnits.toLong(),
                    unitType = behandlingstidUnitType
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
        svarbrev = FullRegistreringView.FullRegistreringSvarbrevView(
            send = sendSvarbrev,
            behandlingstid = if (svarbrevBehandlingstidUnits != null) {
                BehandlingstidView(
                    unitTypeId = svarbrevBehandlingstidUnitType!!.id,
                    units = svarbrevBehandlingstidUnits!!
                )
            } else null,
            fullmektigFritekst = svarbrevFullmektigFritekst,
            receivers = svarbrevReceivers.map { receiver ->
                RecipientView(
                    id = receiver.id,
                    part = partViewWithUtsendingskanal(identifikator = receiver.part.value)!!,
                    handling = receiver.handling,
                    overriddenAddress = receiver.overriddenAddress?.let { address ->
                        RecipientView.AddressView(
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
            overrideCustomText = overrideSvarbrevCustomText ?: false,
            overrideBehandlingstid = overrideSvarbrevBehandlingstid ?: false,
            calculatedFrist = if (mottattKlageinstans != null && svarbrevBehandlingstidUnits != null) {
                calculateFrist(
                    fromDate = mottattKlageinstans!!,
                    units = svarbrevBehandlingstidUnits!!.toLong(),
                    unitType = svarbrevBehandlingstidUnitType!!
                )
            } else null,
        ),
        created = created,
        modified = modified,
        createdBy = createdBy,
        finished = finished,
        behandlingId = behandlingId,
    )

    fun deleteRegistrering(registreringId: UUID) {
        registreringRepository.deleteById(registreringId)
    }

    fun validateRegistrering(registreringId: UUID) {
        val registrering = registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering ikke funnet.") }
        TODO()
    }

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

    private fun getRegistreringForUpdate(registreringId: UUID): Registrering {
        return registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering ikke funnet.") }
            .also {
                if (it.createdBy != tokenUtil.getCurrentIdent()) {
                    throw MissingAccessException("Registreringen tilhører ikke deg.")
                }
                if (it.finished != null) {
                    throw IllegalUpdateException("Registreringen er allerede ferdigstilt.")
                }
            }
    }

}