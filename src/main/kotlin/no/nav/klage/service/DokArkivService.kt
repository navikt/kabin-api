package no.nav.klage.service

import no.nav.klage.clients.KabalApiClient
import no.nav.klage.clients.dokarkiv.*
import no.nav.klage.clients.saf.graphql.Journalstatus
import no.nav.klage.clients.saf.graphql.SafGraphQlClient
import no.nav.klage.kodeverk.Ytelse
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service
import java.util.*

@Service
class DokArkivService(
    private val dokArkivClient: DokArkivClient,
    private val kabalApiService: KabalApiService,
    private val safGraphQlClient: SafGraphQlClient
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

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
        val journalpostInSaf = safGraphQlClient.getJournalpostAsSaksbehandler(journalpostId)
            ?: throw Exception("Journalpost with id $journalpostId not found in SAF")

        //TODO: Må tilpasse denne sjekken dersom vi skal kunne bruke f.eks. Notater her.
        if (journalpostInSaf.journalstatus != Journalstatus.JOURNALFOERT) {
            logger.debug("Finalizing journalpost $journalpostId in Dokarkiv")
            dokArkivClient.finalizeJournalpostOnBehalfOf(journalpostId, journalfoerendeEnhet)
        } else {
            logger.debug("Journalpost $journalpostId already finalized. Returning.")
        }
    }

    fun updateSaksIdInJournalpost(
        journalpostId: String,
        completedKlagebehandling: KabalApiClient.CompletedKlagebehandling
    ) {
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
        val completedKlagebehandling =
            kabalApiService.getCompletedKlagebehandling(klagebehandlingId = klagebehandlingId)
        updateSaksIdInJournalpost(
            journalpostId = journalpostId,
            completedKlagebehandling = completedKlagebehandling
        )
        finalizeJournalpost(
            journalpostId = journalpostId,
            journalfoerendeEnhet = completedKlagebehandling.klageBehandlendeEnhet

        )
    }

    fun updateDocumentTitle(
        journalpostId: String,
        dokumentInfoId: String,
        title: String
    ) {
        dokArkivClient.updateDocumentTitle(
            journalpostId = journalpostId,
            input = createUpdateDocumentTitleJournalpostInput(
                dokumentInfoId = dokumentInfoId,
                title = title
            )
        )
    }

    private fun createUpdateDocumentTitleJournalpostInput(
        dokumentInfoId: String,
        title: String
    ): UpdateDocumentTitleJournalpostInput {
        return UpdateDocumentTitleJournalpostInput(
            dokumenter = listOf(
                UpdateDocumentTitleDokumentInput(
                    dokumentInfoId = dokumentInfoId,
                    tittel = title
                )
            )
        )
    }
}