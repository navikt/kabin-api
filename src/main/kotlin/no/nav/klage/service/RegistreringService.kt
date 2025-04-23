package no.nav.klage.service

import no.nav.klage.api.controller.mapper.toReceiptView
import no.nav.klage.api.controller.view.*
import no.nav.klage.api.controller.view.BehandlingstidChangeRegistreringView.BehandlingstidChangeRegistreringOverstyringerView
import no.nav.klage.api.controller.view.DokumentReferanse.AvsenderMottaker.AvsenderMottakerIdType
import no.nav.klage.api.controller.view.MottattVedtaksinstansChangeRegistreringView.MottattVedtaksinstansChangeRegistreringOverstyringerView
import no.nav.klage.clients.SakFromKlanke
import no.nav.klage.clients.kabalapi.BehandlingIsDuplicateInput
import no.nav.klage.clients.kabalapi.MulighetFromKabal
import no.nav.klage.clients.kabalapi.toView
import no.nav.klage.domain.entities.Mulighet
import no.nav.klage.domain.entities.PartId
import no.nav.klage.domain.entities.Registrering
import no.nav.klage.domain.entities.SvarbrevReceiver
import no.nav.klage.exceptions.*
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.PartIdType
import no.nav.klage.kodeverk.TimeUnitType
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.ytelse.Ytelse
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
    private val kabalApiService: KabalApiService,
    private val tokenUtil: TokenUtil,
    private val klageService: KlageService,
    private val ankeService: AnkeService,
    private val omgjoeringskravService: OmgjoeringskravService,
    private val documentService: DocumentService,
    private val dokArkivService: DokArkivService,
    private val klageFssProxyService: KlageFssProxyService,
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
                mulighetBasedOnJournalpost = false,
                mulighetId = null,
                mottattVedtaksinstans = null,
                mottattKlageinstans = null,
                behandlingstidUnits = 12,
                behandlingstidUnitType = TimeUnitType.WEEKS,
                hjemmelIdList = listOf(),
                ytelse = null,
                saksbehandlerIdent = null,
                gosysOppgaveId = null,
                sendSvarbrev = null,
                svarbrevTitle = "Nav klageinstans orienterer om saksbehandlingen",
                svarbrevCustomText = null,
                svarbrevInitialCustomText = null,
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

        return registrering.toRegistreringView(kabalApiService = kabalApiService)
    }

    fun getRegistrering(registreringId: UUID): FullRegistreringView {
        return registreringRepository.findById(registreringId)
            .orElseThrow { throw RegistreringNotFoundException("Registrering ikke funnet.") }
            .toRegistreringView(kabalApiService = kabalApiService)
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
        ).map { it.toRegistreringView(kabalApiService = kabalApiService) }.sortedByDescending { it.created }
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
                gosysOppgaveId = null
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
        return registrering.toRegistreringView(kabalApiService = kabalApiService)
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
                    if (document.journalposttype == DokumentReferanse.Journalposttype.I && document.avsenderMottaker?.id != null && document.avsenderMottaker.type != null) {
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

        return registrering.toRegistreringView(kabalApiService = kabalApiService)
    }

    fun setTypeId(registreringId: UUID, input: TypeIdInput): TypeChangeRegistreringView {
        return getRegistreringForUpdate(registreringId)
            .apply {
                type = input.typeId?.let { typeId ->
                    Type.of(typeId)
                }
                mulighetBasedOnJournalpost = false
                modified = LocalDateTime.now()
                behandlingstidUnits = getDefaultBehandlingstidUnits(type)
                behandlingstidUnitType = getDefaultBehandlingstidUnitType(type)

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

                gosysOppgaveId = null

                willCreateNewJournalpost = false

            }.toTypeChangeRegistreringView(kabalApiService = kabalApiService)
    }

    fun setMulighetBasedOnJournalpost(registreringId: UUID, input: MulighetBasedOnJournalpostInput): TypeChangeRegistreringView {
        return getRegistreringForUpdate(registreringId)
            .apply {
                mulighetBasedOnJournalpost = input.mulighetBasedOnJournalpost
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

                gosysOppgaveId = null

                willCreateNewJournalpost = false


            }.toTypeChangeRegistreringView(kabalApiService = kabalApiService)
    }

    private fun getDefaultBehandlingstidUnitType(type: Type?): TimeUnitType {
        return TimeUnitType.WEEKS
    }

    private fun getDefaultBehandlingstidUnits(type: Type?): Int {
        return if (type == Type.ANKE) {
            11
        } else {
            12
        }
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

                if (ytelse == null) {
                    //empty the properties that no longer make sense
                    sendSvarbrev = false
                    svarbrevCustomText = null
                    svarbrevBehandlingstidUnits = null
                    svarbrevBehandlingstidUnitType = null
                    overrideSvarbrevBehandlingstid = false
                    overrideSvarbrevCustomText = false

                    hjemmelIdList = emptyList()

                    saksbehandlerIdent = null
                } else if (previousYtelse != ytelse) {
                    //set svarbrev settings (and reset old) for the new ytelse
                    setSvarbrevSettings()

                    hjemmelIdList = newMulighet.hjemmelIdList.ifEmpty {
                        emptyList()
                    }

                    //Could be smarter here.
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
            }.toMulighetChangeRegistreringView(kabalApiService = kabalApiService)
    }

    fun Registrering.reinitializeMuligheter() {
        val input = IdnummerInput(idnummer = sakenGjelder!!.value)

        val tokenWithKlageFSSProxyScope = "Bearer ${tokenUtil.getOnBehalfOfTokenWithKlageFSSProxyScope()}"

        val klagemuligheterFromInfotrygdMono = klageFssProxyService.getKlagemuligheterAsMono(
            input = input,
            token = tokenWithKlageFSSProxyScope,
        )
        val klageTilbakebetalingMuligheterFromInfotrygdMono =
            klageFssProxyService.getKlageTilbakebetalingMuligheterAsMono(
                input = input,
                token = tokenWithKlageFSSProxyScope,
            )
        val ankemuligheterFromInfotrygdMono = klageFssProxyService.getAnkemuligheterAsMono(
            input = input,
            token = tokenWithKlageFSSProxyScope,
        )

        val saksbehandlerAccessTokenWithKabalApiScope =
            "Bearer ${tokenUtil.getSaksbehandlerAccessTokenWithKabalApiScope()}"

        val ankemuligheterFromKabalMono =
            kabalApiService.getAnkemuligheterAsMono(
                input = input,
                token = saksbehandlerAccessTokenWithKabalApiScope,
            )
        val omgjoeringskravmuligheterFromKabalMono =
            kabalApiService.getOmgjoeringskravmuligheterAsMono(
                input = input,
                token = saksbehandlerAccessTokenWithKabalApiScope,
            )

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

        val maskinTilMaskinAccessTokenWithKabalApiScope =
            "Bearer ${tokenUtil.getMaskinTilMaskinAccessTokenWithKabalApiScope()}"

        val behandlingIsDuplicateResponses = Flux.fromIterable(muligheterFromInfotrygd)
            .parallel()
            .runOn(Schedulers.parallel())
            .flatMap { mulighetFromInfotrygd ->
                kabalApiService.checkBehandlingDuplicateInKabal(
                    input = BehandlingIsDuplicateInput(
                        fagsystemId = Fagsystem.IT01.id,
                        kildereferanse = mulighetFromInfotrygd.sakId,
                        typeId = if (mulighetFromInfotrygd.sakstype.startsWith("KLAGE")) Type.KLAGE.id else Type.ANKE.id
                    ),
                    token = maskinTilMaskinAccessTokenWithKabalApiScope,
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
            filteredInfotrygdMuligheter.map { it.toMulighet(kabalApiService = kabalApiService) } + muligheterFromKabal.map { it.toMulighet() }

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
        svarbrevBehandlingstidUnitType = TimeUnitType.of(svarbrevSettings.behandlingstidUnitTypeId)
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
                initialCustomText = registrering.svarbrevInitialCustomText
            ),
            modified = registrering.modified,
        )
    }

    fun setFullmektig(registreringId: UUID, input: FullmektigInput): FullmektigChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                //cases
                //1. fullmektig is set to the same value as before
                if (fullmektig?.value == input.fullmektig?.identifikator) {
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
                        value = input.fullmektig.identifikator,
                        type = when (input.fullmektig.type) {
                            PartType.FNR -> {
                                PartIdType.PERSON
                            }

                            PartType.ORGNR -> {
                                PartIdType.VIRKSOMHET
                            }
                        }
                    )
                    val part =
                        kabalApiService.searchPart(SearchPartInput(identifikator = input.fullmektig.identifikator))
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
                        kabalApiService = kabalApiService,
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
        if (partIdInput != null && svarbrevReceivers.size == 1 && svarbrevReceivers.first().part.value == unchangedRegistrering.sakenGjelder?.value && partIdInput.identifikator != unchangedRegistrering.sakenGjelder?.value) {
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
                if (klager?.value == input.klager?.identifikator) {
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
                        value = input.klager.identifikator,
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
                        kabalApiService = kabalApiService
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
                if (avsender?.value == input.avsender?.identifikator) {
                    return@apply
                }

                //2. avsender is set to null
                if (input.avsender == null) {
                    avsender = null
                } else {
                    //3. avsender is set to a new value
                    avsender = PartId(
                        value = input.avsender.identifikator,
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
                        kabalApiService = kabalApiService
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

    fun setGosysOppgaveId(registreringId: UUID, input: GosysOppgaveIdInput): GosysOppgaveIdChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                gosysOppgaveId = input.gosysOppgaveId
                modified = LocalDateTime.now()
            }

        return GosysOppgaveIdChangeRegistreringView(
            id = registrering.id,
            overstyringer = GosysOppgaveIdChangeRegistreringView.GosysOppgaveIdChangeRegistreringOverstyringerView(
                gosysOppgaveId = registrering.gosysOppgaveId,
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
                overrideCustomText = registrering.overrideSvarbrevCustomText,
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
                    svarbrevBehandlingstidUnitType = TimeUnitType.of(svarbrevSettings.behandlingstidUnitTypeId)
                }

                modified = LocalDateTime.now()
            }
        return SvarbrevOverrideBehandlingstidChangeRegistreringView(
            id = registrering.id,
            svarbrev = SvarbrevOverrideBehandlingstidChangeRegistreringView.SvarbrevOverrideBehandlingstidChangeRegistreringSvarbrevView(
                overrideBehandlingstid = registrering.overrideSvarbrevBehandlingstid,
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

    private fun Registrering.getSvarbrevSettings() = kabalApiService.getSvarbrevSettings(
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

    fun setSvarbrevInitialCustomText(
        registreringId: UUID,
        input: SvarbrevInitialCustomTextInput
    ): SvarbrevInitialCustomTextChangeRegistreringView {
        val registrering = getRegistreringForUpdate(registreringId)
            .apply {
                svarbrevInitialCustomText = input.initialCustomText
                modified = LocalDateTime.now()
            }
        return SvarbrevInitialCustomTextChangeRegistreringView(
            id = registrering.id,
            svarbrev = SvarbrevInitialCustomTextChangeRegistreringView.SvarbrevInitialCustomTextChangeRegistreringSvarbrevView(
                initialCustomText = registrering.svarbrevInitialCustomText,
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
                if (svarbrevReceivers.any { it.part.value == input.part.identifikator }) {
                    //if the receiver is already in the list, we don't need to do anything.
                } else {
                    svarbrevReceivers.add(
                        SvarbrevReceiver(
                            part = PartId(
                                value = input.part.identifikator,
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
            receiver.toRecipientView(registrering = this, kabalApiService = kabalApiService)
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

        val response: CreatedBehandlingResponse = when (registrering.type) {
            Type.ANKE -> {
                ankeService.createAnke(
                    registrering = registrering,
                )
            }

            Type.KLAGE -> {
                klageService.createKlage(
                    registrering = registrering,
                )
            }

            Type.OMGJOERINGSKRAV -> {
                omgjoeringskravService.createOmgjoeringskrav(
                    registrering = registrering
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

    fun getMulighetFromBehandlingId(behandlingId: UUID): Mulighet {
        val registrering = registreringRepository.findByBehandlingId(behandlingId)
        return registrering.muligheter.find { it.id == registrering.mulighetId }!!
    }

    fun getCreatedBehandlingStatus(behandlingId: UUID): CreatedBehandlingStatusView {
        val mulighet = getMulighetFromBehandlingId(behandlingId)
        val status = kabalApiService.getBehandlingStatus(behandlingId = behandlingId)

        return CreatedBehandlingStatusView(
            typeId = status.typeId,
            ytelseId = status.ytelseId,
            vedtakDate = mulighet.vedtakDate,
            sakenGjelder = status.sakenGjelder.partViewWithUtsendingskanal(),
            klager = status.klager.partViewWithUtsendingskanal(),
            fullmektig = status.fullmektig?.partViewWithUtsendingskanal(),
            mottattVedtaksinstans = status.mottattVedtaksinstans,
            mottattKlageinstans = status.mottattKlageinstans,
            frist = status.frist,
            varsletFrist = status.varsletFrist,
            varsletFristUnits = status.varsletFristUnits,
            varsletFristUnitTypeId = status.varsletFristUnitTypeId,
            fagsakId = status.fagsakId,
            fagsystemId = status.fagsystemId,
            journalpost = status.journalpost.toReceiptView(),
            tildeltSaksbehandler = status.tildeltSaksbehandler?.toView(),
            svarbrev = status.svarbrev?.toView(),
        )
    }
}