package no.nav.klage.service

import no.nav.klage.api.controller.view.CreateAnkeInputView
import no.nav.klage.api.controller.view.PartId
import no.nav.klage.api.controller.view.PartView
import no.nav.klage.api.controller.view.SearchPartInput
import no.nav.klage.clients.KabalInnstillingerClient
import no.nav.klage.clients.KlageFssProxyClient
import no.nav.klage.clients.dokarkiv.*
import no.nav.klage.clients.kabalapi.CompletedKlagebehandling
import no.nav.klage.clients.saf.graphql.Journalpost
import no.nav.klage.clients.saf.graphql.Journalposttype
import no.nav.klage.clients.saf.graphql.Journalstatus
import no.nav.klage.clients.saf.graphql.SafGraphQlClient
import no.nav.klage.exceptions.InvalidProperty
import no.nav.klage.exceptions.SectionedValidationErrorWithDetailsException
import no.nav.klage.exceptions.ValidationSection
import no.nav.klage.kodeverk.Tema
import no.nav.klage.kodeverk.Ytelse
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.stereotype.Service
import java.util.*

@Service
class DokArkivService(
    private val dokArkivClient: DokArkivClient,
    private val genericApiService: GenericApiService,
    private val safGraphQlClient: SafGraphQlClient,
    private val tokenUtil: TokenUtil,
    private val fssProxyClient: KlageFssProxyClient,
    private val kabalInnstillingerClient: KabalInnstillingerClient
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    private fun getBruker(sakenGjelder: no.nav.klage.clients.kabalapi.PartView): Bruker {
        return Bruker(
            id = sakenGjelder.id,
            idType = BrukerIdType.valueOf(sakenGjelder.type.name)
        )
    }

    fun getSak(klagebehandling: CompletedKlagebehandling): Sak {
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

    fun updateAvsenderInJournalpost(
        journalpostId: String,
        avsender: PartId,
    ) {
        val requestInput = getUpdateAvsenderMottakerInJournalpostRequest(avsender)

        dokArkivClient.updateAvsenderMottakerInJournalpost(
            journalpostId = journalpostId,
            input = requestInput,
        )
    }

    private fun getUpdateAvsenderMottakerInJournalpostRequest(avsender: PartId): UpdateAvsenderMottakerInJournalpostRequest {
        val avsenderPart = genericApiService.searchPart(
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

    fun updateSakInJournalpost(
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

    fun handleJournalpostBasedOnInfotrygdSak(
        journalpostId: String,
        sakId: String,
        avsender: PartId?,
    ): String {
        val sakFromKlanke = fssProxyClient.getSak(sakId)

        return handleJournalpost(
            journalpostId = journalpostId,
            avsender = avsender,
            tema = Tema.valueOf(sakFromKlanke.tema),
            bruker = Bruker(
                id = sakFromKlanke.fnr, idType = BrukerIdType.FNR
            ),
            sak = Sak(
                sakstype = Sakstype.FAGSAK,
                fagsaksystem = FagsaksSystem.IT01,
                fagsakid = sakFromKlanke.fagsakId
            ),
            journalfoerendeEnhet = kabalInnstillingerClient.getBrukerdata().ansattEnhet.id,
        )
    }

    fun handleJournalpostBasedOnKabalKlagebehandling(
        journalpostId: String,
        klagebehandlingId: UUID,
        avsender: PartId?,
    ): String {
        val completedKlagebehandling =
        genericApiService.getCompletedKlagebehandling(klagebehandlingId = klagebehandlingId)

        return handleJournalpost(
            journalpostId = journalpostId,
            avsender = avsender,
            tema = Ytelse.of(completedKlagebehandling.ytelseId).toTema(),
            bruker = getBruker(completedKlagebehandling.sakenGjelder),
            sak = getSak(completedKlagebehandling),
            journalfoerendeEnhet = completedKlagebehandling.klageBehandlendeEnhet,
        )
    }

    private fun handleJournalpost(
        journalpostId: String,
        avsender: PartId? = null,
        tema: Tema,
        bruker: Bruker,
        sak: Sak,
        journalfoerendeEnhet: String
    ): String {
        val journalpostInSaf = safGraphQlClient.getJournalpostAsSaksbehandler(journalpostId)
            ?: throw Exception("Journalpost with id $journalpostId not found in SAF")

        if (journalpostInSaf.journalposttype != Journalposttype.N
            && avsenderMottakerIsMissing(journalpostInSaf.avsenderMottaker)
            && journalpostCanBeUpdated(journalpostInSaf)
            && avsender == null
        ) {
            throw SectionedValidationErrorWithDetailsException(
                title = "Validation error",
                sections = listOf(
                    ValidationSection(
                        section = "saksdata",
                        properties = listOf(
                            InvalidProperty(
                                field = CreateAnkeInputView::avsender.name,
                                reason = "Velg en avsender."
                            )
                        )
                    )
                )
            )
        }

        if (journalpostInSaf.journalposttype != Journalposttype.N && avsender != null) {
            updateAvsenderInJournalpost(
                journalpostId = journalpostId,
                avsender = avsender,
            )
        }

        if (journalpostCanBeUpdated(journalpostInSaf)) {
            secureLogger.debug("Journalpost: {}", journalpostInSaf)

            updateSakInJournalpost(
                journalpostId = journalpostId,
                tema = tema,
                bruker = bruker,
                sak = sak,
                journalfoerendeEnhet = journalfoerendeEnhet,
            )

            finalizeJournalpost(
                journalpostId = journalpostId,
                journalfoerendeEnhet = journalfoerendeEnhet,
            )
            return journalpostId
        } else {
            return if (journalpostAndCompletedKlagebehandlingHaveTheSameFagsak(
                    journalpostInSaf = journalpostInSaf,
                    sak = sak,
                )
            ) {
                journalpostId
            } else {
                val newJournalpostId = createNewJournalpostBasedOnExistingJournalpost(
                    oldJournalpost = journalpostInSaf,
                    sak = sak,
                    tema = tema,
                    bruker = bruker,
                    journalfoerendeEnhet = journalfoerendeEnhet,
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
            journalfoerendeSaksbehandlerIdent = tokenUtil.getIdent()
        ).nyJournalpostId
    }

    private fun journalpostAndCompletedKlagebehandlingHaveTheSameFagsak(
        journalpostInSaf: Journalpost,
        sak: Sak,
    ): Boolean {
        return if (journalpostInSaf.sak?.fagsakId == null || journalpostInSaf.sak.fagsaksystem == null) {
            false
        } else (journalpostInSaf.sak.fagsakId == sak.fagsakid
                && FagsaksSystem.valueOf(journalpostInSaf.sak.fagsaksystem) == sak.fagsaksystem)
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

    private fun PartView.PartType.toAvsenderMottakerIdType(): AvsenderMottakerIdType {
        return when (this) {
            PartView.PartType.FNR -> AvsenderMottakerIdType.FNR
            PartView.PartType.ORGNR -> AvsenderMottakerIdType.ORGNR
        }
    }
}