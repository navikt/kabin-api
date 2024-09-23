package no.nav.klage.service

import no.nav.klage.api.controller.view.*
import no.nav.klage.api.controller.view.BehandlingstidChangeRegistreringView.BehandlingstidChangeRegistreringOverstyringerView
import no.nav.klage.api.controller.view.DokumentReferanse.AvsenderMottaker.AvsenderMottakerIdType
import no.nav.klage.api.controller.view.MottattVedtaksinstansChangeRegistreringView.MottattVedtaksinstansChangeRegistreringOverstyringerView
import no.nav.klage.clients.SakFromKlanke
import no.nav.klage.clients.kabalapi.BehandlingIsDuplicateInput
import no.nav.klage.clients.kabalapi.KabalApiClient
import no.nav.klage.clients.kabalapi.MulighetFromKabal
import no.nav.klage.domain.entities.Mulighet
import no.nav.klage.domain.entities.PartId
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.domain.entities.SvarbrevReceiver
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
    private val omgjoeringskravService: OmgjoeringskravService,
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
                journalpostDatoOpprettet = null,
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

        registrering.reinitializeMuligheter()
        registrering.handleSvarbrevReceivers()

        return registrering.toRegistreringView(kabalApiClient = kabalApiClient)
    }

    fun getRegistrering(registreringId: UUID): FullRegistreringView {
        return registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering ikke funnet.") }
            .toRegistreringView(kabalApiClient = kabalApiClient)
    }

    fun getFerdigeRegistreringer(
        sidenDager: Int?,
    ): List<FinishedRegistreringView> {
        return registreringRepository.findFerdigeRegistreringer(
            navIdent = tokenUtil.getCurrentIdent(),
            finishedFrom = LocalDateTime.now().minusDays(sidenDager?.toLong() ?: 31)
        ).map { it.toFinishedRegistreringView() }.sortedByDescending { it.finished }
    }

    fun getUferdigeRegistreringer(
    ): List<FullRegistreringView> {
        return registreringRepository.findUferdigeRegistreringer(
            navIdent = tokenUtil.getCurrentIdent(),
        ).map { it.toRegistreringView(kabalApiClient = kabalApiClient) }.sortedByDescending { it.created }
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
                mulighetId = null
                reinitializeMuligheter()

                journalpostId = null
                ytelse = null
                type = null
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

                handleSvarbrevReceivers()
            }
        return registrering.toRegistreringView(kabalApiClient = kabalApiClient)
    }

    fun setJournalpostId(registreringId: UUID, input: JournalpostIdInput): FullRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                journalpostId = input.journalpostId
                modified = LocalDateTime.now()

                val document = documentService.fetchDokument(
                    journalpostId = input.journalpostId
                )

                journalpostDatoOpprettet = document.datoOpprettet.toLocalDate()

                //empty the properties that no longer make sense if journalpostId changes.

                fullmektig = null
                svarbrevFullmektigFritekst = null

                //Slett klager hvis klager ikke kom fra muligheten

                val mulighet = muligheter.find { it.id == mulighetId }

                if (mulighet != null) {
                    when (type) {
                        Type.KLAGE -> {
                            mottattVedtaksinstans = journalpostDatoOpprettet
                        }

                        Type.ANKE, Type.OMGJOERINGSKRAV -> {
                            mottattKlageinstans = journalpostDatoOpprettet
                        }

                        else -> {} //do nothing
                    }
                }

//                if ((mulighet == null) || klager != null && klager?.value != mulighet.klager?.part?.value) {
//                    handleReceiversWhenChangingPart(
//                        unchangedRegistrering = this,
//                        partIdInput = null,
//                        partISaken = PartISaken.KLAGER,
//                    )
//                    klager = null
//                }

                val avsenderPartId =
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

                avsender = avsenderPartId

                if (mulighet != null) {
                    willCreateNewJournalpost = dokArkivService.journalpostIsFinalizedAndConnectedToFagsak(
                        journalpostId = journalpostId!!,
                        fagsakId = mulighet.fagsakId,
                        fagsystemId = mulighet.originalFagsystem.id,
                    )
                }

                handleSvarbrevReceivers()
            }

        return registrering.toRegistreringView(kabalApiClient = kabalApiClient)
    }

    fun setTypeId(registreringId: UUID, input: TypeIdInput): TypeChangeRegistreringView {
        return getRegistreringForUpdate(registreringId)
            .apply {
                type = input.typeId?.let { typeId ->
                    Type.of(typeId)
                }
                modified = LocalDateTime.now()

                //empty the properties that no longer make sense if typeId changes.
                mottattKlageinstans = null
                mottattVedtaksinstans = null

                mulighetId = null

                ytelse = null
                hjemmelIdList = listOf()

                saksbehandlerIdent = null

                sendSvarbrev = null
                overrideSvarbrevBehandlingstid = false
                overrideSvarbrevCustomText = false
                svarbrevBehandlingstidUnits = null
                svarbrevBehandlingstidUnitType = null
                svarbrevCustomText = null

                oppgaveId = null

                willCreateNewJournalpost = false

            }.toTypeChangeRegistreringView(kabalApiClient = kabalApiClient)
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

                    hjemmelIdList = newMulighet.hjemmelIdList.ifEmpty {
                        emptyList()
                    }

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

                    hjemmelIdList = emptyList()

                    saksbehandlerIdent = null
                }

                if (type == Type.KLAGE) {
                    mottattKlageinstans = newMulighet.vedtakDate
                    mottattVedtaksinstans = journalpostDatoOpprettet
                } else if (type in listOf(Type.ANKE, Type.OMGJOERINGSKRAV)) {
                    handleReceiversWhenChangingPart(
                        unchangedRegistrering = this,
                        partIdInput = newMulighet.klager?.part.toPartIdInput(),
                        partISaken = PartISaken.KLAGER,
                    )
                    klager = newMulighet.klager?.part
                    mottattKlageinstans = journalpostDatoOpprettet
                    mottattVedtaksinstans = null
                    handleSvarbrevReceivers()
                }

                modified = LocalDateTime.now()

                willCreateNewJournalpost = dokArkivService.journalpostIsFinalizedAndConnectedToFagsak(
                    journalpostId = this.journalpostId!!,
                    fagsakId = newMulighet.fagsakId,
                    fagsystemId = newMulighet.originalFagsystem.id,
                )

                //What about fullmektig?
            }.toMulighetChangeRegistreringView(kabalApiClient = kabalApiClient)
    }

    fun Registrering.reinitializeMuligheter() {
        val input = IdnummerInput(idnummer = sakenGjelder!!.value)

        val klagemuligheterFromInfotrygdMono = klageService.getKlagemuligheterFromInfotrygdAsMono(input)
        val klageTilbakebetalingMuligheterFromInfotrygdMono =
            klageService.getKlageTilbakebetalingMuligheterFromInfotrygdAsMono(input)
        val ankemuligheterFromInfotrygdMono = ankeService.getAnkemuligheterFromInfotrygdAsMono(input)

        val ankemuligheterFromKabalMono = ankeService.getAnkemuligheterFromKabalAsMono(input)
        val omgjoeringskravmuligheterFromKabalMono = omgjoeringskravService.getOmgjoeringskravmuligheterFromKabalAsMono(input)

        val muligheterFromInfotrygd = mutableListOf<SakFromKlanke>()
        val muligheterFromKabal = mutableListOf<MulighetFromKabal>()

        var start = System.currentTimeMillis()
        var mulighetStart = System.currentTimeMillis()

        Flux.merge(
            klagemuligheterFromInfotrygdMono,
            klageTilbakebetalingMuligheterFromInfotrygdMono,
            ankemuligheterFromInfotrygdMono,
            ankemuligheterFromKabalMono,
            omgjoeringskravmuligheterFromKabalMono,
        ).parallel()
            .runOn(Schedulers.parallel())
            .doOnNext { mulighetList ->
                logger.debug("Time to fetch mulighet: " + (System.currentTimeMillis() - mulighetStart))
                mulighetStart = System.currentTimeMillis()
                mulighetList.forEach { mulighet ->
                    if (mulighet is SakFromKlanke) {
                        muligheterFromInfotrygd.add(mulighet)
                    } else if (mulighet is MulighetFromKabal) {
                        muligheterFromKabal.add(mulighet)
                    }
                }
            }
            .sequential()
            .blockLast()

        logger.debug("Time to merge muligheter: " + (System.currentTimeMillis() - start))

        start = System.currentTimeMillis()

        var duplicateCheckStart = System.currentTimeMillis()

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
                ).also {
                    logger.debug("Time to check duplicate: " + (System.currentTimeMillis() - duplicateCheckStart))
                    duplicateCheckStart = System.currentTimeMillis()
                }
            }
            .sequential()
            .toIterable()

        logger.debug("Time to check duplicates: " + (System.currentTimeMillis() - start))

        logger.debug("Found ${muligheterFromInfotrygd.size} muligheter from Infotrygd.")
        logger.debug("Found ${muligheterFromKabal.size} muligheter from Kabal.")

        val filteredInfotrygdMuligheter = muligheterFromInfotrygd
            .filter { mulighetFromInfotrygd ->
                !behandlingIsDuplicateResponses.first {
                    //enough?
                    it.kildereferanse == mulighetFromInfotrygd.sakId
                }.duplicate
            }

        var muligheterToStoreInDB =
            filteredInfotrygdMuligheter.map { it.toMulighet(kabalApiClient = kabalApiClient) } + muligheterFromKabal.map { it.toMulighet() }

        //Keep chosen mulighet, if it is still valid, and update accordingly.
        if (mulighetId == null) {
            muligheter.clear()
        } else {
            val previouslyChosenMulighet = muligheter.find { it.id == mulighetId }
            muligheter.removeIf {
                !(it.currentFagystemTechnicalId == previouslyChosenMulighet?.currentFagystemTechnicalId
                        && it.currentFagsystem == previouslyChosenMulighet.currentFagsystem)
            }
            muligheterToStoreInDB = muligheterToStoreInDB.filter {
                !(it.currentFagystemTechnicalId == previouslyChosenMulighet?.currentFagystemTechnicalId
                        && it.currentFagsystem == previouslyChosenMulighet.currentFagsystem)
            }
        }

        muligheter.addAll(muligheterToStoreInDB)

        muligheterFetched = LocalDateTime.now()
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
                receivers = registrering.mapToRecipientViews(),
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
                handleReceiversWhenChangingPart(
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
                handleSvarbrevReceivers()
            }
        return FullmektigChangeRegistreringView(
            id = registrering.id,
            svarbrev = FullmektigChangeRegistreringView.FullmektigChangeSvarbrevView(
                fullmektigFritekst = registrering.svarbrevFullmektigFritekst,
                receivers = registrering.mapToRecipientViews()
            ),
            overstyringer = FullmektigChangeRegistreringView.FullmektigChangeRegistreringOverstyringerView(
                fullmektig = registrering.fullmektig?.let {
                    registrering.partViewWithOptionalUtsendingskanal(
                        identifikator = it.value,
                        kabalApiClient = kabalApiClient,
                    )
                }
            ),
            modified = registrering.modified,
        )
    }

    fun handleReceiversWhenChangingPart(
        unchangedRegistrering: Registrering,
        partIdInput: PartIdInput?,
        partISaken: PartISaken
    ) {
        val svarbrevReceivers = unchangedRegistrering.svarbrevReceivers

        //if there is only one receiver, and it is the same as the sakenGjelder (default set), clear it.
        if (partIdInput != null && svarbrevReceivers.size == 1 && svarbrevReceivers.first().part.value == unchangedRegistrering.sakenGjelder?.value && partIdInput.id != unchangedRegistrering.sakenGjelder?.value) {
            svarbrevReceivers.clear()
        }

        val existingParts = listOf(
            unchangedRegistrering.sakenGjelder?.value,
            unchangedRegistrering.klager?.value,
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
                handleReceiversWhenChangingPart(
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
                handleSvarbrevReceivers()
            }
        return KlagerChangeRegistreringView(
            id = registrering.id,
            svarbrev = KlagerChangeRegistreringView.KlagerChangeRegistreringViewSvarbrevView(
                receivers = registrering.mapToRecipientViews()
            ),
            overstyringer = KlagerChangeRegistreringView.KlagerChangeRegistreringViewRegistreringOverstyringerView(
                klager = registrering.klager?.let {
                    registrering.partViewWithOptionalUtsendingskanal(
                        identifikator = it.value,
                        kabalApiClient = kabalApiClient
                    )
                }
            ),
            modified = registrering.modified,
        )
    }

    fun setAvsender(registreringId: UUID, input: AvsenderInput): AvsenderChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                //cases
                //1. avsender is set to the same value as before
                if (avsender?.value == input.avsender?.id) {
                    return@apply
                }

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
                receivers = registrering.mapToRecipientViews()
            ),
            overstyringer = AvsenderChangeRegistreringView.AvsenderChangeRegistreringViewRegistreringOverstyringerView(
                avsender = registrering.avsender?.let {
                    registrering.partViewWithOptionalUtsendingskanal(
                        identifikator = it.value,
                        kabalApiClient = kabalApiClient
                    )
                }
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
                receivers = registrering.mapToRecipientViews()
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
                    //if the receiver is already in the list, we don't need to do anything.
                } else {
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
            }
        return SvarbrevReceiverChangeRegistreringView(
            id = registrering.id,
            svarbrev = SvarbrevReceiverChangeRegistreringView.SvarbrevReceiverChangeRegistreringSvarbrevView(
                receivers = registrering.mapToRecipientViews()
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
                receivers = registrering.mapToRecipientViews()
            ),
            modified = registrering.modified,
        )
    }

    private fun Registrering.mapToRecipientViews() =
        svarbrevReceivers.map { receiver ->
            receiver.toRecipientView(registrering = this, kabalApiClient = kabalApiClient)
        }.sortedBy { it.part.name }

    fun deleteRegistrering(registreringId: UUID) {
        //check rights
        getRegistreringForUpdate(registreringId)
        registreringRepository.deleteById(registreringId)
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
                    input = CreateAnkeInputView(
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
                        svarbrevInput = registrering.toSvarbrevWithReceiverInput(registrering.getSvarbrevSettings()),
                        vedtak = Vedtak(
                            id = mulighet.currentFagystemTechnicalId,
                            sourceId = mulighet.currentFagsystem.id,
                        ),
                        oppgaveId = registrering.oppgaveId,
                    ),
                    ankemulighet = mulighet,
                )
            }

            Type.KLAGE -> {
                klageService.createKlage(
                    input = CreateKlageInputView(
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
                        svarbrevInput = registrering.toSvarbrevWithReceiverInput(registrering.getSvarbrevSettings()),
                        vedtak = Vedtak(
                            id = mulighet.currentFagystemTechnicalId,
                            sourceId = mulighet.currentFagsystem.id,
                        ),
                        oppgaveId = registrering.oppgaveId,
                    ),
                    klagemulighet = mulighet,
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

    fun getMuligheter(registreringId: UUID): MuligheterView {
        val registrering = getRegistreringForUpdate(registreringId)
        registrering.reinitializeMuligheter()

        val klagemuligheter = mutableListOf<Mulighet>()
        val ankemuligheter = mutableListOf<Mulighet>()
        val omgjoeringskravmuligheter = mutableListOf<Mulighet>()

        registrering.muligheter.forEach { mulighet ->
            when (mulighet.type) {
                Type.KLAGE -> {
                    klagemuligheter.add(mulighet)
                }
                Type.ANKE -> {
                    ankemuligheter.add(mulighet)
                }
                Type.OMGJOERINGSKRAV -> {
                    omgjoeringskravmuligheter.add(mulighet)
                }

                else -> error("Not valid mulighet type: ${mulighet.type}")
            }
        }

        val klagemuligheterView = klagemuligheter.map { klagemulighet ->
            klagemulighet.toKlagemulighetView()
        }

        val ankemuligheterView = ankemuligheter.map { ankemulighet ->
            ankemulighet.toKabalmulighetView()
        }

        val omgjoeringskravmuligheterView = omgjoeringskravmuligheter.map { omgjoeringskravmulighet ->
            omgjoeringskravmulighet.toKabalmulighetView()
        }

        return MuligheterView(
            klagemuligheter = klagemuligheterView,
            ankemuligheter = ankemuligheterView,
            omgjoeringskravmuligheter = omgjoeringskravmuligheterView,
            muligheterFetched = registrering.muligheterFetched!!,
        )
    }
}