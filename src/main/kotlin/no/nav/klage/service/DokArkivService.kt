package no.nav.klage.service

import no.nav.klage.clients.KabalApiClient
import no.nav.klage.clients.dokarkiv.*
import no.nav.klage.clients.saf.graphql.Journalpost
import no.nav.klage.clients.saf.graphql.Journalposttype
import no.nav.klage.clients.saf.graphql.Journalstatus
import no.nav.klage.clients.saf.graphql.SafGraphQlClient
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.Tema
import no.nav.klage.kodeverk.Ytelse
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service
import java.util.*

@Service
class DokArkivService(
    private val dokArkivClient: DokArkivClient,
    private val kabalApiService: KabalApiService,
    private val safGraphQlClient: SafGraphQlClient,
    private val tokenUtil: TokenUtil
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

        if (journalpostCanBeFinalized(journalpostInSaf)) {
            logger.debug("Finalizing journalpost $journalpostId in Dokarkiv")
            dokArkivClient.finalizeJournalpost(journalpostId, journalfoerendeEnhet)
        } else {
            //TODO: Sjekk hvor vanlig dette er, og om det heller bør være en warning.
            logger.debug("Journalpost $journalpostId already finalized. Returning.")
        }
    }

    private fun journalpostCanBeFinalized(journalpostInSaf: Journalpost): Boolean {
        return when (journalpostInSaf.journalstatus) {
            Journalstatus.MOTTATT,
            Journalstatus.UNDER_ARBEID,
            Journalstatus.RESERVERT,
            Journalstatus.OPPLASTING_DOKUMENT,
            Journalstatus.UKJENT_BRUKER,
            Journalstatus.FERDIGSTILT -> true

            else -> false
        }
    }

    fun updateSaksIdInJournalpost(
        journalpostId: String,
        completedKlagebehandling: KabalApiClient.CompletedKlagebehandling
    ) {
        val journalpostInSaf = safGraphQlClient.getJournalpostAsSaksbehandler(journalpostId)
            ?: throw Exception("Journalpost with id $journalpostId not found in SAF")

        if (journalpostCanBeUpdated(journalpostInSaf)) {
            dokArkivClient.updateSaksId(
                journalpostId = journalpostId,
                input = UpdateJournalpostSaksIdRequest(
                    tema = Ytelse.of(completedKlagebehandling.ytelseId).toTema(),
                    bruker = getBruker(completedKlagebehandling.sakenGjelder),
                    sak = getSak(completedKlagebehandling),
                    journalfoerendeEnhet = completedKlagebehandling.klageBehandlendeEnhet
                )
            )
        } else {
            //TODO: Sjekk hvor vanlig dette er, og om det heller bør være en warning.
            logger.debug("Saksid can't be updated for journalpost with type ${journalpostInSaf.journalposttype} and status ${journalpostInSaf.journalstatus}")
        }
    }

    private fun journalpostCanBeUpdated(journalpostInSaf: Journalpost): Boolean {
        return when (journalpostInSaf.journalposttype) {
            Journalposttype.I -> {
                when (journalpostInSaf.journalstatus) {
                    Journalstatus.MOTTATT,
                    Journalstatus.OPPLASTING_DOKUMENT,
                    Journalstatus.UKJENT_BRUKER,
                    Journalstatus.UTGAAR -> true

                    Journalstatus.JOURNALFOERT -> false

                    else -> throw RuntimeException("Invalid Journalstatus for journalposttype I")
                }
            }

            Journalposttype.U -> {
                when (journalpostInSaf.journalstatus) {
                    Journalstatus.RESERVERT,
                    Journalstatus.UNDER_ARBEID,
                    Journalstatus.AVBRUTT -> true

                    Journalstatus.FERDIGSTILT,
                    Journalstatus.EKSPEDERT -> false

                    else -> throw RuntimeException("Invalid Journalstatus for journalposttype U")
                }
            }

            Journalposttype.N -> {
                when (journalpostInSaf.journalstatus) {
                    Journalstatus.UNDER_ARBEID,
                    Journalstatus.AVBRUTT -> true

                    Journalstatus.FERDIGSTILT,
                    Journalstatus.EKSPEDERT -> false

                    else -> throw RuntimeException("Invalid Journalstatus for journalposttype N")
                }
            }

            null -> throw RuntimeException("Invalid Journalstatus")
        }
    }

    fun handleJournalpost(journalpostId: String, klagebehandlingId: UUID): String {
        val completedKlagebehandling =
            kabalApiService.getCompletedKlagebehandling(klagebehandlingId = klagebehandlingId)
        val journalpostInSaf = safGraphQlClient.getJournalpostAsSaksbehandler(journalpostId)
            ?: throw Exception("Journalpost with id $journalpostId not found in SAF")

        if (journalpostCanBeUpdated(journalpostInSaf)) {
            updateSaksIdInJournalpost(
                journalpostId = journalpostId,
                completedKlagebehandling = completedKlagebehandling
            )
            finalizeJournalpost(
                journalpostId = journalpostId,
                journalfoerendeEnhet = completedKlagebehandling.klageBehandlendeEnhet
            )
            return journalpostId
        } else {
            return if (journalpostAndCompletedKlagebehandlingHaveTheSameFagsak(
                    journalpostInSaf = journalpostInSaf,
                    completedKlagebehandling = completedKlagebehandling
                )
            ) {
                journalpostId
            } else {
                val newJournalpostId = createNewJournalpostBasedOnExistingJournalpost(
                    oldJournalpost = journalpostInSaf,
                    completedKlagebehandling = completedKlagebehandling
                )
                dokArkivClient.registerErrorInSaksId(journalpostId)
                newJournalpostId
            }
        }
    }

    private fun createNewJournalpostBasedOnExistingJournalpost(
        oldJournalpost: Journalpost,
        completedKlagebehandling: KabalApiClient.CompletedKlagebehandling
    ): String {
        val requestPayload = CreateNewJournalpostBasedOnExistingJournalpostRequest(
            sakstype = Sakstype.FAGSAK,
            fagsakId = completedKlagebehandling.sakFagsakId,
            fagsaksystem = FagsaksSystem.valueOf(completedKlagebehandling.sakFagsystem.name),
            tema = Tema.valueOf(oldJournalpost.tema.name),
            bruker = getBruker(completedKlagebehandling.sakenGjelder),
            //TODO: Må lese ut enhet fra innlogget, dette blir feil.
            journalfoerendeEnhet = oldJournalpost.journalfoerendeEnhet!!
        )

        return dokArkivClient.createNewJournalpostBasedOnExistingJournalpost(
            payload = requestPayload,
            oldJournalpostId = oldJournalpost.journalpostId,
            journalfoerendeSaksbehandlerIdent = tokenUtil.getIdent()
        ).newJournalpostId
    }

    private fun journalpostAndCompletedKlagebehandlingHaveTheSameFagsak(
        journalpostInSaf: Journalpost,
        completedKlagebehandling: KabalApiClient.CompletedKlagebehandling
    ): Boolean {
        return if (journalpostInSaf.sak?.fagsakId == null || journalpostInSaf.sak.fagsaksystem == null) {
            false
        } else (journalpostInSaf.sak.fagsakId == completedKlagebehandling.sakFagsakId
                && Fagsystem.valueOf(journalpostInSaf.sak.fagsaksystem) == completedKlagebehandling.sakFagsystem)
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