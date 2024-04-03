package no.nav.klage.clients.kabalapi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.klage.api.controller.view.CreatedAnkebehandlingStatusView
import no.nav.klage.api.controller.view.PartStatus
import no.nav.klage.api.controller.view.Utsendingskanal
import no.nav.klage.clients.dokarkiv.*
import no.nav.klage.kodeverk.Fagsystem
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreatedBehandlingResponse(
    val behandlingId: UUID,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CompletedBehandling(
    val behandlingId: UUID,
    val ytelseId: String,
    val hjemmelIdList: List<String>,
    val vedtakDate: LocalDateTime,
    val sakenGjelder: PartViewWithUtsendingskanal,
    val klager: PartViewWithUtsendingskanal,
    val fullmektig: PartViewWithUtsendingskanal?,
    val fagsakId: String,
    val fagsystem: Fagsystem,
    val fagsystemId: String,
    val klageBehandlendeEnhet: String,
    val tildeltSaksbehandlerIdent: String?,
    val tildeltSaksbehandlerNavn: String?,
) {
    fun toDokarkivSak(): Sak {
        return Sak(
            sakstype = Sakstype.FAGSAK,
            fagsaksystem = FagsaksSystem.valueOf(fagsystem.name),
            fagsakid = fagsakId
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class AnkemulighetFromKabal(
    val behandlingId: UUID,
    val typeId: String,
    val sourceOfExistingAnkebehandling: List<ExistingAnkebehandling>,
    val ytelseId: String,
    val hjemmelIdList: List<String>,
    val vedtakDate: LocalDateTime,
    val sakenGjelder: PartViewWithUtsendingskanal,
    val klager: PartViewWithUtsendingskanal,
    val fullmektig: PartViewWithUtsendingskanal?,
    val fagsakId: String,
    val fagsystem: Fagsystem,
    val fagsystemId: String,
    val klageBehandlendeEnhet: String,
    val tildeltSaksbehandlerIdent: String?,
    val tildeltSaksbehandlerNavn: String?,
)

data class ExistingAnkebehandling(
    val id: UUID,
    val created: LocalDateTime,
    val completed: LocalDateTime?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreatedAnkebehandlingStatus(
    val typeId: String,
    val ytelseId: String,
    val vedtakDate: LocalDateTime,
    val sakenGjelder: PartViewWithUtsendingskanal,
    val klager: PartViewWithUtsendingskanal,
    val fullmektig: PartViewWithUtsendingskanal?,
    val mottattNav: LocalDate,
    val frist: LocalDate,
    val fagsakId: String,
    val fagsystemId: String,
    val journalpost: DokumentReferanse,
    val tildeltSaksbehandler: TildeltSaksbehandler?,
    val svarbrev: Svarbrev?,
) {
    data class Svarbrev(
        val dokumentUnderArbeidId: UUID,
        val title: String,
        val receivers: List<Receiver>,
    ) {
        data class Receiver(
            val part: PartViewWithUtsendingskanal,
            val overriddenAddress: Address?,
            val handling: SvarbrevInput.Receiver.HandlingEnum,
        ) {
            data class Address(
                val adresselinje1: String?,
                val adresselinje2: String?,
                val adresselinje3: String?,
                val landkode: String,
                val postnummer: String?,
                val poststed: String?,
            )
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreatedKlagebehandlingStatus(
    val typeId: String,
    val ytelseId: String,
    val sakenGjelder: PartViewWithUtsendingskanal,
    val klager: PartViewWithUtsendingskanal,
    val fullmektig: PartViewWithUtsendingskanal?,
    val mottattVedtaksinstans: LocalDate,
    val mottattKlageinstans: LocalDate,
    val frist: LocalDate,
    val fagsakId: String,
    val fagsystemId: String,
    val journalpost: DokumentReferanse,
    val kildereferanse: String,
    val tildeltSaksbehandler: TildeltSaksbehandler?,
)

data class TildeltSaksbehandler(
    val navIdent: String,
    val navn: String,
)

fun TildeltSaksbehandler.toView(): no.nav.klage.api.controller.view.TildeltSaksbehandler {
    return no.nav.klage.api.controller.view.TildeltSaksbehandler(
        navIdent = navIdent,
        navn = navn
    )
}

fun CreatedAnkebehandlingStatus.Svarbrev.toView(): CreatedAnkebehandlingStatusView.Svarbrev {
    return CreatedAnkebehandlingStatusView.Svarbrev(
        dokumentUnderArbeidId = dokumentUnderArbeidId,
        title = title,
        receivers = receivers.map { receiver ->
            CreatedAnkebehandlingStatusView.Svarbrev.Receiver(
                part = receiver.part.partViewWithUtsendingskanal(),
                overriddenAddress = receiver.overriddenAddress?.let {
                    CreatedAnkebehandlingStatusView.Svarbrev.Receiver.Address(
                        adresselinje1 = it.adresselinje1,
                        adresselinje2 = it.adresselinje2,
                        adresselinje3 = it.adresselinje3,
                        landkode = it.landkode,
                        postnummer = it.postnummer,
                        poststed = it.poststed,
                    )
                },
                handling = no.nav.klage.api.controller.view.SvarbrevInput.Receiver.HandlingEnum.valueOf(receiver.handling.name),
            )
        }
    )
}

data class OversendtPartId(
    val type: OversendtPartIdType,
    val value: String
)

enum class OversendtPartIdType { PERSON, VIRKSOMHET }

@JsonIgnoreProperties(ignoreUnknown = true)
data class PartView(
    val id: String,
    val type: PartType,
    val name: String,
    val available: Boolean,
    val statusList: List<PartStatus>,
    val address: Address?,
    val language: String?,
) {
    fun partView(): no.nav.klage.api.controller.view.PartView {
        return no.nav.klage.api.controller.view.PartView(
            id = id,
            type = no.nav.klage.api.controller.view.PartType.valueOf(type.name),
            name = name,
            available = available,
            statusList = statusList.map { partStatus ->
                PartStatus(
                    status = no.nav.klage.api.controller.view.PartStatus.Status.valueOf(partStatus.status.name),
                    date = partStatus.date,
                )
            },
            address = address?.let {
                no.nav.klage.api.controller.view.Address(
                    adresselinje1 = it.adresselinje1,
                    adresselinje2 = it.adresselinje2,
                    adresselinje3 = it.adresselinje3,
                    landkode = it.landkode,
                    postnummer = it.postnummer,
                    poststed = it.poststed,
                )
            },
            language = language,
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PartViewWithUtsendingskanal(
    val id: String,
    val type: PartType,
    val name: String,
    val available: Boolean,
    val statusList: List<PartStatus>,
    val address: Address?,
    val utsendingskanal: Utsendingskanal,
    val language: String?,
) {

    fun partViewWithUtsendingskanal(): no.nav.klage.api.controller.view.PartViewWithUtsendingskanal {
        return no.nav.klage.api.controller.view.PartViewWithUtsendingskanal(
            id = id,
            type = no.nav.klage.api.controller.view.PartType.valueOf(type.name),
            name = name,
            available = available,
            statusList = statusList.map { partStatus ->
                PartStatus(
                    status = no.nav.klage.api.controller.view.PartStatus.Status.valueOf(partStatus.status.name),
                    date = partStatus.date,
                )
            },
            address = address?.let {
                no.nav.klage.api.controller.view.Address(
                    adresselinje1 = it.adresselinje1,
                    adresselinje2 = it.adresselinje2,
                    adresselinje3 = it.adresselinje3,
                    landkode = it.landkode,
                    postnummer = it.postnummer,
                    poststed = it.poststed,
                )
            },
            language = language,
            utsendingskanal = utsendingskanal,
        )
    }

    fun toDokarkivBruker(): Bruker {
        return Bruker(
            id = id,
            idType = BrukerIdType.valueOf(type.name)
        )
    }
}

enum class PartType {
    FNR, ORGNR
}

data class PartStatus(
    val status: Status,
    val date: LocalDate? = null,
) {
    enum class Status {
        DEAD,
        DELETED,
        FORTROLIG,
        STRENGT_FORTROLIG,
        EGEN_ANSATT,
        VERGEMAAL,
        FULLMAKT,
        RESERVERT_I_KRR,
        DELT_ANSVAR,
    }
}

data class Address(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val landkode: String,
    val postnummer: String?,
    val poststed: String?,
)

data class DokumentReferanse(
    val journalpostId: String,
    val dokumentInfoId: String,
    val tittel: String?,
    val tema: String,
    val temaId: String,
    val harTilgangTilArkivvariant: Boolean,
    val vedlegg: MutableList<VedleggReferanse> = mutableListOf(),
    val journalposttype: Journalposttype?,
    val journalstatus: Journalstatus?,
    val behandlingstema: String?,
    val behandlingstemanavn: String?,
    val sak: Sak?,
    val avsenderMottaker: AvsenderMottaker?,
    val journalfoerendeEnhet: String?,
    val journalfortAvNavn: String?,
    val opprettetAvNavn: String?,
    val datoOpprettet: LocalDateTime,
    val relevanteDatoer: List<RelevantDato>?,
    val antallRetur: Int?,
    val tilleggsopplysninger: List<Tilleggsopplysning>?,
    val kanal: String,
    val kanalnavn: String,
    val utsendingsinfo: Utsendingsinfo?,
    var alreadyUsed: Boolean = false,
) {

    enum class Journalstatus {
        //Journalposten er mottatt, men ikke journalført. "Mottatt" er et annet ord for "arkivert" eller "midlertidig journalført"
        //Statusen vil kun forekomme for inngående dokumenter.
        MOTTATT,

        //Journalposten er ferdigstilt og ansvaret for videre behandling av forsendelsen er overført til fagsystemet. Journalen er i prinsippet låst for videre endringer.
        //Journalposter med status JOURNALFØRT oppfyller minimumskrav til metadata i arkivet, som for eksempel tema, sak, bruker og avsender.
        JOURNALFOERT,

        //Journalposten med tilhørende dokumenter er ferdigstilt, og journalen er i prinsippet låst for videre endringer. FERDIGSTILT tilsvarer statusen JOURNALFØRT for inngående dokumenter.
        //Tilsvarer begrepet Arkivert
        //Statusen kan forekomme for utgående dokumenter og notater.
        FERDIGSTILT,

        //Dokumentet er sendt til bruker. Statusen benyttes også når dokumentet er tilgjengeliggjort for bruker på DittNAV, og bruker er varslet.
        //Tilsvarer begrepet Sendt
        //Statusen kan forekomme for utgående dokumenter.
        EKSPEDERT,

        //Journalposten er opprettet i arkivet, men fremdeles under arbeid.
        //Statusen kan forekomme for utgående dokumenter og notater.
        UNDER_ARBEID,

        //Journalposten har blitt arkivavgrenset etter at den feilaktig har blitt knyttet til en sak.
        //Statusen kan forekomme for alle journalposttyper.
        FEILREGISTRERT,

        //Journalposten er arkivavgrenset grunnet en feilsituasjon, ofte knyttet til skanning eller journalføring.
        //Statusen vil kun forekomme for inngående dokumenter.
        UTGAAR,

        //Utgående dokumenter og notater kan avbrytes mens de er under arbeid, og ikke enda er ferdigstilt. Statusen AVBRUTT brukes stort sett ved feilsituasjoner knyttet til dokumentproduksjon.
        //Statusen kan forekomme for utgående dokumenter og notater.
        AVBRUTT,

        //Journalposten har ikke noen kjent bruker.
        //NB: UKJENT_BRUKER er ikke en midlertidig status, men benyttes der det ikke er mulig å journalføre fordi man ikke klarer å identifisere brukeren forsendelsen gjelder.
        //Statusen kan kun forekomme for inngående dokumenter.
        UKJENT_BRUKER,

        //Statusen benyttes bl.a. i forbindelse med brevproduksjon for å reservere 'plass' i journalen for dokumenter som skal populeres på et senere tidspunkt.
        //Dersom en journalpost blir stående i status RESEVERT over tid, tyder dette på at noe har gått feil under dokumentproduksjon eller ved skanning av et utgående dokument.
        //Statusen kan forekomme for utgående dokumenter og notater.
        RESERVERT,

        //Midlertidig status på vei mot MOTTATT.
        //Dersom en journalpost blir stående i status OPPLASTING_DOKUMENT over tid, tyder dette på at noe har gått feil under opplasting av vedlegg ved arkivering.
        //Statusen kan kun forekomme for inngående dokumenter.
        OPPLASTING_DOKUMENT,

        //Dersom statusfeltet i Joark er tomt, mappes dette til "UKJENT"
        UKJENT
    }

    data class AvsenderMottaker(
        val id: String,
        val type: AvsenderMottakerIdType,
        val navn: String?,
    ) {
        enum class AvsenderMottakerIdType {
            //TODO look into NULL
            FNR, ORGNR, HPRNR, UTL_ORG, UKJENT, NULL
        }
    }

    data class VedleggReferanse(
        val dokumentInfoId: String,
        val tittel: String?,
        val harTilgangTilArkivvariant: Boolean,
    )

    enum class Journalposttype {
        I, //Inngående dokument: Dokumentasjon som NAV har mottatt fra en ekstern part. De fleste inngående dokumenter er søknader, ettersendelser av dokumentasjon til sak, eller innsendinger fra arbeidsgivere. Meldinger brukere har sendt til "Skriv til NAV" arkiveres også som inngående dokumenter.
        U, //Utgående dokument: Dokumentasjon som NAV har produsert og sendt ut til en ekstern part. De fleste utgående dokumenter er informasjons- eller vedtaksbrev til privatpersoner eller organisasjoner. "Skriv til NAV"-meldinger som saksbehandlere har sendt til brukere arkiveres også som utgående dokumenter.
        N //Notat: Dokumentasjon som NAV har produsert selv, uten at formålet er å distribuere dette ut av NAV. Eksempler på notater er samtalereferater med veileder på kontaktsenter og interne forvaltningsnotater.
    }

    data class Sak(
        val datoOpprettet: LocalDateTime?,
        val fagsakId: String?,
        val fagsaksystem: String?,
        val fagsystemId: String?
    )

    data class RelevantDato(
        val dato: LocalDateTime,
        val datotype: Datotype,
    ) {
        enum class Datotype {
            DATO_SENDT_PRINT,
            DATO_EKSPEDERT,
            DATO_JOURNALFOERT,
            DATO_REGISTRERT,
            DATO_AVS_RETUR,
            DATO_DOKUMENT,
            DATO_LEST,
        }
    }

    data class Tilleggsopplysning(
        val key: String,
        val value: String,
    )

    data class Utsendingsinfo(
        val epostVarselSendt: EpostVarselSendt?,
        val smsVarselSendt: SmsVarselSendt?,
        val fysiskpostSendt: FysiskpostSendt?,
        val digitalpostSendt: DigitalpostSendt?,
    ) {
        data class EpostVarselSendt(
            val tittel: String,
            val adresse: String,
            val varslingstekst: String,
        )

        data class SmsVarselSendt(
            val adresse: String,
            val varslingstekst: String,
        )

        data class FysiskpostSendt(
            val adressetekstKonvolutt: String,
        )

        data class DigitalpostSendt(
            val adresse: String,
        )
    }

}