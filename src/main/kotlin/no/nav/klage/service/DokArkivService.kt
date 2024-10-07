package no.nav.klage.service

import no.nav.klage.api.controller.view.PartIdInput
import no.nav.klage.api.controller.view.PartType
import no.nav.klage.api.controller.view.SearchPartInput
import no.nav.klage.clients.KabalInnstillingerClient
import no.nav.klage.clients.dokarkiv.*
import no.nav.klage.clients.oppgaveapi.FerdigstillOppgaveRequest
import no.nav.klage.clients.oppgaveapi.OppgaveClient
import no.nav.klage.clients.saf.graphql.Journalpost
import no.nav.klage.clients.saf.graphql.Journalposttype
import no.nav.klage.clients.saf.graphql.Journalstatus
import no.nav.klage.domain.CreateBehandlingInput
import no.nav.klage.domain.entities.Mulighet
import no.nav.klage.exceptions.InvalidProperty
import no.nav.klage.exceptions.SectionedValidationErrorWithDetailsException
import no.nav.klage.exceptions.ValidationSection
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.PartIdType
import no.nav.klage.kodeverk.Tema
import no.nav.klage.util.MulighetSource
import no.nav.klage.util.canChangeAvsenderInJournalpost
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.stereotype.Service

@Service
class DokArkivService(
    private val dokArkivClient: DokArkivClient,
    private val safService: SafService,
    private val kabalInnstillingerClient: KabalInnstillingerClient,
    private val kabalApiService: KabalApiService,
    private val oppgaveClient: OppgaveClient,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    private fun finalizeJournalpost(journalpostId: String, journalfoerendeEnhet: String) {
        val journalpostInSaf = safService.getJournalpostAsSaksbehandler(journalpostId)
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

    private fun updateAvsenderInJournalpost(
        journalpostId: String,
        avsender: PartIdInput,
    ) {
        val requestInput = getUpdateAvsenderMottakerInJournalpostRequest(avsender)

        dokArkivClient.updateAvsenderMottakerInJournalpost(
            journalpostId = journalpostId,
            input = requestInput,
        )
    }

    private fun getUpdateAvsenderMottakerInJournalpostRequest(avsender: PartIdInput): UpdateAvsenderMottakerInJournalpostRequest {
        val avsenderPart = kabalApiService.searchPart(
            searchPartInput = SearchPartInput(identifikator = avsender.id)
        )
        return UpdateAvsenderMottakerInJournalpostRequest(
            avsenderMottaker = AvsenderMottaker(
                id = avsender.id,
                idType = avsender.type.toAvsenderMottakerIdType(),
                navn = avsenderPart.name
            ),
        )
    }

    private fun updateSakInJournalpost(
        journalpostId: String,
        tema: Tema,
        bruker: Bruker,
        sak: Sak,
        journalfoerendeEnhet: String
    ) {
        val requestInput = UpdateSakInJournalpostRequest(
            tema = tema,
            bruker = bruker,
            sak = sak,
            journalfoerendeEnhet = journalfoerendeEnhet,
        )

        dokArkivClient.updateSakInJournalpost(
            journalpostId = journalpostId,
            input = requestInput,
        )
    }

    private fun Journalpost.isFinalized(): Boolean {
        return when (journalposttype) {
            Journalposttype.I -> {
                when (journalstatus) {
                    Journalstatus.JOURNALFOERT -> true
                    else -> false
                }
            }

            Journalposttype.U -> {
                when (journalstatus) {
                    Journalstatus.FERDIGSTILT,
                    Journalstatus.EKSPEDERT -> true

                    else -> false
                }
            }

            Journalposttype.N -> {
                when (journalstatus) {
                    Journalstatus.FERDIGSTILT,
                    Journalstatus.EKSPEDERT -> true

                    else -> false
                }
            }
        }
    }

    private fun handleJournalpostBasedOnInfotrygdSak(
        journalpostId: String,
        mulighet: Mulighet,
        avsender: PartIdInput?,
    ): String {
        return handleJournalpost(
            journalpostId = journalpostId,
            avsender = avsender,
            tema = mulighet.tema,
            bruker = Bruker(
                id = mulighet.sakenGjelder.part.value, idType = when (mulighet.sakenGjelder.part.type) {
                    PartIdType.PERSON -> {
                        BrukerIdType.FNR
                    }

                    else -> {
                        BrukerIdType.ORGNR
                    }
                }
            ),
            sakInFagsystem = Sak(
                sakstype = Sakstype.FAGSAK,
                fagsaksystem = FagsaksSystem.IT01,
                fagsakid = mulighet.fagsakId
            ),
            journalfoerendeEnhet = kabalInnstillingerClient.getBrukerdata().ansattEnhet.id,
        )
    }

    fun handleJournalpost(
        mulighet: Mulighet, journalpostId: String, avsender: PartIdInput?
    ): String {
        return when (MulighetSource.of(mulighet.currentFagsystem)) {
            MulighetSource.INFOTRYGD -> handleJournalpostBasedOnInfotrygdSak(
                journalpostId = journalpostId,
                mulighet = mulighet,
                avsender = avsender,
            )

            MulighetSource.KABAL -> handleJournalpostBasedOnKabalKlagebehandling(
                journalpostId = journalpostId,
                mulighet = mulighet,
                avsender = avsender,
            )
        }
    }

    fun handleJournalpostBasedOnKabalKlagebehandling(
        journalpostId: String,
        mulighet: Mulighet,
        avsender: PartIdInput?,
    ): String {
        return handleJournalpost(
            journalpostId = journalpostId,
            avsender = avsender,
            tema = mulighet.tema,
            bruker = Bruker(
                id = mulighet.sakenGjelder.part.value, idType = when (mulighet.sakenGjelder.part.type) {
                    PartIdType.PERSON -> {
                        BrukerIdType.FNR
                    }

                    else -> {
                        BrukerIdType.ORGNR
                    }
                }
            ),
            sakInFagsystem = Sak(
                sakstype = Sakstype.FAGSAK,
                fagsaksystem = FagsaksSystem.valueOf(mulighet.originalFagsystem.navn),
                fagsakid = mulighet.fagsakId
            ),
            journalfoerendeEnhet = mulighet.klageBehandlendeEnhet,
        )
    }

    private fun handleJournalpost(
        journalpostId: String,
        avsender: PartIdInput?,
        tema: Tema,
        bruker: Bruker,
        sakInFagsystem: Sak,
        journalfoerendeEnhet: String,
    ): String {
        logger.debug("handleJournalpost called")
        val journalpostInSaf = safService.getJournalpostAsSaksbehandler(journalpostId)
            ?: throw Exception("Journalpost with id $journalpostId not found in SAF")

        secureLogger.debug(
            "handleJournalpost called. Fetched journalpostInSaf: {}, sak: {}, tema: {}",
            journalpostInSaf,
            sakInFagsystem,
            tema
        )

        val journalpostType = journalpostInSaf.journalposttype

        if (journalpostType == Journalposttype.I
            && avsenderMottakerIsMissing(journalpostInSaf.avsenderMottaker)
            && !journalpostInSaf.isFinalized()
            && avsender == null
        ) {
            throw SectionedValidationErrorWithDetailsException(
                title = "Validation error",
                sections = listOf(
                    ValidationSection(
                        section = "saksdata",
                        properties = listOf(
                            InvalidProperty(
                                field = CreateBehandlingInput::avsender.name,
                                reason = "Velg en avsender."
                            )
                        )
                    )
                )
            )
        }

        if (journalpostType == Journalposttype.I && avsender != null) {
            if (journalpostInSaf.avsenderMottaker?.id != avsender.id) {
                if (canChangeAvsenderInJournalpost(journalpostInSaf)) {
                    logger.debug("updating avsender in journalpost")
                    updateAvsenderInJournalpost(
                        journalpostId = journalpostId,
                        avsender = avsender,
                    )
                }
            }
        }

        if (journalpostInSaf.isFinalized()) {
            return if (journalpostIsConnectedToSakInFagsystem(
                    journalpostInSaf = journalpostInSaf,
                    sakInFagsystem = sakInFagsystem,
                )
            ) {
                logger.debug("journalpostIsConnectedToSakInFagsystem, no changes to journalpost.")
                journalpostId
            } else {
                logger.debug(
                    "createNewJournalpostBasedOnExistingJournalpost. Old journalpost id: {}",
                    journalpostInSaf.journalpostId
                )
                secureLogger.debug(
                    "createNewJournalpostBasedOnExistingJournalpost. JournalpostinSaf: {}, sak: {}",
                    journalpostInSaf,
                    sakInFagsystem
                )
                val newJournalpostId = createNewJournalpostBasedOnExistingJournalpost(
                    oldJournalpost = journalpostInSaf,
                    sak = sakInFagsystem,
                    tema = tema,
                    bruker = bruker,
                    journalfoerendeEnhet = journalfoerendeEnhet,
                )
                newJournalpostId
            }
        } else {
            logger.debug("Journalpost is not finalized")
            secureLogger.debug("Journalpost is not finalized: {}", journalpostInSaf)

            if (!journalpostIsConnectedToSakInFagsystem(
                    journalpostInSaf = journalpostInSaf,
                    sakInFagsystem = sakInFagsystem,
                )
            ) {
                updateSakInJournalpost(
                    journalpostId = journalpostId,
                    tema = tema,
                    bruker = bruker,
                    sak = sakInFagsystem,
                    journalfoerendeEnhet = journalfoerendeEnhet,
                )
            }

            finalizeJournalpost(
                journalpostId = journalpostId,
                journalfoerendeEnhet = journalfoerendeEnhet,
            )

            logger.debug("About to fetch journalfoeringsoppgave")
            val oppgave = oppgaveClient.fetchJournalfoeringsoppgave(
                journalpostId = journalpostId,
            )

            if (oppgave == null) {
                logger.warn("No journalfoeringsoppgave found")
                return journalpostId
            }

            oppgaveClient.ferdigstillOppgave(
                FerdigstillOppgaveRequest(
                    oppgaveId = oppgave.id,
                    versjon = oppgave.versjon,
                )
            )
            logger.debug("Ferdigstilt journalfoeringsoppgave")

            return journalpostId
        }
    }

    fun journalpostIsFinalizedAndConnectedToFagsak(
        journalpostId: String,
        fagsakId: String,
        fagsystemId: String
    ): Boolean {
        val journalpostInSaf = safService.getJournalpostAsSaksbehandler(journalpostId)
            ?: throw Exception("Journalpost with id $journalpostId not found in SAF")

        if (!journalpostInSaf.isFinalized()) {
            return false
        }

        val fagsystem = Fagsystem.of(fagsystemId)

        return !journalpostIsConnectedToSakInFagsystem(
            journalpostInSaf = journalpostInSaf,
            sakInFagsystem = Sak(
                sakstype = Sakstype.FAGSAK,
                fagsaksystem = FagsaksSystem.valueOf(fagsystem.name),
                fagsakid = fagsakId,
            )
        )
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
        sak: Sak,
        tema: Tema,
        bruker: Bruker,
        journalfoerendeEnhet: String,
    ): String {
        val requestPayload = CreateNewJournalpostBasedOnExistingJournalpostRequest(
            sakstype = Sakstype.FAGSAK,
            fagsakId = sak.fagsakid,
            fagsaksystem = sak.fagsaksystem,
            tema = tema,
            bruker = bruker,
            journalfoerendeEnhet = journalfoerendeEnhet,
        )

        return dokArkivClient.createNewJournalpostBasedOnExistingJournalpost(
            payload = requestPayload,
            oldJournalpostId = oldJournalpost.journalpostId,
        ).nyJournalpostId
    }

    private fun journalpostIsConnectedToSakInFagsystem(
        journalpostInSaf: Journalpost,
        sakInFagsystem: Sak,
    ): Boolean {
        return if (journalpostInSaf.sak?.fagsakId == null || journalpostInSaf.sak.fagsaksystem == null) {
            false
        } else {
            logger.debug(
                "journalpostInSaf.sak.fagsaksystem: {}, sak.fagsaksystem: {}",
                FagsaksSystem.valueOf(journalpostInSaf.sak.fagsaksystem),
                sakInFagsystem.fagsaksystem
            )
            (journalpostInSaf.sak.fagsakId == sakInFagsystem.fagsakid
                    && FagsaksSystem.valueOf(journalpostInSaf.sak.fagsaksystem) == sakInFagsystem.fagsaksystem)
        }
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

    private fun PartType.toAvsenderMottakerIdType(): AvsenderMottakerIdType {
        return when (this) {
            PartType.FNR -> AvsenderMottakerIdType.FNR
            PartType.ORGNR -> AvsenderMottakerIdType.ORGNR
        }
    }
}