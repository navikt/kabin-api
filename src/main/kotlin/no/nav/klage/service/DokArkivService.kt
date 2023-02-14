package no.nav.klage.service

import no.nav.klage.clients.KabalApiClient
import no.nav.klage.clients.dokarkiv.*
import no.nav.klage.kodeverk.Ytelse
import org.springframework.stereotype.Service
import java.util.*

@Service
class DokArkivService(
    private val dokArkivClient: DokArkivClient,
    private val kabalApiService: KabalApiService
) {
    private fun getBruker(sakenGjelder: KabalApiClient.SakenGjelderView): Bruker {
        return if (sakenGjelder.person != null) {
            Bruker(
                id = sakenGjelder.person.foedselsnummer!!,
                idType = BrukerIdType.FNR,
            )
        } else if (sakenGjelder.virksomhet != null) {
            Bruker(
                id = sakenGjelder.virksomhet.virksomhetsnummer!!,
                idType = BrukerIdType.ORGNR
            )
        } else throw Exception("Error in sakenGjelder.")
    }

    private fun getSak(klagebehandling: KabalApiClient.CompletedKlagebehandling): Sak {
        return Sak(
            sakstype = Sakstype.FAGSAK,
            fagsaksystem = FagsaksSystem.valueOf(klagebehandling.sakFagsystem.name),
            fagsakid = klagebehandling.sakFagsakId
        )
    }

    fun finalizeJournalpost(journalpostId: String, journalfoerendeEnhet: String) {
        dokArkivClient.finalizeJournalpost(journalpostId, journalfoerendeEnhet)
    }

    fun updateSaksIdInJournalpost(journalpostId: String, completedKlagebehandling: KabalApiClient.CompletedKlagebehandling) {

        dokArkivClient.updateDocumentTitleOnBehalfOf(
            journalpostId = journalpostId,
            input = UpdateJournalpostSaksIdRequest(
                tema = Ytelse.of(completedKlagebehandling.ytelseId).toTema(),
                bruker = getBruker(completedKlagebehandling.sakenGjelder),
                sak = getSak(completedKlagebehandling),
                journalfoerendeEnhet = completedKlagebehandling.klageBehandlendeEnhet
            )
        )
    }

    fun updateSaksIdAndFinalizeJournalpost(journalpostId: String, klagebehandlingId: UUID) {
        val completedKlagebehandling = kabalApiService.getCompletedKlagebehandling(klagebehandlingId = klagebehandlingId)
        updateSaksIdInJournalpost(
            journalpostId = journalpostId,
            completedKlagebehandling = completedKlagebehandling
        )
        finalizeJournalpost(
            journalpostId = journalpostId,
            journalfoerendeEnhet = completedKlagebehandling.klageBehandlendeEnhet

        )
    }
}