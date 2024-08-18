package no.nav.klage.service

import no.nav.klage.api.controller.view.*
import no.nav.klage.api.controller.view.Address
import no.nav.klage.api.controller.view.ExistingAnkebehandling
import no.nav.klage.clients.SakFromKlanke
import no.nav.klage.clients.kabalapi.AnkemulighetFromKabal
import no.nav.klage.clients.kabalapi.KabalApiClient
import no.nav.klage.clients.kabalapi.PartView
import no.nav.klage.clients.kabalapi.SvarbrevSettingsView
import no.nav.klage.domain.entities.*
import no.nav.klage.domain.entities.PartStatus
import no.nav.klage.kodeverk.*
import no.nav.klage.util.calculateFrist

fun SvarbrevReceiver.toRecipientView(
    registrering: Registrering,
    kabalApiClient: KabalApiClient,
) = RecipientView(
    id = id,
    part = registrering.partViewWithOptionalUtsendingskanal(identifikator = part.value, kabalApiClient = kabalApiClient),
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

fun Registrering.toRecipientViews(kabalApiClient: KabalApiClient) =
    svarbrevReceivers.map { receiver ->
        receiver.toRecipientView(this, kabalApiClient = kabalApiClient)
    }.sortedBy { it.part.name }

fun Registrering.toTypeChangeRegistreringView(kabalApiClient: KabalApiClient): TypeChangeRegistreringView {
    return TypeChangeRegistreringView(
        id = id,
        typeId = type?.id,
        overstyringer = TypeChangeRegistreringView.TypeChangeRegistreringOverstyringerView(),
        svarbrev = TypeChangeRegistreringView.TypeChangeRegistreringSvarbrevView(
            send = sendSvarbrev,
            behandlingstid = if (svarbrevBehandlingstidUnits != null) {
                BehandlingstidView(
                    unitTypeId = svarbrevBehandlingstidUnitType!!.id,
                    units = svarbrevBehandlingstidUnits!!
                )
            } else null,
            fullmektigFritekst = svarbrevFullmektigFritekst,
            receivers = toRecipientViews(kabalApiClient = kabalApiClient),
            overrideCustomText = overrideSvarbrevCustomText,
            overrideBehandlingstid = overrideSvarbrevBehandlingstid,
            customText = svarbrevCustomText,
        ),
        modified = modified,
        willCreateNewJournalpost = willCreateNewJournalpost,
    )
}

fun Registrering.toMulighetChangeRegistreringView(kabalApiClient: KabalApiClient): MulighetChangeRegistreringView {
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
            fullmektig = fullmektig?.let { partViewWithOptionalUtsendingskanal(identifikator = it.value, kabalApiClient = kabalApiClient) },
            klager = klager?.let { partViewWithOptionalUtsendingskanal(identifikator = it.value, kabalApiClient = kabalApiClient) },
            avsender = avsender?.let { partViewWithOptionalUtsendingskanal(identifikator = it.value, kabalApiClient = kabalApiClient) },
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
            receivers = toRecipientViews(kabalApiClient),
            overrideCustomText = overrideSvarbrevCustomText,
            overrideBehandlingstid = overrideSvarbrevBehandlingstid,
            customText = svarbrevCustomText,
        ),
        modified = modified,
        willCreateNewJournalpost = willCreateNewJournalpost,
    )
}

fun Registrering.toRegistreringView(kabalApiClient: KabalApiClient) = FullRegistreringView(
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
        fullmektig = fullmektig?.let { partViewWithOptionalUtsendingskanal(identifikator = it.value, kabalApiClient = kabalApiClient) },
        klager = klager?.let { partViewWithOptionalUtsendingskanal(identifikator = it.value, kabalApiClient = kabalApiClient) },
        avsender = avsender?.let { partViewWithOptionalUtsendingskanal(identifikator = it.value, kabalApiClient = kabalApiClient) },
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
        receivers = toRecipientViews(kabalApiClient),
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

fun Registrering.partViewWithOptionalUtsendingskanal(identifikator: String, kabalApiClient: KabalApiClient): PartViewWithOptionalUtsendingskanal =
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

fun no.nav.klage.clients.kabalapi.PartViewWithUtsendingskanal.partViewWithOptionalUtsendingskanal(): PartViewWithOptionalUtsendingskanal {
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

fun PartView.partViewWithOptionalUtsendingskanal(): PartViewWithOptionalUtsendingskanal {
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

fun Registrering.toSvarbrevWithReceiverInput(svarbrevSettings: SvarbrevSettingsView): SvarbrevWithReceiverInput? {
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

fun PartId?.toPartIdInput(): PartIdInput? {
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

fun PartWithUtsendingskanal?.toPartViewWithUtsendingskanal(partStatusList: Set<PartStatus>): PartViewWithUtsendingskanal? {
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

fun AnkemulighetFromKabal.toMulighet(): Mulighet {
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
        klageBehandlendeEnhet = klageBehandlendeEnhet,
        currentFagystemTechnicalId = behandlingId.toString(),
    )
}

fun SakFromKlanke.toMulighet(kabalApiClient: KabalApiClient): Mulighet {
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

fun no.nav.klage.clients.kabalapi.PartViewWithUtsendingskanal?.toPartWithUtsendingskanal(): PartWithUtsendingskanal? {
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

fun Mulighet.toKlagemulighetView() =
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

fun Mulighet.toAnkemulighetView(): AnkemulighetView =
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