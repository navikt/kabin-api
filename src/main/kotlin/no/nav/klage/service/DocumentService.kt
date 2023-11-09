package no.nav.klage.service

import no.nav.klage.api.controller.view.DokumenterResponse
import no.nav.klage.clients.saf.graphql.Datotype
import no.nav.klage.clients.saf.graphql.DokumentoversiktBruker
import no.nav.klage.clients.saf.graphql.Journalposttype
import no.nav.klage.clients.saf.graphql.Journalstatus
import no.nav.klage.clients.saf.rest.ArkivertDokument
import no.nav.klage.exceptions.IllegalUpdateException
import no.nav.klage.kodeverk.Tema
import no.nav.klage.mapper.DokumentMapper
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class DocumentService(
    private val kabalApiService: KabalApiService,
    private val safService: SafService,
    private val dokArkivService: DokArkivService
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
        validateJournalpostChange(journalpostId = journalpostId)

        dokArkivService.updateDocumentTitle(
            journalpostId = journalpostId,
            dokumentInfoId = dokumentInfoId,
            title = title,
        )
    }

    private fun validateJournalpostChange(journalpostId: String) {
        val journalpost = safService.getJournalpostAsSaksbehandler(journalpostId = journalpostId)
        val datoJournalfoert = journalpost?.relevanteDatoer?.find { it.datotype == Datotype.DATO_JOURNALFOERT }?.dato
        val journalpostType = journalpost?.journalposttype
        val journalStatus = journalpost?.journalstatus

        if (journalpostType == Journalposttype.I
            && journalStatus == Journalstatus.JOURNALFOERT
            && datoJournalfoert?.isBefore(LocalDateTime.now().minusYears(1)) == true) {
            throw IllegalUpdateException("Kan ikke oppdatere tittel på inngående dokument journalført for over et år siden.")
        }
    }
}

