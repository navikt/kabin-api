package no.nav.klage.service

import no.nav.klage.api.controller.view.DokumentReferanse
import no.nav.klage.clients.saf.graphql.DokumentoversiktBruker
import no.nav.klage.clients.saf.graphql.Journalpost
import no.nav.klage.clients.saf.graphql.SafGraphQlClient
import no.nav.klage.clients.saf.rest.ArkivertDokument
import no.nav.klage.clients.saf.rest.SafRestClient
import no.nav.klage.kodeverk.Tema
import org.springframework.stereotype.Service

@Service
class SafService(
    private val safGraphQlClient: SafGraphQlClient,
    private val safRestClient: SafRestClient,
) {

    fun getDokumentoversiktBruker(
        idnummer: String,
        tema: List<Tema>,
        pageSize: Int,
        previousPageRef: String? = null
    ): DokumentoversiktBruker {
        return safGraphQlClient.getDokumentoversiktBruker(
            idnummer = idnummer,
            tema = mapTema(tema),
            pageSize = pageSize,
            previousPageRef = previousPageRef,
        )
    }

    fun getDokument(
        journalpostId: String,
        dokumentInfoId: String,
        variantFormat: DokumentReferanse.Variant.Format,
    ): ArkivertDokument {
        return safRestClient.getDokument(
            dokumentInfoId = dokumentInfoId,
            journalpostId = journalpostId,
            variantFormat = variantFormat.name,
        )
    }

    private fun mapTema(temaer: List<Tema>): List<no.nav.klage.clients.saf.graphql.Tema> =
        temaer.map { tema -> no.nav.klage.clients.saf.graphql.Tema.valueOf(tema.name) }

    fun getJournalpostAsSaksbehandler(journalpostId: String): Journalpost? {
        return safGraphQlClient.getJournalpostAsSaksbehandler(journalpostId)
    }
}