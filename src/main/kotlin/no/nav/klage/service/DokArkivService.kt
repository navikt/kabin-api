package no.nav.klage.service

import no.nav.klage.api.controller.view.CreateAnkeBasedOnKlagebehandling
import no.nav.klage.clients.KabalApiClient
import no.nav.klage.clients.dokarkiv.*
import no.nav.klage.clients.saf.graphql.Journalpost
import no.nav.klage.clients.saf.graphql.Journalposttype
import no.nav.klage.clients.saf.graphql.Journalstatus
import no.nav.klage.clients.saf.graphql.SafGraphQlClient
import no.nav.klage.exceptions.InvalidProperty
import no.nav.klage.exceptions.SectionedValidationErrorWithDetailsException
import no.nav.klage.exceptions.ValidationSection
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.Ytelse
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
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
        private val secureLogger = getSecureLogger()
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

    fun getSak(klagebehandling: KabalApiClient.CompletedKlagebehandling): Sak {
        return Sak(
            sakstype = Sakstype.FAGSAK,
            fagsaksystem = FagsaksSystem.valueOf(klagebehandling.fagsystem.name),
            fagsakid = klagebehandling.fagsakId
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

    fun updateJournalpost(
        journalpostId: String,
        completedKlagebehandling: KabalApiClient.CompletedKlagebehandling,
        avsender: CreateAnkeBasedOnKlagebehandling.OversendtPartId?,
        journalpostType: Journalposttype
    ) {
        val requestInput = UpdateJournalpostRequest(
            tema = Ytelse.of(completedKlagebehandling.ytelseId).toTema(),
            bruker = getBruker(completedKlagebehandling.sakenGjelder),
            sak = getSak(completedKlagebehandling),
            journalfoerendeEnhet = completedKlagebehandling.klageBehandlendeEnhet,
            avsenderMottaker = null,
        )

        if (journalpostType != Journalposttype.N && avsender != null) {
            logger.debug("Including AvsenderMottaker in update request.")
            requestInput.avsenderMottaker = AvsenderMottaker(
                id = avsender.value,
                idType = avsender.type.toAvsenderMottakerIdType(),
            )
        }

        dokArkivClient.updateJournalpost(
            journalpostId = journalpostId,
            input = requestInput,
        )
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
        }
    }

    fun handleJournalpost(
        journalpostId: String,
        klagebehandlingId: UUID,
        avsender: CreateAnkeBasedOnKlagebehandling.OversendtPartId? = null
    ): String {
        val completedKlagebehandling =
            kabalApiService.getCompletedKlagebehandling(klagebehandlingId = klagebehandlingId)
        val journalpostInSaf = safGraphQlClient.getJournalpostAsSaksbehandler(journalpostId)
            ?: throw Exception("Journalpost with id $journalpostId not found in SAF")

        if (journalpostCanBeUpdated(journalpostInSaf)) {
            secureLogger.debug("Journalpost: {}", journalpostInSaf)
            if (journalpostInSaf.journalposttype != Journalposttype.N
                && avsenderMottakerIsMissing(journalpostInSaf.avsenderMottaker)
                && avsender == null
            ) {
                throw SectionedValidationErrorWithDetailsException(
                    title = "Validation error",
                    sections = listOf(
                        ValidationSection(
                            section = "saksdata",
                            properties = listOf(
                                InvalidProperty(
                                    field = CreateAnkeBasedOnKlagebehandling::avsender.name,
                                    reason = "Avsender må velges på denne journalposten"
                                )
                            )
                        )
                    )
                )
            }

            updateJournalpost(
                journalpostId = journalpostId,
                completedKlagebehandling = completedKlagebehandling,
                avsender = avsender,
                journalpostType = journalpostInSaf.journalposttype
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

    private fun avsenderMottakerIsMissing(avsenderMottaker: no.nav.klage.clients.saf.graphql.AvsenderMottaker?): Boolean {
        return if (avsenderMottaker == null) {
            true
        } else if (avsenderMottaker.type == no.nav.klage.clients.saf.graphql.AvsenderMottaker.AvsenderMottakerIdType.FNR) {
            avsenderMottaker.id == null
        } else avsenderMottaker.navn == null
    }

    private fun createNewJournalpostBasedOnExistingJournalpost(
        oldJournalpost: Journalpost,
        completedKlagebehandling: KabalApiClient.CompletedKlagebehandling
    ): String {
        val requestPayload = CreateNewJournalpostBasedOnExistingJournalpostRequest(
            sakstype = Sakstype.FAGSAK,
            fagsakId = completedKlagebehandling.fagsakId,
            fagsaksystem = FagsaksSystem.valueOf(completedKlagebehandling.fagsystem.name),
            tema = Ytelse.of(completedKlagebehandling.ytelseId).toTema(),
            bruker = getBruker(completedKlagebehandling.sakenGjelder),
            journalfoerendeEnhet = completedKlagebehandling.klageBehandlendeEnhet
        )

        return dokArkivClient.createNewJournalpostBasedOnExistingJournalpost(
            payload = requestPayload,
            oldJournalpostId = oldJournalpost.journalpostId,
            journalfoerendeSaksbehandlerIdent = tokenUtil.getIdent()
        ).nyJournalpostId
    }

    private fun journalpostAndCompletedKlagebehandlingHaveTheSameFagsak(
        journalpostInSaf: Journalpost,
        completedKlagebehandling: KabalApiClient.CompletedKlagebehandling
    ): Boolean {
        return if (journalpostInSaf.sak?.fagsakId == null || journalpostInSaf.sak.fagsaksystem == null) {
            false
        } else (journalpostInSaf.sak.fagsakId == completedKlagebehandling.fagsakId
                && Fagsystem.valueOf(journalpostInSaf.sak.fagsaksystem) == completedKlagebehandling.fagsystem)
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

    private fun CreateAnkeBasedOnKlagebehandling.OversendtPartIdType.toAvsenderMottakerIdType(): AvsenderMottakerIdType {
        return when (this) {
            CreateAnkeBasedOnKlagebehandling.OversendtPartIdType.PERSON -> AvsenderMottakerIdType.FNR
            CreateAnkeBasedOnKlagebehandling.OversendtPartIdType.VIRKSOMHET -> AvsenderMottakerIdType.ORGNR
        }
    }
}