package no.nav.klage.service

import no.nav.klage.api.controller.view.DokumentReferanse
import no.nav.klage.api.controller.view.DokumenterResponse
import no.nav.klage.clients.saf.graphql.*
import no.nav.klage.clients.saf.rest.ArkivertDokument
import no.nav.klage.clients.saf.rest.SafRestClient
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.Tema
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.stereotype.Service

@Service
class DocumentService(
    private val safGraphQlClient: SafGraphQlClient,
    private val safRestClient: SafRestClient,
    private val kabalApiService: KabalApiService,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
        private val dokumentMapper = DokumentMapper()
    }

    fun fetchDokumentlisteForBruker(
        idnummer: String,
        temaer: List<Tema>,
        pageSize: Int,
        previousPageRef: String?
    ): DokumenterResponse {
        if (idnummer.length == 11) {
            val dokumentoversiktBruker: DokumentoversiktBruker =
                safGraphQlClient.getDokumentoversiktBruker(
                    idnummer = idnummer,
                    tema = mapTema(temaer),
                    pageSize = pageSize,
                    previousPageRef = previousPageRef
                )

            val dokumenter = dokumentoversiktBruker.journalposter.map { journalpost ->
                dokumentMapper.mapJournalpostToDokumentReferanse(journalpost)
            }

            //enrich documents with usage info
            val usedJournalpostIdList = kabalApiService.getUsedJournalpostIdListForPerson(fnr = idnummer)
            dokumenter.forEach { document ->
                if (document.journalpostId in usedJournalpostIdList) {
                    document.alreadyUsed = true
                }
            }

            return DokumenterResponse(
                dokumenter = dokumenter,
                pageReference = if (dokumentoversiktBruker.sideInfo.finnesNesteSide) {
                    dokumentoversiktBruker.sideInfo.sluttpeker
                } else {
                    null
                },
                antall = dokumentoversiktBruker.sideInfo.antall,
                totaltAntall = dokumentoversiktBruker.sideInfo.totaltAntall
            )
        } else {
            return DokumenterResponse(dokumenter = emptyList(), pageReference = null, antall = 0, totaltAntall = 0)
        }
    }

    private fun mapTema(temaer: List<Tema>): List<no.nav.klage.clients.saf.graphql.Tema> =
        temaer.map { tema -> no.nav.klage.clients.saf.graphql.Tema.valueOf(tema.name) }

    fun getArkivertDokument(journalpostId: String, dokumentInfoId: String): ArkivertDokument {
        return safRestClient.getDokument(dokumentInfoId, journalpostId)
    }

    private fun harArkivVariantformat(dokumentInfo: DokumentInfo): Boolean =
        dokumentInfo.dokumentvarianter.any { dv ->
            dv.variantformat == Variantformat.ARKIV
        }

}

class DokumentMapper {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    //TODO: Har ikke tatt høyde for skjerming, ref https://confluence.adeo.no/pages/viewpage.action?pageId=320364687
    fun mapJournalpostToDokumentReferanse(
        journalpost: Journalpost,
    ): DokumentReferanse {

        val hoveddokument = journalpost.dokumenter?.firstOrNull()
            ?: throw RuntimeException("Could not find hoveddokument for journalpost ${journalpost.journalpostId}")

        val dokumentReferanse = DokumentReferanse(
            tittel = hoveddokument.tittel,
            //remove when client no longer uses
            tema = Tema.fromNavn(journalpost.tema.name).id,
            temaId = Tema.fromNavn(journalpost.tema.name).id,
            registrert = journalpost.datoOpprettet.toLocalDate(),
            dokumentInfoId = hoveddokument.dokumentInfoId,
            journalpostId = journalpost.journalpostId,
            harTilgangTilArkivvariant = harTilgangTilArkivvariant(hoveddokument),
            journalposttype = DokumentReferanse.Journalposttype.valueOf(journalpost.journalposttype!!.name),
            journalstatus = if (journalpost.journalstatus != null) {
                DokumentReferanse.Journalstatus.valueOf(journalpost.journalstatus.name)
            } else null,
            behandlingstema = journalpost.behandlingstema,
            behandlingstemanavn = journalpost.behandlingstemanavn,
            sak = if (journalpost.sak != null) {
                DokumentReferanse.Sak(
                    datoOpprettet = journalpost.sak.datoOpprettet,
                    fagsakId = journalpost.sak.fagsakId,
                    fagsaksystem = journalpost.sak.fagsaksystem,
                    fagsystemId = journalpost.sak.fagsaksystem?.let { Fagsystem.fromNavn(journalpost.sak.fagsaksystem).id },
                )
            } else null,
            avsenderMottaker = if (journalpost.avsenderMottaker != null) {
                DokumentReferanse.AvsenderMottaker(
                    id = journalpost.avsenderMottaker.id,
                    type = if (journalpost.avsenderMottaker.type != null) DokumentReferanse.AvsenderMottaker.AvsenderMottakerIdType.valueOf(
                        journalpost.avsenderMottaker.type.name
                    ) else null,
                    navn = journalpost.avsenderMottaker.navn,
                    land = journalpost.avsenderMottaker.land,
                    erLikBruker = journalpost.avsenderMottaker.erLikBruker,

                    )
            } else null,
            journalfoerendeEnhet = journalpost.journalfoerendeEnhet,
            journalfortAvNavn = journalpost.journalfortAvNavn,
            opprettetAvNavn = journalpost.opprettetAvNavn,
            datoOpprettet = journalpost.datoOpprettet,
            relevanteDatoer = journalpost.relevanteDatoer?.map {
                DokumentReferanse.RelevantDato(
                    dato = it.dato,
                    datotype = DokumentReferanse.RelevantDato.Datotype.valueOf(it.datotype.name)
                )
            },
            antallRetur = journalpost.antallRetur?.toInt(),
            tilleggsopplysninger = journalpost.tilleggsopplysninger?.map {
                DokumentReferanse.Tilleggsopplysning(
                    key = it.nokkel,
                    value = it.verdi,
                )
            },
            kanal = DokumentReferanse.Kanal.valueOf(journalpost.kanal.name),
            kanalnavn = journalpost.kanalnavn,
            utsendingsinfo = getUtsendingsinfo(journalpost.utsendingsinfo),
        )

        dokumentReferanse.vedlegg.addAll(getVedlegg(journalpost))

        return dokumentReferanse
    }

    private fun getUtsendingsinfo(utsendingsinfo: Utsendingsinfo?): DokumentReferanse.Utsendingsinfo? {
        if (utsendingsinfo == null) {
            return null
        }

        return with(utsendingsinfo) {
            DokumentReferanse.Utsendingsinfo(
                epostVarselSendt = if (epostVarselSendt != null) {
                    DokumentReferanse.Utsendingsinfo.EpostVarselSendt(
                        tittel = epostVarselSendt.tittel,
                        adresse = epostVarselSendt.adresse,
                        varslingstekst = epostVarselSendt.varslingstekst,
                    )
                } else null,
                smsVarselSendt = if (smsVarselSendt != null) {
                    DokumentReferanse.Utsendingsinfo.SmsVarselSendt(
                        adresse = smsVarselSendt.adresse,
                        varslingstekst = smsVarselSendt.varslingstekst,
                    )
                } else null,
                fysiskpostSendt = if (fysiskpostSendt != null) {
                    DokumentReferanse.Utsendingsinfo.FysiskpostSendt(
                        adressetekstKonvolutt = fysiskpostSendt.adressetekstKonvolutt,
                    )
                } else null,
                digitalpostSendt = if (digitalpostSendt != null) {
                    DokumentReferanse.Utsendingsinfo.DigitalpostSendt(
                        adresse = digitalpostSendt.adresse,
                    )
                } else null,
            )
        }
    }

    private fun getVedlegg(
        journalpost: Journalpost,
    ): List<DokumentReferanse.VedleggReferanse> {
        return if ((journalpost.dokumenter?.size ?: 0) > 1) {
            journalpost.dokumenter?.subList(1, journalpost.dokumenter.size)?.map { vedlegg ->
                DokumentReferanse.VedleggReferanse(
                    tittel = vedlegg.tittel,
                    dokumentInfoId = vedlegg.dokumentInfoId,
                    harTilgangTilArkivvariant = harTilgangTilArkivvariant(vedlegg),
                )
            } ?: throw RuntimeException("could not create VedleggReferanser from dokumenter")
        } else {
            emptyList()
        }
    }

    private fun harTilgangTilArkivvariant(dokumentInfo: DokumentInfo?): Boolean =
        dokumentInfo?.dokumentvarianter?.any { dv ->
            dv.variantformat == Variantformat.ARKIV && dv.saksbehandlerHarTilgang
        } == true
}
