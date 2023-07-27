package no.nav.klage.service

import no.nav.klage.clients.saf.graphql.*
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

    fun getDokument(journalpostId: String, dokumentInfoId: String): ArkivertDokument {
        return safRestClient.getDokument(dokumentInfoId, journalpostId)
    }

    private fun mapTema(temaer: List<Tema>): List<no.nav.klage.clients.saf.graphql.Tema> =
        temaer.map { tema -> no.nav.klage.clients.saf.graphql.Tema.valueOf(tema.name) }

    fun getJournalpostAsSaksbehandler(journalpostId: String): Journalpost? {
        return safGraphQlClient.getJournalpostAsSaksbehandler(journalpostId)
    }
}