package no.nav.klage.api.controller.mapper

import no.nav.klage.api.controller.view.DokumentReferanseForReceipt
import no.nav.klage.clients.kabalapi.DokumentReferanse
import no.nav.klage.api.controller.view.DokumentReferanse as DokumentReferanseView

fun DokumentReferanse.toReceiptView(): DokumentReferanseForReceipt =
    DokumentReferanseForReceipt(
        journalpostId = journalpostId,
        dokumentInfoId = dokumentInfoId,
        tittel = tittel,
        tema = tema,
        temaId = temaId,
        harTilgangTilArkivvariant = harTilgangTilArkivvariant,
        logiskeVedlegg =
            logiskeVedlegg?.map {
                DokumentReferanseView.LogiskVedlegg(
                    logiskVedleggId = it.logiskVedleggId,
                    tittel = it.tittel,
                )
            },
        vedlegg = vedlegg.map { it.toView() }.toMutableList(),
        journalposttype = journalposttype?.toView(),
        journalstatus = journalstatus?.toView(),
        behandlingstema = behandlingstema,
        behandlingstemanavn = behandlingstemanavn,
        sak = sak?.toView(),
        avsenderMottaker = avsenderMottaker?.toView(),
        journalfoerendeEnhet = journalfoerendeEnhet,
        journalfortAvNavn = journalfortAvNavn,
        opprettetAvNavn = opprettetAvNavn,
        datoOpprettet = datoOpprettet,
        relevanteDatoer = relevanteDatoer?.map { it.toView() },
        antallRetur = antallRetur,
        tilleggsopplysninger = tilleggsopplysninger?.map { it.toView() },
        kanal = kanal,
        kanalnavn = kanalnavn,
        utsendingsinfo = utsendingsinfo?.toView(),
        alreadyUsed = alreadyUsed,
    )

fun DokumentReferanse.VedleggReferanse.toView(): DokumentReferanseView.VedleggReferanseForReceipt =
    DokumentReferanseView.VedleggReferanseForReceipt(
        dokumentInfoId = dokumentInfoId,
        tittel = tittel,
        harTilgangTilArkivvariant = harTilgangTilArkivvariant,
        logiskeVedlegg =
            logiskeVedlegg?.map {
                DokumentReferanseView.LogiskVedlegg(
                    logiskVedleggId = it.logiskVedleggId,
                    tittel = it.tittel,
                )
            },
    )

fun DokumentReferanse.Journalposttype.toView(): DokumentReferanseView.Journalposttype =
    DokumentReferanseView.Journalposttype
        .valueOf(this.name)

fun DokumentReferanse.Journalstatus.toView(): DokumentReferanseView.Journalstatus =
    DokumentReferanseView.Journalstatus
        .valueOf(this.name)

fun DokumentReferanse.Sak.toView(): DokumentReferanseView.Sak =
    DokumentReferanseView.Sak(
        datoOpprettet = datoOpprettet,
        fagsakId = fagsakId,
        fagsaksystem = fagsaksystem,
        fagsystemId = fagsystemId,
    )

fun DokumentReferanse.AvsenderMottaker.toView(): DokumentReferanseView.AvsenderMottaker =
    DokumentReferanseView.AvsenderMottaker(
        id = id,
        type = type?.toView(),
        // Ta-da:
        name = navn,
    )

fun DokumentReferanse.AvsenderMottaker.AvsenderMottakerIdType.toView(): DokumentReferanseView.AvsenderMottaker.AvsenderMottakerIdType =
    DokumentReferanseView.AvsenderMottaker.AvsenderMottakerIdType
        .valueOf(this.name)

fun DokumentReferanse.RelevantDato.toView(): DokumentReferanseView.RelevantDato =
    DokumentReferanseView.RelevantDato(
        dato = dato,
        datotype = datotype.toView(),
    )

fun DokumentReferanse.RelevantDato.Datotype.toView(): DokumentReferanseView.RelevantDato.Datotype =
    DokumentReferanseView.RelevantDato.Datotype
        .valueOf(this.name)

fun DokumentReferanse.Tilleggsopplysning.toView(): DokumentReferanseView.Tilleggsopplysning =
    DokumentReferanseView.Tilleggsopplysning(
        key = key,
        value = value,
    )

fun DokumentReferanse.Utsendingsinfo.toView(): DokumentReferanseView.Utsendingsinfo =
    DokumentReferanseView.Utsendingsinfo(
        epostVarselSendt = epostVarselSendt?.toView(),
        smsVarselSendt = smsVarselSendt?.toView(),
        fysiskpostSendt = fysiskpostSendt?.toView(),
        digitalpostSendt = digitalpostSendt?.toView(),
    )

fun DokumentReferanse.Utsendingsinfo.EpostVarselSendt.toView(): DokumentReferanseView.Utsendingsinfo.EpostVarselSendt =
    DokumentReferanseView.Utsendingsinfo.EpostVarselSendt(
        tittel = tittel,
        adresse = adresse,
        varslingstekst = varslingstekst,
    )

fun DokumentReferanse.Utsendingsinfo.SmsVarselSendt.toView(): DokumentReferanseView.Utsendingsinfo.SmsVarselSendt =
    DokumentReferanseView.Utsendingsinfo.SmsVarselSendt(
        adresse = adresse,
        varslingstekst = varslingstekst,
    )

fun DokumentReferanse.Utsendingsinfo.FysiskpostSendt.toView(): DokumentReferanseView.Utsendingsinfo.FysiskpostSendt =
    DokumentReferanseView.Utsendingsinfo.FysiskpostSendt(
        adressetekstKonvolutt = adressetekstKonvolutt,
    )

fun DokumentReferanse.Utsendingsinfo.DigitalpostSendt.toView(): DokumentReferanseView.Utsendingsinfo.DigitalpostSendt =
    DokumentReferanseView.Utsendingsinfo.DigitalpostSendt(
        adresse = adresse,
    )
