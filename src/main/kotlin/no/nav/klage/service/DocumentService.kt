package no.nav.klage.service

import no.nav.klage.api.controller.view.DokumenterResponse
import no.nav.klage.api.controller.view.LogiskVedleggResponse
import no.nav.klage.clients.dokarkiv.DokArkivClient
import no.nav.klage.clients.saf.graphql.DokumentoversiktBruker
import no.nav.klage.clients.saf.rest.ArkivertDokument
import no.nav.klage.kodeverk.Tema
import no.nav.klage.mapper.DokumentMapper
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.stereotype.Service

@Service
class DocumentService(
    private val kabalApiService: KabalApiService,
    private val safService: SafService,
    private val dokArkivService: DokArkivService,
    private val dokArkivClient: DokArkivClient
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
                safService.getDokumentoversiktBruker(
                    idnummer = idnummer,
                    tema = temaer,
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

    fun getArkivertDokument(journalpostId: String, dokumentInfoId: String): ArkivertDokument {
        return safService.getDokument(
            journalpostId = journalpostId,
            dokumentInfoId = dokumentInfoId
        )
    }

    fun updateDocumentTitle(
        journalpostId: String,
        dokumentInfoId: String,
        title: String
    ) {
        dokArkivService.updateDocumentTitle(
            journalpostId = journalpostId,
            dokumentInfoId = dokumentInfoId,
            title = title,
        )
    }

    fun addLogiskVedlegg(dokumentInfoId: String, title: String): LogiskVedleggResponse {
        val logiskVedlegg = dokArkivClient.addLogiskVedlegg(
            dokumentInfoId = dokumentInfoId,
            title = title,
        )

        return LogiskVedleggResponse(
            tittel = title,
            logiskVedleggId = logiskVedlegg.logiskVedleggId
        )
    }

    fun updateLogiskVedlegg(dokumentInfoId: String, logiskVedleggId: String, title: String): LogiskVedleggResponse {
        dokArkivClient.updateLogiskVedlegg(
            dokumentInfoId = dokumentInfoId,
            logiskVedleggId = logiskVedleggId,
            title = title
        )

        return LogiskVedleggResponse(
            tittel = title,
            logiskVedleggId = logiskVedleggId
        )
    }

    fun deleteLogiskVedlegg(dokumentInfoId: String, logiskVedleggId: String) {
        dokArkivClient.deleteLogiskVedlegg(
            dokumentInfoId = dokumentInfoId,
            logiskVedleggId = logiskVedleggId,
        )
    }
}

