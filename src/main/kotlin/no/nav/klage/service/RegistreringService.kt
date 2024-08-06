package no.nav.klage.service

import no.nav.klage.api.controller.view.*
import no.nav.klage.api.controller.view.BehandlingstidChangeRegistreringView.BehandlingstidChangeRegistreringOverstyringerView
import no.nav.klage.api.controller.view.MottattVedtaksinstansChangeRegistreringView.MottattVedtaksinstansChangeRegistreringOverstyringerView
import no.nav.klage.clients.kabalapi.KabalApiClient
import no.nav.klage.domain.entities.PartId
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.domain.entities.SvarbrevReceiver
import no.nav.klage.exceptions.*
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
    private val klageService: KlageService,
    private val ankeService: AnkeService,
    private val documentService: DocumentService,
    private val dokArkivService: DokArkivService,
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
                mulighetOriginalFagsystem = null,
                mulighetCurrentFagsystem = null,
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
                overrideSvarbrevCustomText = false,
                overrideSvarbrevBehandlingstid = false,
                finished = null,
                behandlingId = null,
                willCreateNewJournalpost = false,
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
                mulighetOriginalFagsystem = null
                mottattVedtaksinstans = null
                mottattKlageinstans = null
                hjemmelIdList = listOf()
                klager = null
                fullmektig = null
                avsender = null
                saksbehandlerIdent = null
                oppgaveId = null
                sendSvarbrev = null
                overrideSvarbrevBehandlingstid = false
                overrideSvarbrevCustomText = false
                svarbrevCustomText = null
                svarbrevBehandlingstidUnits = null
                svarbrevBehandlingstidUnitType = null
                svarbrevFullmektigFritekst = null
                svarbrevReceivers.clear()
                willCreateNewJournalpost = false
            }
        return registrering.toRegistreringView()
    }

    fun setJournalpostId(registreringId: UUID, input: JournalpostIdInput): FullRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                journalpostId = input.journalpostId
                modified = LocalDateTime.now()

                val document = documentService.fetchDokument(
                    journalpostId = input.journalpostId
                )
                mottattKlageinstans = document.datoOpprettet.toLocalDate()
                mottattVedtaksinstans = document.datoOpprettet.toLocalDate()

                //empty the properties that no longer make sense if journalpostId changes.
//                ytelse = null
//                type = null
//                mulighetId = null
//                mulighetOriginalFagsystem = null
//                hjemmelIdList = listOf()
//                klager = null
//                fullmektig = null
                //TODO depends on previous value
                avsender = null
                svarbrevReceivers.clear()
//                saksbehandlerIdent = null
//                oppgaveId = null
//                sendSvarbrev = null
//                overrideSvarbrevBehandlingstid = false
//                overrideSvarbrevCustomText = false
//                svarbrevCustomText = null
//                svarbrevBehandlingstidUnits = null
//                svarbrevBehandlingstidUnitType = null
//                svarbrevFullmektigFritekst = null
                if (mulighetId != null) {
                    willCreateNewJournalpost = dokArkivService.journalpostIsFinalizedAndConnectedToFagsak(
                        journalpostId = journalpostId!!,
                        fagsakId = mulighetId!!,
                        fagsystemId = mulighetOriginalFagsystem!!.id,
                    )
                }
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
                mulighetOriginalFagsystem = null
//                mottattVedtaksinstans = null
//                hjemmelIdList = listOf()
                //TODO depends
                klager = null
                //TODO depends on mulighet
//                fullmektig = null
                //TODO depends
                avsender = null
                saksbehandlerIdent = null
                oppgaveId = null
                sendSvarbrev = null
                overrideSvarbrevBehandlingstid = false
                overrideSvarbrevCustomText = false
                svarbrevCustomText = null
                svarbrevBehandlingstidUnits = null
                svarbrevBehandlingstidUnitType = null
                svarbrevFullmektigFritekst = null
                svarbrevReceivers.clear()
                willCreateNewJournalpost = false

            }.toTypeChangeRegistreringView()
    }

    fun setMulighet(registreringId: UUID, input: MulighetInput): MulighetChangeRegistreringView {
        return getRegistreringForUpdate(registreringId)
            .apply {
                mulighetId = input.mulighetId
                mulighetOriginalFagsystem = Fagsystem.of(input.originalFagsystemId)
                mulighetCurrentFagsystem = Fagsystem.of(input.currentFagsystemId)
                ytelse = getYtelseOrNull(this)
                modified = LocalDateTime.now()

                willCreateNewJournalpost = dokArkivService.journalpostIsFinalizedAndConnectedToFagsak(
                    journalpostId = this.journalpostId!!,
                    fagsakId = this.mulighetId!!,
                    fagsystemId = this.mulighetOriginalFagsystem!!.id,
                )

                //empty the properties that no longer make sense if mulighet changes.
                mottattVedtaksinstans = null
                mottattKlageinstans = null
                hjemmelIdList = listOf()
                klager = null
//                fullmektig = null
                avsender = null
                saksbehandlerIdent = null
                oppgaveId = null
                sendSvarbrev = null
                overrideSvarbrevBehandlingstid = false
                overrideSvarbrevCustomText = false
                svarbrevCustomText = null
                svarbrevBehandlingstidUnits = null
                svarbrevBehandlingstidUnitType = null
                svarbrevFullmektigFritekst = null
                svarbrevReceivers.clear()
            }.toMulighetChangeRegistreringView()
    }

    private fun getYtelseOrNull(registrering: Registrering): Ytelse? {
        if (registrering.type == null) {
            return null
        }

        val input = IdnummerInput(idnummer = registrering.sakenGjelder!!.value)

        val temaId = if (registrering.type == Type.KLAGE) {
            val mulighet = klageService.getKlagemuligheter(input = input).find {
                it.id == registrering.mulighetId && it.originalFagsystemId == registrering.mulighetOriginalFagsystem!!.id
            }
            mulighet?.temaId
        } else if (registrering.type == Type.ANKE) {
            val mulighet = ankeService.getAnkemuligheter(input = input).find {
                it.id == registrering
                    .mulighetId && it.fagsystemId == registrering.mulighetOriginalFagsystem!!.id
            }
            if (mulighet?.ytelseId != null) {
                return Ytelse.of(mulighet.ytelseId)
            }
            mulighet?.temaId
        } else null

        if (temaId == null) {
            return null
        }

        val possibleYtelser = Ytelse.entries.filter { it.toTema().id == temaId }

        return if (possibleYtelser.size == 1) {
            possibleYtelser.first()
        } else null
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

    fun setHjemmelIdList(registreringId: UUID, input: HjemmelIdListInput): HjemmelIdListChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                hjemmelIdList = input.hjemmelIdList
                modified = LocalDateTime.now()
            }
        return HjemmelIdListChangeRegistreringView(
            id = registrering.id,
            overstyringer = HjemmelIdListChangeRegistreringView.HjemmelIdListChangeRegistreringOverstyringerView(
                hjemmelIdList = registrering.hjemmelIdList,
            ),
            modified = registrering.modified,
        )
    }

    fun setYtelseId(registreringId: UUID, input: YtelseIdInput): YtelseChangeRegistreringView {
        //svarbrev settings

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

    fun setFullmektig(registreringId: UUID, input: FullmektigInput): FullmektigChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                //cases
                //1. fullmektig is set to the same value as before
                if (fullmektig?.value == input.fullmektig?.id) {
                    return@apply
                }
                //handle receivers for all cases
                handleReceiversWhenAddingPart(
                    unchangedRegistrering = this,
                    partIdInput = input.fullmektig,
                    partISaken = PartISaken.FULLMEKTIG
                )

                //2. fullmektig is set to null
                if (input.fullmektig == null) {
                    fullmektig = null
                    svarbrevFullmektigFritekst = null
                } else {
                    //3. fullmektig is set to a new value
                    fullmektig = PartId(
                        value = input.fullmektig.id,
                        type = when (input.fullmektig.type) {
                            PartType.FNR -> {
                                PartIdType.PERSON
                            }

                            PartType.ORGNR -> {
                                PartIdType.VIRKSOMHET
                            }
                        }
                    )
                    val part = kabalApiClient.searchPart(SearchPartInput(identifikator = input.fullmektig.id))
                    svarbrevFullmektigFritekst = part.name
                }
                modified = LocalDateTime.now()
            }
        return FullmektigChangeRegistreringView(
            id = registrering.id,
            svarbrev = FullmektigChangeRegistreringView.FullmektigChangeSvarbrevView(
                fullmektigFritekst = registrering.svarbrevFullmektigFritekst,
                receivers = registrering.svarbrevReceivers.map { receiver ->
                    receiver.toRecipientView(registrering)
                }
            ),
            overstyringer = FullmektigChangeRegistreringView.FullmektigChangeRegistreringOverstyringerView(
                fullmektig = registrering.fullmektig?.let { registrering.partViewWithUtsendingskanal(identifikator = it.value) }
            ),
            modified = registrering.modified,
        )
    }

    fun handleReceiversWhenAddingPart(
        unchangedRegistrering: Registrering,
        partIdInput: PartIdInput?,
        partISaken: PartISaken
    ) {
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
        KLAGER,
        AVSENDER,
        FULLMEKTIG,
    }

    fun setKlager(registreringId: UUID, input: KlagerInput): KlagerChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                //cases
                //1. klager is set to the same value as before
                if (klager?.value == input.klager?.id) {
                    return@apply
                }
                //handle receivers for all cases
                handleReceiversWhenAddingPart(
                    unchangedRegistrering = this,
                    partIdInput = input.klager,
                    partISaken = PartISaken.KLAGER
                )

                //2. klager is set to null
                if (input.klager == null) {
                    klager = null
                } else {
                    //3. klager is set to a new value
                    klager = PartId(
                        value = input.klager.id,
                        type = when (input.klager.type) {
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
            }
        return KlagerChangeRegistreringView(
            id = registrering.id,
            svarbrev = KlagerChangeRegistreringView.KlagerChangeRegistreringViewSvarbrevView(
                receivers = registrering.svarbrevReceivers.map { receiver ->
                    receiver.toRecipientView(registrering)
                }
            ),
            overstyringer = KlagerChangeRegistreringView.KlagerChangeRegistreringViewRegistreringOverstyringerView(
                klager = registrering.klager?.let { registrering.partViewWithUtsendingskanal(identifikator = it.value) }
            ),
            modified = registrering.modified,
        )
    }

    private fun SvarbrevReceiver.toRecipientView(
        registrering: Registrering
    ) = RecipientView(
        id = id,
        part = registrering.partViewWithUtsendingskanal(identifikator = part.value)!!,
        handling = handling,
        overriddenAddress = overriddenAddress?.let { address ->
            RecipientView.AddressView(
                adresselinje1 = address.adresselinje1,
                adresselinje2 = address.adresselinje2,
                adresselinje3 = address.adresselinje3,
                landkode = address.landkode,
                postnummer = address.postnummer,
            )
        }
    )

    fun setAvsender(registreringId: UUID, input: AvsenderInput): AvsenderChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                //cases
                //1. avsender is set to the same value as before
                if (avsender?.value == input.avsender?.id) {
                    return@apply
                }
                //handle receivers for all cases
                handleReceiversWhenAddingPart(
                    unchangedRegistrering = this,
                    partIdInput = input.avsender,
                    partISaken = PartISaken.AVSENDER
                )

                //2. avsender is set to null
                if (input.avsender == null) {
                    avsender = null
                } else {
                    //3. avsender is set to a new value
                    avsender = PartId(
                        value = input.avsender.id,
                        type = when (input.avsender.type) {
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
            }
        return AvsenderChangeRegistreringView(
            id = registrering.id,
            svarbrev = AvsenderChangeRegistreringView.AvsenderChangeRegistreringViewSvarbrevView(
                receivers = registrering.svarbrevReceivers.map { receiver ->
                    receiver.toRecipientView(registrering)
                }
            ),
            overstyringer = AvsenderChangeRegistreringView.AvsenderChangeRegistreringViewRegistreringOverstyringerView(
                avsender = registrering.avsender?.let { registrering.partViewWithUtsendingskanal(identifikator = it.value) }
            ),
            modified = registrering.modified,
        )

    }

    fun setSaksbehandlerIdent(
        registreringId: UUID,
        input: SaksbehandlerIdentInput
    ): SaksbehandlerIdentChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                saksbehandlerIdent = input.saksbehandlerIdent
                modified = LocalDateTime.now()
            }
        return SaksbehandlerIdentChangeRegistreringView(
            id = registrering.id,
            overstyringer = SaksbehandlerIdentChangeRegistreringView.SaksbehandlerIdentChangeRegistreringOverstyringerView(
                saksbehandlerIdent = registrering.saksbehandlerIdent,
            ),
            modified = registrering.modified,
        )
    }

    fun setOppgaveId(registreringId: UUID, input: OppgaveIdInput): OppgaveIdChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                oppgaveId = input.oppgaveId
                modified = LocalDateTime.now()
            }

        return OppgaveIdChangeRegistreringView(
            id = registrering.id,
            overstyringer = OppgaveIdChangeRegistreringView.OppgaveIdChangeRegistreringOverstyringerView(
                oppgaveId = registrering.oppgaveId,
            ),
            modified = registrering.modified,
        )
    }

    fun setSendSvarbrev(registreringId: UUID, input: SendSvarbrevInput): SendSvarbrevChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                sendSvarbrev = input.send
                modified = LocalDateTime.now()
            }

        return SendSvarbrevChangeRegistreringView(
            id = registrering.id,
            svarbrev = SendSvarbrevChangeRegistreringView.SendSvarbrevChangeRegistreringSvarbrevView(
                send = registrering.sendSvarbrev!!,
            ),
            modified = registrering.modified,
        )
    }

    fun setSvarbrevOverrideCustomText(
        registreringId: UUID,
        input: SvarbrevOverrideCustomTextInput
    ): SvarbrevOverrideCustomTextChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                overrideSvarbrevCustomText = input.overrideCustomText

                val svarbrevSettings = getSvarbrevSettings()

                if (!input.overrideCustomText) {
                    svarbrevCustomText = svarbrevSettings.customText
                }

                modified = LocalDateTime.now()
            }
        return SvarbrevOverrideCustomTextChangeRegistreringView(
            id = registrering.id,
            svarbrev = SvarbrevOverrideCustomTextChangeRegistreringView.SvarbrevOverrideCustomTextChangeRegistreringSvarbrevView(
                overrideCustomText = registrering.overrideSvarbrevCustomText!!,
                customText = registrering.svarbrevCustomText,
            ),
            modified = registrering.modified,
        )
    }

    fun setSvarbrevOverrideBehandlingstid(
        registreringId: UUID,
        input: SvarbrevOverrideBehandlingstidInput
    ): SvarbrevOverrideBehandlingstidChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                overrideSvarbrevBehandlingstid = input.overrideBehandlingstid

                val svarbrevSettings = getSvarbrevSettings()

                if (!input.overrideBehandlingstid) {
                    svarbrevBehandlingstidUnits = svarbrevSettings.behandlingstidUnits
                    svarbrevBehandlingstidUnitType = svarbrevSettings.behandlingstidUnitType
                }

                modified = LocalDateTime.now()
            }
        return SvarbrevOverrideBehandlingstidChangeRegistreringView(
            id = registrering.id,
            svarbrev = SvarbrevOverrideBehandlingstidChangeRegistreringView.SvarbrevOverrideBehandlingstidChangeRegistreringSvarbrevView(
                overrideBehandlingstid = registrering.overrideSvarbrevBehandlingstid!!,
                behandlingstid = if (registrering.svarbrevBehandlingstidUnits != null) {
                    BehandlingstidView(
                        unitTypeId = registrering.svarbrevBehandlingstidUnitType!!.id,
                        units = registrering.svarbrevBehandlingstidUnits!!
                    )
                } else null,
            ),
            modified = registrering.modified,
        )
    }

    private fun Registrering.getSvarbrevSettings() = kabalApiClient.getSvarbrevSettings(
        ytelseId = ytelse!!.id,
        typeId = type!!.id
    )

    fun setSvarbrevTitle(registreringId: UUID, input: SvarbrevTitleInput): SvarbrevTitleChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                svarbrevTitle = input.title
                modified = LocalDateTime.now()
            }
        return SvarbrevTitleChangeRegistreringView(
            id = registrering.id,
            svarbrev = SvarbrevTitleChangeRegistreringView.SvarbrevTitleChangeRegistreringSvarbrevView(
                title = registrering.svarbrevTitle,
            ),
            modified = registrering.modified,
        )
    }

    fun setSvarbrevCustomText(
        registreringId: UUID,
        input: SvarbrevCustomTextInput
    ): SvarbrevCustomTextChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                svarbrevCustomText = input.customText
                modified = LocalDateTime.now()
            }
        return SvarbrevCustomTextChangeRegistreringView(
            id = registrering.id,
            svarbrev = SvarbrevCustomTextChangeRegistreringView.SvarbrevCustomTextChangeRegistreringSvarbrevView(
                customText = registrering.svarbrevCustomText!!,
            ),
            modified = registrering.modified,
        )
    }

    fun setSvarbrevBehandlingstid(
        registreringId: UUID,
        input: BehandlingstidInput
    ): SvarbrevBehandlingstidChangeRegistreringView {
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

    fun setSvarbrevFullmektigFritekst(
        registreringId: UUID,
        input: SvarbrevFullmektigFritekstInput
    ): SvarbrevFullmektigFritekstChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                val fritekst = if (!input.fullmektigFritekst.isNullOrBlank()) {
                    input.fullmektigFritekst
                } else {
                    null
                }
                svarbrevFullmektigFritekst = fritekst
                modified = LocalDateTime.now()
            }
        return SvarbrevFullmektigFritekstChangeRegistreringView(
            id = registrering.id,
            svarbrev = SvarbrevFullmektigFritekstChangeRegistreringView.SvarbrevFullmektigFritekstChangeRegistreringSvarbrevView(
                fullmektigFritekst = registrering.svarbrevFullmektigFritekst,
            ),
            modified = registrering.modified,
        )
    }

    fun deleteSvarbrevReceiver(registreringId: UUID, svarbrevReceiverId: UUID): SvarbrevReceiverChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                svarbrevReceivers.removeIf { it.id == svarbrevReceiverId }
                modified = LocalDateTime.now()
            }
        return SvarbrevReceiverChangeRegistreringView(
            id = registrering.id,
            svarbrev = SvarbrevReceiverChangeRegistreringView.SvarbrevReceiverChangeRegistreringSvarbrevView(
                receivers = registrering.svarbrevReceivers.map { receiver ->
                    receiver.toRecipientView(registrering)
                }
            ),
            modified = registrering.modified,
        )
    }

    fun addSvarbrevReceiver(
        registreringId: UUID,
        input: SvarbrevRecipientInput
    ): SvarbrevReceiverChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                if (svarbrevReceivers.any { it.part.value == input.part.id }) {
                    throw ReceiverAlreadyExistException("Mottaker finnes allerede.")
                }
                svarbrevReceivers.add(
                    SvarbrevReceiver(
                        part = PartId(
                            value = input.part.id,
                            type = when (input.part.type) {
                                PartType.FNR -> {
                                    PartIdType.PERSON
                                }

                                PartType.ORGNR -> {
                                    PartIdType.VIRKSOMHET
                                }
                            }
                        ),
                        handling = input.handling,
                        overriddenAddress = input.overriddenAddress?.let { address ->
                            no.nav.klage.domain.entities.Address(
                                adresselinje1 = address.adresselinje1,
                                adresselinje2 = address.adresselinje2,
                                adresselinje3 = address.adresselinje3,
                                landkode = address.landkode,
                                postnummer = address.postnummer,
                            )
                        }
                    )
                )
                modified = LocalDateTime.now()
            }
        return SvarbrevReceiverChangeRegistreringView(
            id = registrering.id,
            svarbrev = SvarbrevReceiverChangeRegistreringView.SvarbrevReceiverChangeRegistreringSvarbrevView(
                receivers = registrering.svarbrevReceivers.map { receiver ->
                    receiver.toRecipientView(registrering)
                }
            ),
            modified = registrering.modified,
        )
    }

    fun modifySvarbrevReceiver(
        registreringId: UUID,
        svarbrevReceiverId: UUID,
        input: ModifySvarbrevRecipientInput
    ): SvarbrevReceiverChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                val receiver = svarbrevReceivers.find { it.id == svarbrevReceiverId }
                    ?: throw ReceiverNotFoundException("Mottaker ikke funnet.")
                receiver.apply {
                    handling = input.handling
                    overriddenAddress = input.overriddenAddress?.let { address ->
                        no.nav.klage.domain.entities.Address(
                            adresselinje1 = address.adresselinje1,
                            adresselinje2 = address.adresselinje2,
                            adresselinje3 = address.adresselinje3,
                            landkode = address.landkode,
                            postnummer = address.postnummer,
                        )
                    }
                }
                modified = LocalDateTime.now()
            }
        return SvarbrevReceiverChangeRegistreringView(
            id = registrering.id,
            svarbrev = SvarbrevReceiverChangeRegistreringView.SvarbrevReceiverChangeRegistreringSvarbrevView(
                receivers = registrering.svarbrevReceivers.map { receiver ->
                    receiver.toRecipientView(registrering)
                }
            ),
            modified = registrering.modified,
        )
    }

    private fun Registrering.toTypeChangeRegistreringView(): TypeChangeRegistreringView {
        return TypeChangeRegistreringView(
            id = id,
            typeId = type?.id,
            overstyringer = TypeChangeRegistreringView.TypeChangeRegistreringOverstyringerView(),
            svarbrev = TypeChangeRegistreringView.TypeChangeRegistreringSvarbrevView(),
            modified = modified,
            willCreateNewJournalpost = willCreateNewJournalpost,
        )
    }

    private fun Registrering.toMulighetChangeRegistreringView(): MulighetChangeRegistreringView {
        return MulighetChangeRegistreringView(
            id = id,
            mulighet = if (mulighetId != null) {
                MulighetView(
                    id = mulighetId!!,
                    originalFagsystemId = mulighetOriginalFagsystem!!.id,
                    currentFagsystemId = mulighetCurrentFagsystem!!.id,
                )
            } else null,
            overstyringer = MulighetChangeRegistreringView.MulighetChangeRegistreringOverstyringerView(
                ytelseId = ytelse?.id,
            ),
            svarbrev = MulighetChangeRegistreringView.MulighetChangeRegistreringSvarbrevView(),
            modified = modified,
            willCreateNewJournalpost = willCreateNewJournalpost,
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
                originalFagsystemId = mulighetOriginalFagsystem!!.id,
                currentFagsystemId = mulighetCurrentFagsystem!!.id,
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
                receiver.toRecipientView(this)
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
        willCreateNewJournalpost = willCreateNewJournalpost,
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

    fun finishRegistrering(registreringId: UUID): FerdigstiltRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)

        val response: CreatedBehandlingResponse = if (registrering.type == Type.ANKE) {
            ankeService.createAnke(
                CreateAnkeInputView(
                    mottattKlageinstans = registrering.mottattKlageinstans,
                    behandlingstidUnits = registrering.behandlingstidUnits,
                    behandlingstidUnitType = registrering.behandlingstidUnitType,
                    behandlingstidUnitTypeId = registrering.behandlingstidUnitType.id,
                    klager = registrering.klager.toPartIdInput(),
                    fullmektig = registrering.fullmektig.toPartIdInput(),
                    journalpostId = registrering.journalpostId,
                    ytelseId = registrering.ytelse?.id,
                    hjemmelIdList = registrering.hjemmelIdList,
                    avsender = registrering.avsender.toPartIdInput(),
                    saksbehandlerIdent = registrering.saksbehandlerIdent,
                    svarbrevInput = registrering.toSvarbrevWithReceiverInput(),
                    vedtak = if (registrering.mulighetId != null && registrering.mulighetCurrentFagsystem != null) {
                        Vedtak(
                            id = registrering.mulighetId!!,
                            sourceId = registrering.mulighetCurrentFagsystem!!.id,
                        )
                    } else null,
                    oppgaveId = registrering.oppgaveId,
                )
            )
        } else if (registrering.type == Type.KLAGE) {
            klageService.createKlage(
                CreateKlageInputView(
                    mottattKlageinstans = registrering.mottattKlageinstans,
                    mottattVedtaksinstans = registrering.mottattVedtaksinstans,
                    behandlingstidUnits = registrering.behandlingstidUnits,
                    behandlingstidUnitType = registrering.behandlingstidUnitType,
                    behandlingstidUnitTypeId = registrering.behandlingstidUnitType.id,
                    klager = registrering.klager.toPartIdInput(),
                    fullmektig = registrering.fullmektig.toPartIdInput(),
                    journalpostId = registrering.journalpostId,
                    ytelseId = registrering.ytelse?.id,
                    hjemmelIdList = registrering.hjemmelIdList,
                    avsender = registrering.avsender.toPartIdInput(),
                    saksbehandlerIdent = registrering.saksbehandlerIdent,
                    svarbrevInput = registrering.toSvarbrevWithReceiverInput(),
                    vedtak = if (registrering.mulighetId != null && registrering.mulighetOriginalFagsystem != null) {
                        Vedtak(
                            id = registrering.mulighetId!!,
                            sourceId = registrering.mulighetOriginalFagsystem!!.id,
                        )
                    } else null,
                    oppgaveId = registrering.oppgaveId,
                )
            )
        } else {
            throw IllegalInputException("Registreringen er av en type som ikke støttes: ${registrering.type}.")
        }

        val now = LocalDateTime.now()
        registrering.behandlingId = response.behandlingId
        registrering.finished = now
        registrering.modified = now

        return FerdigstiltRegistreringView(
            id = registrering.id,
            modified = registrering.modified,
            finished = registrering.finished!!,
            behandlingId = registrering.behandlingId!!,
        )

    }

    private fun Registrering.toSvarbrevWithReceiverInput(): SvarbrevWithReceiverInput? {
        val svarbrevSettings = getSvarbrevSettings()

        if (svarbrevReceivers.isEmpty()) {
            return null
        }

        return SvarbrevWithReceiverInput(
            title = svarbrevTitle,
            customText = if (overrideSvarbrevCustomText == true) svarbrevCustomText else svarbrevSettings.customText,
            receivers = svarbrevReceivers.map { receiver ->
                SvarbrevWithReceiverInput.Receiver(
                    id = receiver.part.value,
                    handling = SvarbrevWithReceiverInput.Receiver.HandlingEnum.valueOf(receiver.handling.name),
                    overriddenAddress = receiver.overriddenAddress?.let { address ->
                        SvarbrevWithReceiverInput.Receiver.AddressInput(
                            adresselinje1 = address.adresselinje1,
                            adresselinje2 = address.adresselinje2,
                            adresselinje3 = address.adresselinje3,
                            landkode = address.landkode!!,
                            postnummer = address.postnummer,
                        )
                    }
                )
            },
            fullmektigFritekst = svarbrevFullmektigFritekst,
            varsletBehandlingstidUnits = if (overrideSvarbrevBehandlingstid == true) svarbrevBehandlingstidUnits!! else svarbrevSettings.behandlingstidUnits,
            varsletBehandlingstidUnitType = if (overrideSvarbrevBehandlingstid == true) svarbrevBehandlingstidUnitType!! else svarbrevSettings.behandlingstidUnitType,
            varsletBehandlingstidUnitTypeId = if (overrideSvarbrevBehandlingstid == true) svarbrevBehandlingstidUnitType!!.id else svarbrevSettings.behandlingstidUnitType.id,
        )
    }

    private fun PartId?.toPartIdInput(): PartIdInput? {
        if (this == null) {
            return null
        }
        return PartIdInput(
            id = value,
            type = when (type) {
                PartIdType.PERSON -> PartType.FNR
                PartIdType.VIRKSOMHET -> PartType.ORGNR
            }
        )
    }

}