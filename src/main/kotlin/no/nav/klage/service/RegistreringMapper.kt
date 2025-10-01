package no.nav.klage.service

import no.nav.klage.api.controller.view.*
import no.nav.klage.api.controller.view.Address
import no.nav.klage.clients.SakFromKlanke
import no.nav.klage.clients.kabalapi.MulighetFromKabal
import no.nav.klage.clients.kabalapi.SearchPartView
import no.nav.klage.clients.kabalapi.SvarbrevInput
import no.nav.klage.clients.kabalapi.SvarbrevSettingsView
import no.nav.klage.clients.saf.graphql.Journalpost
import no.nav.klage.domain.entities.*
import no.nav.klage.domain.entities.PartStatus
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.PartIdType
import no.nav.klage.kodeverk.Tema
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.util.calculateFrist

fun SvarbrevReceiver.toRecipientView(
    registrering: Registrering,
    kabalApiService: KabalApiService,
) = RecipientView(
    id = id,
    part = registrering.partViewWithOptionalUtsendingskanal(
        identifikator = part.value,
        kabalApiService = kabalApiService
    ),
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

fun Registrering.toRecipientViews(kabalApiService: KabalApiService) =
    svarbrevReceivers.map { receiver ->
        receiver.toRecipientView(this, kabalApiService = kabalApiService)
    }.sortedBy { it.part.name }

fun Registrering.toTypeChangeRegistreringView(kabalApiService: KabalApiService): TypeChangeRegistreringView {
    return TypeChangeRegistreringView(
        id = id,
        typeId = type?.id,
        mulighetIsBasedOnJournalpost = mulighetIsBasedOnJournalpost,
        overstyringer = TypeChangeRegistreringView.TypeChangeRegistreringOverstyringerView(
            behandlingstid = BehandlingstidView(
                unitTypeId = behandlingstidUnitType.id,
                units = behandlingstidUnits
            )
        ),
        svarbrev = TypeChangeRegistreringView.TypeChangeRegistreringSvarbrevView(
            send = sendSvarbrev,
            behandlingstid = if (svarbrevBehandlingstidUnits != null) {
                BehandlingstidView(
                    unitTypeId = svarbrevBehandlingstidUnitType!!.id,
                    units = svarbrevBehandlingstidUnits!!
                )
            } else null,
            fullmektigFritekst = svarbrevFullmektigFritekst,
            receivers = toRecipientViews(kabalApiService = kabalApiService),
            overrideCustomText = overrideSvarbrevCustomText,
            overrideBehandlingstid = overrideSvarbrevBehandlingstid,
            customText = svarbrevCustomText,
            initialCustomText = svarbrevInitialCustomText,
        ),
        modified = modified,
        willCreateNewJournalpost = willCreateNewJournalpost,
    )
}

fun Registrering.toMulighetChangeRegistreringView(kabalApiService: KabalApiService): MulighetChangeRegistreringView {
    return MulighetChangeRegistreringView(
        id = id,
        mulighet = mulighetId?.let {
            if (mulighetIsBasedOnJournalpost) {
                val chosenMulighet = muligheter.find { mulighet ->
                    mulighet.id == mulighetId
                }
                MulighetIdView(
                    id = chosenMulighet!!.currentFagystemTechnicalId
                )
            } else {
                MulighetIdView(
                    id = it.toString(),
                )
            }
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
            fullmektig = fullmektig?.let {
                partViewWithOptionalUtsendingskanal(
                    identifikator = it.value,
                    kabalApiService = kabalApiService
                )
            },
            klager = klager?.let {
                partViewWithOptionalUtsendingskanal(
                    identifikator = it.value,
                    kabalApiService = kabalApiService
                )
            },
            avsender = avsender?.let {
                partViewWithOptionalUtsendingskanal(
                    identifikator = it.value,
                    kabalApiService = kabalApiService
                )
            },
            saksbehandlerIdent = saksbehandlerIdent,
            gosysOppgaveId = gosysOppgaveId,
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
            receivers = toRecipientViews(kabalApiService),
            overrideCustomText = overrideSvarbrevCustomText,
            overrideBehandlingstid = overrideSvarbrevBehandlingstid,
            customText = svarbrevCustomText,
            initialCustomText = svarbrevInitialCustomText,
        ),
        modified = modified,
        willCreateNewJournalpost = willCreateNewJournalpost,
    )
}

fun Registrering.toFinishedRegistreringView(): FinishedRegistreringView = FinishedRegistreringView(
    id = id,
    sakenGjelderValue = sakenGjelder!!.value,
    typeId = type!!.id,
    ytelseId = ytelse!!.id,
    finished = finished!!,
    created = created,
    behandlingId = behandlingId!!,
)

fun Registrering.toRegistreringView(kabalApiService: KabalApiService) = FullRegistreringView(
    id = id,
    journalpostId = journalpostId,
    sakenGjelderValue = sakenGjelder?.value,
    typeId = type?.id,
    mulighetIsBasedOnJournalpost = mulighetIsBasedOnJournalpost,
    mulighet = mulighetId?.let {
        if (mulighetIsBasedOnJournalpost) {
            val chosenMulighet = muligheter.find { mulighet ->
                mulighet.id == mulighetId
            }
            MulighetIdView(
                id = chosenMulighet!!.currentFagystemTechnicalId
            )
        } else {
            MulighetIdView(
                id = it.toString(),
            )
        }
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
        fullmektig = fullmektig?.let {
            partViewWithOptionalUtsendingskanal(
                identifikator = it.value,
                kabalApiService = kabalApiService
            )
        },
        klager = klager?.let {
            partViewWithOptionalUtsendingskanal(
                identifikator = it.value,
                kabalApiService = kabalApiService
            )
        },
        avsender = avsender?.let {
            partViewWithOptionalUtsendingskanal(
                identifikator = it.value,
                kabalApiService = kabalApiService
            )
        },
        saksbehandlerIdent = saksbehandlerIdent,
        gosysOppgaveId = gosysOppgaveId,
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
        receivers = toRecipientViews(kabalApiService),
        title = svarbrevTitle,
        customText = svarbrevCustomText,
        initialCustomText = svarbrevInitialCustomText,
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
        mulighet.toKabalmulighetView()
    },
    omgjoeringskravmuligheter = muligheter.filter {
        it.type == Type.OMGJOERINGSKRAV && !(mulighetIsBasedOnJournalpost && it.id == mulighetId)
    }.map { mulighet ->
        mulighet.toKabalmulighetView()
    },
    gjenopptaksmuligheter = muligheter.filter {
        it.type == Type.BEGJAERING_OM_GJENOPPTAK && !(mulighetIsBasedOnJournalpost && it.id == mulighetId)
    }.map { mulighet ->
        mulighet.toKabalmulighetView()
    },
    muligheterFetched = muligheterFetched,
)

fun Registrering.partViewWithOptionalUtsendingskanal(
    identifikator: String,
    kabalApiService: KabalApiService
): PartViewWithOptionalUtsendingskanal =
    if (ytelse != null) {
        kabalApiService.searchPartWithUtsendingskanal(
            searchPartInput = SearchPartWithUtsendingskanalInput(
                identifikator = identifikator,
                sakenGjelderId = sakenGjelder!!.value,
                ytelseId = ytelse!!.id
            )
        ).partViewWithOptionalUtsendingskanal()
    } else {
        kabalApiService.searchPart(
            searchPartInput = SearchPartInput(
                identifikator = identifikator,
            )
        ).partViewWithOptionalUtsendingskanal()
    }

fun no.nav.klage.clients.kabalapi.PartViewWithUtsendingskanal.partViewWithOptionalUtsendingskanal(): PartViewWithOptionalUtsendingskanal {
    return PartViewWithOptionalUtsendingskanal(
        identifikator = identifikator,
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

fun SearchPartView.partViewWithOptionalUtsendingskanal(): PartViewWithOptionalUtsendingskanal {
    return PartViewWithOptionalUtsendingskanal(
        identifikator = identifikator,
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

fun Registrering.toSvarbrevInput(svarbrevSettings: SvarbrevSettingsView): SvarbrevInput? {
    if (sendSvarbrev != true) {
        return null
    }

    return SvarbrevInput(
        title = svarbrevTitle,
        initialCustomText = svarbrevInitialCustomText,
        customText = if (overrideSvarbrevCustomText) svarbrevCustomText else svarbrevSettings.customText,
        receivers = svarbrevReceivers.map { receiver ->
            SvarbrevInput.Receiver(
                identifikator = receiver.part.value,
                handling = SvarbrevInput.Receiver.HandlingEnum.valueOf(receiver.handling.name),
                overriddenAddress = receiver.overriddenAddress?.let { address ->
                    SvarbrevInput.Receiver.AddressInput(
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
        varsletBehandlingstidUnitTypeId = if (overrideSvarbrevBehandlingstid) svarbrevBehandlingstidUnitType!!.id else svarbrevSettings.behandlingstidUnitTypeId,
    )
}

fun PartId?.toPartIdInput(): PartIdInput? {
    if (this == null) {
        return null
    }
    return PartIdInput(
        identifikator = value,
        type = when (type) {
            PartIdType.PERSON -> PartType.FNR
            PartIdType.VIRKSOMHET -> PartType.ORGNR
        }
    )
}

fun PartWithUtsendingskanal?.toPartViewWithUtsendingskanal(partStatusList: Set<PartStatus>): PartViewWithUtsendingskanal? {
    return this?.let {
        return PartViewWithUtsendingskanal(
            identifikator = part.value,
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

fun MulighetFromKabal.toMulighet(): Mulighet {
    val ytelse = Ytelse.of(ytelseId)
    return Mulighet(
        originalType = Type.of(originalTypeId),
        type = Type.of(typeId),
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
        klageBehandlendeEnhet = klageBehandlendeEnhet,
        currentFagystemTechnicalId = behandlingId.toString(),
    )
}

fun SakFromKlanke.toMulighet(kabalApiService: KabalApiService): Mulighet {
    val type = if (sakstype.startsWith("KLAGE")) Type.KLAGE else Type.ANKE
    return Mulighet(
        type = type,
        originalType = type,
        tema = Tema.valueOf(tema),
        vedtakDate = if (type == Type.KLAGE) vedtaksdato else null,
        sakenGjelder = kabalApiService.searchPartWithUtsendingskanal(
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

fun Journalpost.toMulighet(kabalApiService: KabalApiService, registrering: Registrering): Mulighet {
    return Mulighet(
        type = registrering.type!!,
        originalType = null,
        tema = Tema.valueOf(tema.name),
        //Vurder om vi skal sette noe annet enn null her
        vedtakDate = null,
        sakenGjelder = kabalApiService.searchPartWithUtsendingskanal(
            SearchPartWithUtsendingskanalInput(
                identifikator = registrering.sakenGjelder!!.value,
                sakenGjelderId = registrering.sakenGjelder!!.value,
                //don't care which ytelse is picked, as long as Tema is correct. Could be prettier.
                ytelseId = Ytelse.entries.find { y -> y.toTema().navn == tema.name }!!.id,
            )
        ).toPartWithUtsendingskanal()!!,
        fagsakId = sak!!.fagsakId!!,
        originalFagsystem = Fagsystem.valueOf(sak.fagsaksystem!!),
        currentFagsystem = Fagsystem.valueOf(sak.fagsaksystem),
        ytelse = null,
        klager = null,
        fullmektig = null,
        klageBehandlendeEnhet = journalfoerendeEnhet!!,
        currentFagystemTechnicalId = journalpostId,
        previousSaksbehandlerIdent = null,
        previousSaksbehandlerName = null,
        hjemmelIdList = emptyList(),
    )
}

fun no.nav.klage.clients.kabalapi.PartViewWithUtsendingskanal?.toPartWithUtsendingskanal(): PartWithUtsendingskanal? {
    return this?.let {
        PartWithUtsendingskanal(
            part = PartId(
                value = identifikator,
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

fun Mulighet.toKlagemulighetView() =
    KlagemulighetView(
        id = id,
        temaId = tema.id,
        vedtakDate = vedtakDate!!,
        sakenGjelder = sakenGjelder.toPartViewWithUtsendingskanal(sakenGjelderStatusList)!!,
        fagsakId = fagsakId,
        originalFagsystemId = originalFagsystem.id,
        currentFagsystemId = currentFagsystem.id,
        typeId = originalType!!.id,
        klageBehandlendeEnhet = klageBehandlendeEnhet,
    )

fun Mulighet.toKabalmulighetView(): KabalmulighetView =
    KabalmulighetView(
        id = id,
        temaId = tema.id,
        vedtakDate = vedtakDate,
        sakenGjelder = sakenGjelder.toPartViewWithUtsendingskanal(sakenGjelderStatusList)!!,
        fagsakId = fagsakId,
        originalFagsystemId = originalFagsystem.id,
        currentFagsystemId = currentFagsystem.id,
        typeId = originalType!!.id,
        sourceOfExistingBehandlinger = sourceOfExistingAnkebehandling.map {
            ExistingBehandling(
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