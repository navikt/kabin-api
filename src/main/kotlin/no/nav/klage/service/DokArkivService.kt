package no.nav.klage.service

import no.nav.klage.clients.dokarkiv.*
import no.nav.klage.kodeverk.Fagsystem
import org.springframework.stereotype.Service

@Service
class DokArkivService(
    private val dokArkivClient: DokArkivClient
) {
    fun updateSaksIdInJournalpost(journalpostId: String, sakFagsakId: String, sakFagsystem: Fagsystem) {
        dokArkivClient.updateDocumentTitleOnBehalfOf(
            journalpostId = journalpostId,
            input = UpdateJournalpostSaksIdRequest(
                sak = Sak(sakstype = Sakstype.FAGSAK,
                    fagsaksystem = FagsaksSystem.valueOf(sakFagsystem.name), fagsakid = sakFagsakId)
            )
        )
    }
}