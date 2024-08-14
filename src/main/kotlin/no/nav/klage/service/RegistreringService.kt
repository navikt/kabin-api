package no.nav.klage.service

import no.nav.klage.api.controller.view.*
import no.nav.klage.api.controller.view.Address
import no.nav.klage.api.controller.view.BehandlingstidChangeRegistreringView.BehandlingstidChangeRegistreringOverstyringerView
import no.nav.klage.api.controller.view.DokumentReferanse.AvsenderMottaker.AvsenderMottakerIdType
import no.nav.klage.api.controller.view.ExistingAnkebehandling
import no.nav.klage.api.controller.view.MottattVedtaksinstansChangeRegistreringView.MottattVedtaksinstansChangeRegistreringOverstyringerView
import no.nav.klage.clients.SakFromKlanke
import no.nav.klage.clients.kabalapi.AnkemulighetFromKabal
import no.nav.klage.clients.kabalapi.BehandlingIsDuplicateInput
import no.nav.klage.clients.kabalapi.KabalApiClient
import no.nav.klage.clients.kabalapi.PartView
import no.nav.klage.domain.entities.*
import no.nav.klage.domain.entities.PartStatus
import no.nav.klage.exceptions.*
import no.nav.klage.kodeverk.*
import no.nav.klage.repository.RegistreringRepository
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.calculateFrist
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
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

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun createRegistrering(input: SakenGjelderValueInput): FullRegistreringView {
        val registrering = registreringRepository.save(
            Registrering(
                createdBy = tokenUtil.getCurrentIdent(),
                sakenGjelder = input.sakenGjelderValue?.let { sakenGjelderValue ->
                    PartId(
                        value = sakenGjelderValue,
                        type = PartIdType.PERSON
                    )
                },
                //defaults
                klager = null,
                fullmektig = null,
                avsender = null,
                journalpostId = null,
                type = null,
                mulighetId = null,
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
                muligheterFetched = null,
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

                reinitializeMuligheter()

                //empty the properties that no longer make sense if sakenGjelder changes.
                journalpostId = null
                ytelse = null
                type = null
                mulighetId = null
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

                if (type == Type.KLAGE) {
                    mottattVedtaksinstans = document.datoOpprettet.toLocalDate()
                } else if (type == Type.ANKE) {
                    mottattKlageinstans = document.datoOpprettet.toLocalDate()
                }

                //empty the properties that no longer make sense if journalpostId changes.
                fullmektig = null

                //TODO: Behold hvis klager kom fra muligheten.
                //Still correct? It is quite expensive to fetch mulighet, so we are waiting with this.
                klager = null

                //FIXME: Handle parts and receivers
                avsender =
                    if (document.journalposttype == DokumentReferanse.Journalposttype.I && document.avsenderMottaker != null) {
                        PartId(
                            value = document.avsenderMottaker.id,
                            type = when (document.avsenderMottaker.type) {
                                AvsenderMottakerIdType.FNR -> PartIdType.PERSON
                                AvsenderMottakerIdType.ORGNR -> PartIdType.VIRKSOMHET
                                AvsenderMottakerIdType.HPRNR -> TODO()
                                AvsenderMottakerIdType.UTL_ORG -> TODO()
                                AvsenderMottakerIdType.UKJENT -> TODO()
                                AvsenderMottakerIdType.NULL -> TODO()
                            }
                        )
                    } else {
                        null
                    }

                if (mulighetId != null) {
                    val mulighet = muligheter.find { it.id == mulighetId }
                        ?: throw MulighetNotFoundException("Valgt mulighet ikke funnet. Id: $mulighetId")
                    willCreateNewJournalpost = dokArkivService.journalpostIsFinalizedAndConnectedToFagsak(
                        journalpostId = journalpostId!!,
                        fagsakId = mulighet.fagsakId,
                        fagsystemId = mulighet.originalFagsystem.id,
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
                if (type == Type.KLAGE) {
                    mottattKlageinstans = null
                }

                mulighetId = null

                ytelse = null
                hjemmelIdList = listOf()

                //FIXME: Handle parts and receivers
                klager = null

                saksbehandlerIdent = null

                sendSvarbrev = null
                overrideSvarbrevBehandlingstid = false
                overrideSvarbrevCustomText = false
                svarbrevBehandlingstidUnits = null
                svarbrevBehandlingstidUnitType = null
                svarbrevCustomText = null

                oppgaveId = null

                willCreateNewJournalpost = false

            }.toTypeChangeRegistreringView()
    }

    fun Registrering.reinitializeMuligheter() {
        muligheter.clear()

        val klagemuligheterFromInfotrygd =
            klageService.getKlagemuligheterFromInfotrygd(IdnummerInput(idnummer = sakenGjelder!!.value))
        val klageTilbakebetalingMuligheterFromInfotrygd =
            klageService.getKlageTilbakebetalingMuligheterFromInfotrygd(IdnummerInput(idnummer = sakenGjelder!!.value))
        val ankemuligheterFromInfotrygd =
            ankeService.getAnkemuligheterFromInfotrygd(IdnummerInput(idnummer = sakenGjelder!!.value))

        val ankemuligheterFromKabal =
            ankeService.getAnkemuligheterFromKabal(IdnummerInput(idnummer = sakenGjelder!!.value))

        val muligheterFromInfotrygd = mutableListOf<SakFromKlanke>()
        val muligheterFromKabal = mutableListOf<AnkemulighetFromKabal>()

        var start = System.currentTimeMillis()

        Flux.merge(
            klagemuligheterFromInfotrygd,
            klageTilbakebetalingMuligheterFromInfotrygd,
            ankemuligheterFromInfotrygd,
            ankemuligheterFromKabal,
        ).parallel()
            .runOn(Schedulers.parallel())
            .doOnNext { mulighetList ->
                mulighetList.forEach { mulighet ->
                    if (mulighet is SakFromKlanke) {
                        muligheterFromInfotrygd.add(mulighet)
                    } else if (mulighet is AnkemulighetFromKabal) {
                        muligheterFromKabal.add(mulighet)
                    }
                }
            }
            .sequential()
            .blockLast()

        logger.debug("Time to merge muligheter: " + (System.currentTimeMillis() - start))

        start = System.currentTimeMillis()

        val behandlingIsDuplicateResponses = Flux.fromIterable(muligheterFromInfotrygd)
            .parallel()
            .runOn(Schedulers.parallel())
            .flatMap { mulighetFromInfotrygd ->
                kabalApiClient.checkBehandlingDuplicateInKabal(
                    input = BehandlingIsDuplicateInput(
                        fagsystemId = Fagsystem.IT01.id,
                        kildereferanse = mulighetFromInfotrygd.sakId,
                        typeId = if (mulighetFromInfotrygd.sakstype.startsWith("KLAGE")) Type.KLAGE.id else Type.ANKE.id
                    )
                )
            }
            .sequential()
            .toIterable()

        logger.debug("Time to check duplicates: " + (System.currentTimeMillis() - start))

        logger.debug("Found ${muligheterFromInfotrygd.size} muligheter from Infotrygd.")
        logger.debug("Found ${muligheterFromKabal.size} muligheter from Kabal.")

        muligheter.addAll(muligheterFromInfotrygd
            .filter { mulighetFromInfotrygd ->
                !behandlingIsDuplicateResponses.first {
                    //enough?
                    it.kildereferanse == mulighetFromInfotrygd.sakId
                }.duplicate
            }
            .map { it.toMulighet() })

        muligheter.addAll(muligheterFromKabal.map { it.toMulighet() })
        muligheterFetched = LocalDateTime.now()
    }

    fun setMulighet(registreringId: UUID, input: MulighetInput): MulighetChangeRegistreringView {
        return getRegistreringForUpdate(registreringId)
            .apply {
                mulighetId = input.mulighetId

                val newMulighet = muligheter.find { it.id == input.mulighetId }
                    ?: throw MulighetNotFoundException("Mulighet ikke funnet.")

                val previousYtelse = ytelse
                val currentYtelseCandidates = getYtelseOrNull(newMulighet)
                if (previousYtelse != null && previousYtelse in currentYtelseCandidates) {
                    //don't change ytelse if it is still valid.
                } else if (currentYtelseCandidates.size == 1) {
                    ytelse = currentYtelseCandidates.first()
                } else {
                    ytelse = null
                }

                if ((previousYtelse != null && ytelse != null && previousYtelse != ytelse)
                    || (previousYtelse == null && ytelse != null)
                ) {
                    //set svarbrev settings (and reset old) for the new ytelse
                    setSvarbrevSettings()
                    svarbrevReceivers.clear()
                    hjemmelIdList = emptyList()

                    //Could be smarter here.
                    saksbehandlerIdent = null
                } else if (ytelse == null) {
                    //empty the properties that no longer make sense if ytelse is null.
                    sendSvarbrev = false
                    svarbrevCustomText = null
                    svarbrevBehandlingstidUnits = null
                    svarbrevBehandlingstidUnitType = null
                    overrideSvarbrevBehandlingstid = false
                    overrideSvarbrevCustomText = false

                    svarbrevReceivers.clear()

                    hjemmelIdList = emptyList()

                    saksbehandlerIdent = null
                }

                if (type == Type.KLAGE) {
                    mottattKlageinstans = newMulighet.vedtakDate
                } else if (type == Type.ANKE) {
                    //FIXME: Handle parts and receivers
                    klager = newMulighet.klager?.part
                }

                modified = LocalDateTime.now()

                willCreateNewJournalpost = dokArkivService.journalpostIsFinalizedAndConnectedToFagsak(
                    journalpostId = this.journalpostId!!,
                    fagsakId = newMulighet.fagsakId,
                    fagsystemId = newMulighet.originalFagsystem.id,
                )

                //What about fullmektig?
            }.toMulighetChangeRegistreringView()
    }

    private fun Registrering.setSvarbrevSettings() {
        val svarbrevSettings = getSvarbrevSettings()
        sendSvarbrev = svarbrevSettings.shouldSend
        svarbrevCustomText = svarbrevSettings.customText
        svarbrevBehandlingstidUnits = svarbrevSettings.behandlingstidUnits
        svarbrevBehandlingstidUnitType = svarbrevSettings.behandlingstidUnitType
        overrideSvarbrevBehandlingstid = false
        overrideSvarbrevCustomText = false
    }

    private fun getYtelseOrNull(mulighet: Mulighet?): List<Ytelse> {
        if (mulighet == null) {
            return emptyList()
        }

        if (mulighet.ytelse != null) {
            return listOf(mulighet.ytelse)
        } else {
            val possibleYtelser = Ytelse.entries.filter { it.toTema() == mulighet.tema }
            return possibleYtelser
        }
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
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                ytelse = input.ytelseId?.let { ytelseId ->
                    Ytelse.of(ytelseId)
                }
                modified = LocalDateTime.now()

                setSvarbrevSettings()

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
            svarbrev = YtelseChangeRegistreringView.YtelseChangeRegistreringSvarbrevView(
                send = registrering.sendSvarbrev,
                behandlingstid = if (registrering.svarbrevBehandlingstidUnits != null) {
                    BehandlingstidView(
                        unitTypeId = registrering.svarbrevBehandlingstidUnitType!!.id,
                        units = registrering.svarbrevBehandlingstidUnits!!
                    )
                } else null,
                fullmektigFritekst = registrering.svarbrevFullmektigFritekst,
                receivers = mapTorecipientViews(registrering),
                overrideCustomText = registrering.overrideSvarbrevCustomText,
                overrideBehandlingstid = registrering.overrideSvarbrevBehandlingstid,
                customText = registrering.svarbrevCustomText,
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
                receivers = mapTorecipientViews(registrering)
            ),
            overstyringer = FullmektigChangeRegistreringView.FullmektigChangeRegistreringOverstyringerView(
                fullmektig = registrering.fullmektig?.let {
                    registrering.partViewWithOptionalUtsendingskanal(
                        identifikator = it.value
                    )
                }
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
                receivers = mapTorecipientViews(registrering)
            ),
            overstyringer = KlagerChangeRegistreringView.KlagerChangeRegistreringViewRegistreringOverstyringerView(
                klager = registrering.klager?.let { registrering.partViewWithOptionalUtsendingskanal(identifikator = it.value) }
            ),
            modified = registrering.modified,
        )
    }

    private fun SvarbrevReceiver.toRecipientView(
        registrering: Registrering
    ) = RecipientView(
        id = id,
        part = registrering.partViewWithOptionalUtsendingskanal(identifikator = part.value),
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
                receivers = mapTorecipientViews(registrering)
            ),
            overstyringer = AvsenderChangeRegistreringView.AvsenderChangeRegistreringViewRegistreringOverstyringerView(
                avsender = registrering.avsender?.let { registrering.partViewWithOptionalUtsendingskanal(identifikator = it.value) }
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
                calculatedFrist = if (registrering.mottattKlageinstans != null && registrering.svarbrevBehandlingstidUnits != null) {
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
                receivers = mapTorecipientViews(registrering)
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
                                poststed = address.poststed,
                            )
                        }
                    )
                )
                modified = LocalDateTime.now()
            }
        return SvarbrevReceiverChangeRegistreringView(
            id = registrering.id,
            svarbrev = SvarbrevReceiverChangeRegistreringView.SvarbrevReceiverChangeRegistreringSvarbrevView(
                receivers = mapTorecipientViews(registrering)
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
                            poststed = address.poststed,
                        )
                    }
                }
                modified = LocalDateTime.now()
            }
        return SvarbrevReceiverChangeRegistreringView(
            id = registrering.id,
            svarbrev = SvarbrevReceiverChangeRegistreringView.SvarbrevReceiverChangeRegistreringSvarbrevView(
                receivers = mapTorecipientViews(registrering)
            ),
            modified = registrering.modified,
        )
    }

    private fun mapTorecipientViews(registrering: Registrering) =
        registrering.svarbrevReceivers.map { receiver ->
            receiver.toRecipientView(registrering)
        }.sortedBy { it.part.name }

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
            mulighet = mulighetId?.let {
                MulighetIdView(
                    id = it,
                )
            },
            overstyringer = MulighetChangeRegistreringView.MulighetChangeRegistreringOverstyringerView(
                ytelseId = ytelse?.id,
                mottattVedtaksinstans = mottattVedtaksinstans,
                mottattKlageinstans = mottattKlageinstans,
                behandlingstid = BehandlingstidView(
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
                fullmektig = fullmektig?.let { partViewWithOptionalUtsendingskanal(identifikator = it.value) },
                klager = klager?.let { partViewWithOptionalUtsendingskanal(identifikator = it.value) },
                avsender = avsender?.let { partViewWithOptionalUtsendingskanal(identifikator = it.value) },
                saksbehandlerIdent = saksbehandlerIdent,
                oppgaveId = oppgaveId,

                ),
            svarbrev = MulighetChangeRegistreringView.MulighetChangeRegistreringSvarbrevView(
                send = sendSvarbrev,
                behandlingstid = if (svarbrevBehandlingstidUnits != null) {
                    BehandlingstidView(
                        unitTypeId = svarbrevBehandlingstidUnitType!!.id,
                        units = svarbrevBehandlingstidUnits!!
                    )
                } else null,
                fullmektigFritekst = svarbrevFullmektigFritekst,
                receivers = mapTorecipientViews(this),
                overrideCustomText = overrideSvarbrevCustomText,
                overrideBehandlingstid = overrideSvarbrevBehandlingstid,
                customText = svarbrevCustomText,
            ),
            modified = modified,
            willCreateNewJournalpost = willCreateNewJournalpost,
        )
    }

    private fun Registrering.toRegistreringView() = FullRegistreringView(
        id = id,
        journalpostId = journalpostId,
        sakenGjelderValue = sakenGjelder?.value,
        typeId = type?.id,
        mulighet = mulighetId?.let {
            MulighetIdView(
                id = it,
            )
        },
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
            fullmektig = fullmektig?.let { partViewWithOptionalUtsendingskanal(identifikator = it.value) },
            klager = klager?.let { partViewWithOptionalUtsendingskanal(identifikator = it.value) },
            avsender = avsender?.let { partViewWithOptionalUtsendingskanal(identifikator = it.value) },
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
            receivers = mapTorecipientViews(this),
            title = svarbrevTitle,
            customText = svarbrevCustomText,
            overrideCustomText = overrideSvarbrevCustomText,
            overrideBehandlingstid = overrideSvarbrevBehandlingstid,
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
        klagemuligheter = muligheter.filter { it.type == Type.KLAGE }.map { mulighet ->
            mulighet.toKlagemulighetView()
        },
        ankemuligheter = muligheter.filter { it.type == Type.ANKE }.map { mulighet ->
            mulighet.toAnkemulighetView()
        },
        muligheterFetched = muligheterFetched,
    )

    fun deleteRegistrering(registreringId: UUID) {
        //check rights
        getRegistreringForUpdate(registreringId)
        registreringRepository.deleteById(registreringId)
    }

    fun validateRegistrering(registreringId: UUID) {
        val registrering = registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering ikke funnet.") }
        TODO()
    }

    private fun Registrering.partViewWithOptionalUtsendingskanal(identifikator: String): PartViewWithOptionalUtsendingskanal =
        if (ytelse != null) {
            kabalApiClient.searchPartWithUtsendingskanal(
                searchPartInput = SearchPartWithUtsendingskanalInput(
                    identifikator = identifikator,
                    sakenGjelderId = sakenGjelder!!.value,
                    ytelseId = ytelse!!.id
                )
            ).partViewWithOptionalUtsendingskanal()
        } else {
            kabalApiClient.searchPart(
                searchPartInput = SearchPartInput(
                    identifikator = identifikator,
                )
            ).partViewWithOptionalUtsendingskanal()
        }

    private fun no.nav.klage.clients.kabalapi.PartViewWithUtsendingskanal.partViewWithOptionalUtsendingskanal(): PartViewWithOptionalUtsendingskanal {
        return PartViewWithOptionalUtsendingskanal(
            id = id,
            type = PartType.valueOf(type.name),
            name = name,
            available = available,
            statusList = statusList.map { partStatus ->
                no.nav.klage.api.controller.view.PartStatus(
                    status = no.nav.klage.api.controller.view.PartStatus.Status.valueOf(partStatus.status.name),
                    date = partStatus.date,
                )
            },
            address = address?.let {
                Address(
                    adresselinje1 = it.adresselinje1,
                    adresselinje2 = it.adresselinje2,
                    adresselinje3 = it.adresselinje3,
                    landkode = it.landkode,
                    postnummer = it.postnummer,
                    poststed = it.poststed,
                )
            },
            language = language,
            utsendingskanal = utsendingskanal,
        )
    }

    private fun PartView.partViewWithOptionalUtsendingskanal(): PartViewWithOptionalUtsendingskanal {
        return PartViewWithOptionalUtsendingskanal(
            id = id,
            type = PartType.valueOf(type.name),
            name = name,
            available = available,
            statusList = statusList.map { partStatus ->
                no.nav.klage.api.controller.view.PartStatus(
                    status = no.nav.klage.api.controller.view.PartStatus.Status.valueOf(partStatus.status.name),
                    date = partStatus.date,
                )
            },
            address = address?.let {
                Address(
                    adresselinje1 = it.adresselinje1,
                    adresselinje2 = it.adresselinje2,
                    adresselinje3 = it.adresselinje3,
                    landkode = it.landkode,
                    postnummer = it.postnummer,
                    poststed = it.poststed,
                )
            },
            language = language,
            utsendingskanal = null,
        )
    }

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

        val mulighet = registrering.mulighetId?.let { mulighetId ->
            registrering.muligheter.find { it.id == mulighetId }
        } ?: throw IllegalInputException("Muligheten som registreringen refererer til finnes ikke.")

        val response: CreatedBehandlingResponse = when (registrering.type) {
            Type.ANKE -> {
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
                        vedtak = Vedtak(
                            id = mulighet.currentFagystemTechnicalId,
                            sourceId = mulighet.currentFagsystem.id,
                        ),
                        oppgaveId = registrering.oppgaveId,
                    )
                )
            }

            Type.KLAGE -> {
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
                        vedtak = Vedtak(
                            id = mulighet.currentFagystemTechnicalId,
                            sourceId = mulighet.currentFagsystem.id,
                        ),
                        oppgaveId = registrering.oppgaveId,
                    )
                )
            }

            else -> {
                throw IllegalInputException("Registreringen er av en type som ikke støttes: ${registrering.type}.")
            }
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
            customText = if (overrideSvarbrevCustomText) svarbrevCustomText else svarbrevSettings.customText,
            receivers = svarbrevReceivers.map { receiver ->
                SvarbrevWithReceiverInput.Receiver(
                    id = receiver.part.value,
                    handling = receiver.handling?.let { SvarbrevWithReceiverInput.Receiver.HandlingEnum.valueOf(it.name) },
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
            varsletBehandlingstidUnits = if (overrideSvarbrevBehandlingstid) svarbrevBehandlingstidUnits!! else svarbrevSettings.behandlingstidUnits,
            varsletBehandlingstidUnitType = if (overrideSvarbrevBehandlingstid) svarbrevBehandlingstidUnitType!! else svarbrevSettings.behandlingstidUnitType,
            varsletBehandlingstidUnitTypeId = if (overrideSvarbrevBehandlingstid) svarbrevBehandlingstidUnitType!!.id else svarbrevSettings.behandlingstidUnitType.id,
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

    fun getMuligheter(registreringId: UUID): Muligheter {
        val registrering = getRegistreringForUpdate(registreringId)
        if (registrering.muligheter.isEmpty()) {
            registrering.reinitializeMuligheter()
        }
        val (klagemuligheter, ankemuligheter) = registrering.muligheter.partition { it.type == Type.KLAGE }

        val klagemuligheterView = klagemuligheter.map { klagemulighet ->
            klagemulighet.toKlagemulighetView()
        }

        val ankemuligheterView = ankemuligheter.map { ankemulighet ->
            ankemulighet.toAnkemulighetView()
        }

        return Muligheter(
            klagemuligheter = klagemuligheterView,
            ankemuligheter = ankemuligheterView,
            muligheterFetched = registrering.muligheterFetched!!,
        )
    }

    private fun PartWithUtsendingskanal?.toPartViewWithUtsendingskanal(partStatusList: Set<PartStatus>): PartViewWithUtsendingskanal? {
        return this?.let {
            return PartViewWithUtsendingskanal(
                id = part.value,
                type = when (part.type) {
                    PartIdType.PERSON -> PartType.FNR
                    PartIdType.VIRKSOMHET -> PartType.ORGNR
                },
                name = name,
                available = available ?: false,
                statusList = partStatusList.map { partStatus ->
                    no.nav.klage.api.controller.view.PartStatus(
                        status = no.nav.klage.api.controller.view.PartStatus.Status.valueOf(partStatus.status.name),
                        date = partStatus.date,
                    )
                },
                address = address?.let {
                    Address(
                        adresselinje1 = it.adresselinje1,
                        adresselinje2 = it.adresselinje2,
                        adresselinje3 = it.adresselinje3,
                        landkode = it.landkode!!,
                        postnummer = it.postnummer,
                        poststed = it.poststed,
                    )
                },
                language = language,
                utsendingskanal = Utsendingskanal.valueOf(utsendingskanal!!.name),
            )
        }
    }

    private fun AnkemulighetFromKabal.toMulighet(): Mulighet {
        val ytelse = Ytelse.of(ytelseId)
        return Mulighet(
            type = Type.ANKE,
            tema = ytelse.toTema(),
            vedtakDate = vedtakDate.toLocalDate(),
            sakenGjelder = sakenGjelder.toPartWithUtsendingskanal()!!,
            fagsakId = fagsakId,
            originalFagsystem = fagsystem,
            currentFagsystem = Fagsystem.KABAL,
            ytelse = ytelse,
            hjemmelIdList = hjemmelIdList,
            klager = klager.toPartWithUtsendingskanal(),
            fullmektig = fullmektig.toPartWithUtsendingskanal(),
            previousSaksbehandlerIdent = tildeltSaksbehandlerIdent,
            previousSaksbehandlerName = tildeltSaksbehandlerNavn,
            sourceOfExistingAnkebehandling = sourceOfExistingAnkebehandling.map {
                no.nav.klage.domain.entities.ExistingAnkebehandling(
                    ankebehandlingId = it.id,
                    created = it.created,
                    completed = it.completed,
                )
            }.toMutableSet(),
            klageBehandlendeEnhet = null,
            currentFagystemTechnicalId = behandlingId.toString(),
        )
    }

    private fun SakFromKlanke.toMulighet(): Mulighet {
        return Mulighet(
            type = if (sakstype.startsWith("KLAGE")) Type.KLAGE else Type.ANKE,
            tema = Tema.valueOf(tema),
            vedtakDate = vedtaksdato,
            sakenGjelder = kabalApiClient.searchPartWithUtsendingskanal(
                SearchPartWithUtsendingskanalInput(
                    identifikator = fnr,
                    sakenGjelderId = fnr,
                    //don't care which ytelse is picked, as long as Tema is correct. Could be prettier.
                    ytelseId = Ytelse.entries.find { y -> y.toTema().navn == tema }!!.id,
                )
            ).toPartWithUtsendingskanal()!!,
            fagsakId = fagsakId,
            originalFagsystem = Fagsystem.IT01,
            currentFagsystem = Fagsystem.IT01,
            ytelse = null,
            klager = null,
            fullmektig = null,
            klageBehandlendeEnhet = enhetsnummer,
            currentFagystemTechnicalId = sakId,
            previousSaksbehandlerIdent = null,
            previousSaksbehandlerName = null,
            hjemmelIdList = emptyList(),
        )
    }

    private fun no.nav.klage.clients.kabalapi.PartViewWithUtsendingskanal?.toPartWithUtsendingskanal(): PartWithUtsendingskanal? {
        return this?.let {
            PartWithUtsendingskanal(
                part = PartId(
                    value = id,
                    type = when (type) {
                        no.nav.klage.clients.kabalapi.PartType.FNR -> PartIdType.PERSON
                        no.nav.klage.clients.kabalapi.PartType.ORGNR -> PartIdType.VIRKSOMHET
                    },
                ),
                name = name,
                available = available,
                address = address?.let {
                    no.nav.klage.domain.entities.Address(
                        adresselinje1 = it.adresselinje1,
                        adresselinje2 = it.adresselinje2,
                        adresselinje3 = it.adresselinje3,
                        landkode = it.landkode,
                        postnummer = it.postnummer,
                        poststed = it.poststed,
                    )
                },
                language = language,
                utsendingskanal = no.nav.klage.domain.entities.PartWithUtsendingskanal.Utsendingskanal.valueOf(
                    utsendingskanal.name
                ),
            )
        }
    }

    private fun Mulighet.toKlagemulighetView() =
        KlagemulighetView(
            id = id,
            temaId = tema.id,
            vedtakDate = vedtakDate!!,
            sakenGjelder = sakenGjelder.toPartViewWithUtsendingskanal(sakenGjelderStatusList)!!,
            fagsakId = fagsakId,
            originalFagsystemId = originalFagsystem.id,
            currentFagsystemId = currentFagsystem.id,
            typeId = type.id,
            klageBehandlendeEnhet = klageBehandlendeEnhet!!,
        )

    private fun Mulighet.toAnkemulighetView(): AnkemulighetView =
        AnkemulighetView(
            id = id,
            temaId = tema.id,
            vedtakDate = vedtakDate!!,
            sakenGjelder = sakenGjelder.toPartViewWithUtsendingskanal(sakenGjelderStatusList)!!,
            fagsakId = fagsakId,
            originalFagsystemId = originalFagsystem.id,
            currentFagsystemId = currentFagsystem.id,
            typeId = type.id,
            sourceOfExistingAnkebehandling = sourceOfExistingAnkebehandling.map {
                ExistingAnkebehandling(
                    id = it.ankebehandlingId,
                    created = it.created,
                    completed = it.completed,
                )
            },
            ytelseId = ytelse?.id,
            hjemmelIdList = hjemmelIdList,
            klager = klager.toPartViewWithUtsendingskanal(klagerStatusList),
            fullmektig = fullmektig.toPartViewWithUtsendingskanal(fullmektigStatusList),
            previousSaksbehandler = previousSaksbehandlerIdent?.let {
                PreviousSaksbehandler(
                    navIdent = it,
                    navn = previousSaksbehandlerName ?: "navn mangler for $it",
                )
            },
        )
}