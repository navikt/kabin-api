package no.nav.klage.api.controller.mapper

import no.nav.klage.clients.kabalapi.DokumentReferanse


fun DokumentReferanse.toView(): no.nav.klage.api.controller.view.DokumentReferanse {
    return no.nav.klage.api.controller.view.DokumentReferanse(
        journalpostId = journalpostId,
        dokumentInfoId = dokumentInfoId,
        tittel = tittel,
        tema = tema,
        temaId = temaId,
        harTilgangTilArkivvariant = harTilgangTilArkivvariant,
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
        alreadyUsed = alreadyUsed
    )
}

fun DokumentReferanse.VedleggReferanse.toView(): no.nav.klage.api.controller.view.DokumentReferanse.VedleggReferanse {
    return no.nav.klage.api.controller.view.DokumentReferanse.VedleggReferanse(
        dokumentInfoId = dokumentInfoId,
        tittel = tittel,
        harTilgangTilArkivvariant = harTilgangTilArkivvariant
    )
}

fun DokumentReferanse.Journalposttype.toView(): no.nav.klage.api.controller.view.DokumentReferanse.Journalposttype {
    return no.nav.klage.api.controller.view.DokumentReferanse.Journalposttype.valueOf(this.name)
}

fun DokumentReferanse.Journalstatus.toView(): no.nav.klage.api.controller.view.DokumentReferanse.Journalstatus {
    return no.nav.klage.api.controller.view.DokumentReferanse.Journalstatus.valueOf(this.name)
}

fun DokumentReferanse.Sak.toView(): no.nav.klage.api.controller.view.DokumentReferanse.Sak {
    return no.nav.klage.api.controller.view.DokumentReferanse.Sak(
        datoOpprettet = datoOpprettet,
        fagsakId = fagsakId,
        fagsaksystem = fagsaksystem,
        fagsystemId = fagsystemId
    )
}

fun DokumentReferanse.AvsenderMottaker.toView(): no.nav.klage.api.controller.view.DokumentReferanse.AvsenderMottaker {
    return no.nav.klage.api.controller.view.DokumentReferanse.AvsenderMottaker(
        id = id,
        type = type.toView(),
        //Ta-da:
        name = navn
    )
}

fun DokumentReferanse.AvsenderMottaker.AvsenderMottakerIdType.toView(): no.nav.klage.api.controller.view.DokumentReferanse.AvsenderMottaker.AvsenderMottakerIdType {
    return no.nav.klage.api.controller.view.DokumentReferanse.AvsenderMottaker.AvsenderMottakerIdType.valueOf(this.name)
}

fun DokumentReferanse.RelevantDato.toView(): no.nav.klage.api.controller.view.DokumentReferanse.RelevantDato {
    return no.nav.klage.api.controller.view.DokumentReferanse.RelevantDato(
        dato = dato,
        datotype = datotype.toView()
    )
}

fun DokumentReferanse.RelevantDato.Datotype.toView(): no.nav.klage.api.controller.view.DokumentReferanse.RelevantDato.Datotype {
    return no.nav.klage.api.controller.view.DokumentReferanse.RelevantDato.Datotype.valueOf(this.name)
}

fun DokumentReferanse.Tilleggsopplysning.toView(): no.nav.klage.api.controller.view.DokumentReferanse.Tilleggsopplysning {
    return no.nav.klage.api.controller.view.DokumentReferanse.Tilleggsopplysning(
        key = key,
        value = value
    )
}

fun DokumentReferanse.Utsendingsinfo.toView(): no.nav.klage.api.controller.view.DokumentReferanse.Utsendingsinfo {
    return no.nav.klage.api.controller.view.DokumentReferanse.Utsendingsinfo(
        epostVarselSendt = epostVarselSendt?.toView(),
        smsVarselSendt = smsVarselSendt?.toView(),
        fysiskpostSendt = fysiskpostSendt?.toView(),
        digitalpostSendt = digitalpostSendt?.toView(),
    )
}

fun DokumentReferanse.Utsendingsinfo.EpostVarselSendt.toView(): no.nav.klage.api.controller.view.DokumentReferanse.Utsendingsinfo.EpostVarselSendt {
    return no.nav.klage.api.controller.view.DokumentReferanse.Utsendingsinfo.EpostVarselSendt(
        tittel = tittel,
        adresse = adresse,
        varslingstekst = varslingstekst
    )
}

fun DokumentReferanse.Utsendingsinfo.SmsVarselSendt.toView(): no.nav.klage.api.controller.view.DokumentReferanse.Utsendingsinfo.SmsVarselSendt {
    return no.nav.klage.api.controller.view.DokumentReferanse.Utsendingsinfo.SmsVarselSendt(
        adresse = adresse,
        varslingstekst = varslingstekst
    )
}

fun DokumentReferanse.Utsendingsinfo.FysiskpostSendt.toView(): no.nav.klage.api.controller.view.DokumentReferanse.Utsendingsinfo.FysiskpostSendt {
    return no.nav.klage.api.controller.view.DokumentReferanse.Utsendingsinfo.FysiskpostSendt(
        adressetekstKonvolutt = adressetekstKonvolutt
    )
}

fun DokumentReferanse.Utsendingsinfo.DigitalpostSendt.toView(): no.nav.klage.api.controller.view.DokumentReferanse.Utsendingsinfo.DigitalpostSendt {
    return no.nav.klage.api.controller.view.DokumentReferanse.Utsendingsinfo.DigitalpostSendt(
        adresse = adresse
    )
}