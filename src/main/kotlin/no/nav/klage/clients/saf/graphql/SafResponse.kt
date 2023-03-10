package no.nav.klage.clients.saf.graphql

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime

data class DokumentoversiktBrukerResponse(val data: DokumentoversiktBrukerDataWrapper?, val errors: List<Error>?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Error(val message: String, val extensions: Extensions)

data class Extensions(val classification: String)

data class DokumentoversiktBrukerDataWrapper(val dokumentoversiktBruker: DokumentoversiktBruker)

data class DokumentoversiktBruker(val journalposter: List<Journalpost>, val sideInfo: SideInfo)

data class SideInfo(val sluttpeker: String?, val finnesNesteSide: Boolean, val antall: Int, val totaltAntall: Int)

data class JournalpostResponse(val data: JournalpostDataWrapper?, val errors: List<Error>?)

data class JournalpostDataWrapper(val journalpost: Journalpost?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Journalpost(
    val journalpostId: String,
    val tittel: String?,
    val journalposttype: Journalposttype,
    val journalstatus: Journalstatus?,
    val tema: Tema,
    val temanavn: String?,
    val behandlingstema: String?,
    val behandlingstemanavn: String?,
    val sak: Sak?,
    val avsenderMottaker: AvsenderMottaker?,
    val journalfoerendeEnhet: String?,
    val journalfortAvNavn: String?,
    val opprettetAvNavn: String?,
    val skjerming: String?,
    val datoOpprettet: LocalDateTime,
    val dokumenter: List<DokumentInfo>?,
    val relevanteDatoer: List<RelevantDato>?,
    val antallRetur: String?,
    val tilleggsopplysninger: List<Tilleggsopplysning>?,
    val kanal: Kanal,
    val kanalnavn: String,
    val utsendingsinfo: Utsendingsinfo?,
)

data class AvsenderMottaker(
    val id: String?,
    val type: AvsenderMottakerIdType?,
    val navn: String?,
    val land: String?,
    val erLikBruker: Boolean,

    ) {
    enum class AvsenderMottakerIdType {
        //Why is NULL sent as a value?
        FNR, ORGNR, HPRNR, UTL_ORG, UKJENT, NULL
    }
}

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

enum class Kanal {
    ALTINN,
    EIA,
    NAV_NO,
    NAV_NO_UINNLOGGET,
    NAV_NO_CHAT,
    SKAN_NETS,
    SKAN_PEN,
    SKAN_IM,
    INNSENDT_NAV_ANSATT,
    EESSI,
    EKST_OPPS,
    SENTRAL_UTSKRIFT,
    LOKAL_UTSKRIFT,
    SDP,
    TRYGDERETTEN,
    HELSENETTET,
    INGEN_DISTRIBUSJON,
    DPV,
    DPVS,
    UKJENT,
}

data class Tilleggsopplysning(
    val nokkel: String,
    val verdi: String,
)

data class RelevantDato(
    val dato: LocalDateTime,
    val datotype: Datotype,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DokumentInfo(
    val dokumentInfoId: String,
    val tittel: String?,
    val brevkode: String?,
    val skjerming: String?,
    val dokumentvarianter: List<Dokumentvariant>
)

data class Dokumentvariant(
    val variantformat: Variantformat,
    val filnavn: String?,
    val saksbehandlerHarTilgang: Boolean,
    val skjerming: SkjermingType?
)

enum class SkjermingType {
    POL, //Indikerer at det er fattet et vedtak etter personopplysningsloven (GDPR - brukers rett til ?? bli glemt).
    FEIL //Indikerer at det har blitt gjort en feil under mottak, journalf??ring eller brevproduksjon, slik at journalposten eller dokumentene er markert for sletting.
}

enum class Variantformat {
    //Den "offisielle" versjonen av et dokument, som er beregnet p?? visning og langtidsbevaring. I de fleste tilfeller er arkivvarianten lik dokumentet brukeren sendte inn eller mottok (digitalt eller p?? papir). Arkivvarianten er alltid i menneskelesbart format, som PDF, PDF/A eller PNG.
    //Alle dokumenter har en arkivvariant, med mindre bruker har f??tt innvilget vedtak om sletting eller skjerming av opplysninger i arkivet.
    ARKIV,

    //Dette er en sladdet variant av det opprinnelige dokumentet. Dersom det finnes en SLADDET variant, vil de fleste NAV-ansatte kun ha tilgang til denne varianten og ikke arkivvariant. Enkelte saksbehandlere vil imidlertid ha tilgang til b??de SLADDET og ARKIV.
    SLADDET,

    //Produksjonsvariant i eget propriet??rt format. Varianten finnes for dokumenter som er produsert i Metaforce eller Brevklient.
    PRODUKSJON,

    //Produksjonsvariant i eget propriet??rt format. Varianten finnes kun for dokumenter som er produsert i Exstream Live Editor.
    PRODUKSJON_DLF,

    //Variant av dokument som inneholder sp??rsm??lstekster, hjelpetekster og ubesvarte sp??rsm??l fra s??knadsdialogen. Fullversjon genereres for enkelte s??knadsskjema fra nav.no, og brukes ved klagebehandling.
    FULLVERSJON,

    //Variant av dokumentet i strukturert format, f.eks. XML eller JSON. Originalvarianten er beregnet p?? maskinell lesning og behandling.
    ORIGINAL
}

data class Sak(val datoOpprettet: LocalDateTime?, val fagsakId: String?, val fagsaksystem: String?)

enum class Tema {
    AAP, //Arbeidsavklaringspenger
    AAR, //Aa-registeret
    AGR, //Ajourhold - Grunnopplysninger
    BAR, //Barnetrygd
    BID, //Bidrag
    BIL, //Bil
    DAG, //Dagpenger
    ENF, //Enslig fors??rger
    ERS, //Erstatning
    FAR, //Farskap
    FEI, //Feilutbetaling
    FOR, //Foreldre- og svangerskapspenger
    FOS, //Forsikring
    FRI, //Kompensasjon for selvstendig n??ringsdrivende/frilansere
    FUL, //Fullmakt
    GEN, //Generell
    GRA, //Gravferdsst??nad
    GRU, //Grunn- og hjelpest??nad
    HEL, //Helsetjenester og ortopediske hjelpemidler
    HJE, //Hjelpemidler
    IAR, //Inkluderende arbeidsliv
    IND, //Tiltakspenger
    KON, //Kontantst??tte
    KTR, //Kontroll
    MED, //Medlemskap
    MOB, //Mobilitetsfremmende st??nad
    OMS, //Omsorgspenger, pleiepenger og oppl??ringspenger
    OPA, //Oppf??lging - Arbeidsgiver
    OPP, //Oppf??lging
    PEN, //Pensjon
    PER, //Permittering og masseoppsigelser
    REH, //Rehabilitering
    REK, //Rekruttering og stilling
    RPO, //Retting av personopplysninger
    RVE, //Rettferdsvederlag
    SAA, //Sanksjon - Arbeidsgiver
    SAK, //Saksomkostninger
    SAP, //Sanksjon - Person
    SER, //Serviceklager
    SIK, //Sikkerhetstiltak
    STO, //Regnskap/utbetaling
    SUP, //	Supplerende st??nad
    SYK, //Sykepenger
    SYM, //Sykmeldinger
    TIL, //Tiltak
    TRK, //Trekkh??ndtering
    TRY, //Trygdeavgift
    TSO, //Tilleggsst??nad
    TSR, //Tilleggsst??nad arbeidss??kere
    UFM, //Unntak fra medlemskap
    UFO, //Uf??retrygd
    UKJ, //Ukjent
    VEN, //Ventel??nn
    YRA, //Yrkesrettet attf??ring
    YRK //Yrkesskade / Menerstatning
}

enum class Journalstatus {
    //Journalposten er mottatt, men ikke journalf??rt. "Mottatt" er et annet ord for "arkivert" eller "midlertidig journalf??rt"
    //Statusen vil kun forekomme for inng??ende dokumenter.
    MOTTATT,

    //Journalposten er ferdigstilt og ansvaret for videre behandling av forsendelsen er overf??rt til fagsystemet. Journalen er i prinsippet l??st for videre endringer.
    //Journalposter med status JOURNALF??RT oppfyller minimumskrav til metadata i arkivet, som for eksempel tema, sak, bruker og avsender.
    JOURNALFOERT,

    //Journalposten med tilh??rende dokumenter er ferdigstilt, og journalen er i prinsippet l??st for videre endringer. FERDIGSTILT tilsvarer statusen JOURNALF??RT for inng??ende dokumenter.
    //Tilsvarer begrepet Arkivert
    //Statusen kan forekomme for utg??ende dokumenter og notater.
    FERDIGSTILT,

    //Dokumentet er sendt til bruker. Statusen benyttes ogs?? n??r dokumentet er tilgjengeliggjort for bruker p?? DittNAV, og bruker er varslet.
    //Tilsvarer begrepet Sendt
    //Statusen kan forekomme for utg??ende dokumenter.
    EKSPEDERT,

    //Journalposten er opprettet i arkivet, men fremdeles under arbeid.
    //Statusen kan forekomme for utg??ende dokumenter og notater.
    UNDER_ARBEID,

    //Journalposten har blitt arkivavgrenset etter at den feilaktig har blitt knyttet til en sak.
    //Statusen kan forekomme for alle journalposttyper.
    FEILREGISTRERT,

    //Journalposten er arkivavgrenset grunnet en feilsituasjon, ofte knyttet til skanning eller journalf??ring.
    //Statusen vil kun forekomme for inng??ende dokumenter.
    UTGAAR,

    //Utg??ende dokumenter og notater kan avbrytes mens de er under arbeid, og ikke enda er ferdigstilt. Statusen AVBRUTT brukes stort sett ved feilsituasjoner knyttet til dokumentproduksjon.
    //Statusen kan forekomme for utg??ende dokumenter og notater.
    AVBRUTT,

    //Journalposten har ikke noen kjent bruker.
    //NB: UKJENT_BRUKER er ikke en midlertidig status, men benyttes der det ikke er mulig ?? journalf??re fordi man ikke klarer ?? identifisere brukeren forsendelsen gjelder.
    //Statusen kan kun forekomme for inng??ende dokumenter.
    UKJENT_BRUKER,

    //Statusen benyttes bl.a. i forbindelse med brevproduksjon for ?? reservere 'plass' i journalen for dokumenter som skal populeres p?? et senere tidspunkt.
    //Dersom en journalpost blir st??ende i status RESEVERT over tid, tyder dette p?? at noe har g??tt feil under dokumentproduksjon eller ved skanning av et utg??ende dokument.
    //Statusen kan forekomme for utg??ende dokumenter og notater.
    RESERVERT,

    //Midlertidig status p?? vei mot MOTTATT.
    //Dersom en journalpost blir st??ende i status OPPLASTING_DOKUMENT over tid, tyder dette p?? at noe har g??tt feil under opplasting av vedlegg ved arkivering.
    //Statusen kan kun forekomme for inng??ende dokumenter.
    OPPLASTING_DOKUMENT,

    //Dersom statusfeltet i Joark er tomt, mappes dette til "UKJENT"
    UKJENT
}

enum class Journalposttype {
    I, //Inng??ende dokument: Dokumentasjon som NAV har mottatt fra en ekstern part. De fleste inng??ende dokumenter er s??knader, ettersendelser av dokumentasjon til sak, eller innsendinger fra arbeidsgivere. Meldinger brukere har sendt til "Skriv til NAV" arkiveres ogs?? som inng??ende dokumenter.
    U, //Utg??ende dokument: Dokumentasjon som NAV har produsert og sendt ut til en ekstern part. De fleste utg??ende dokumenter er informasjons- eller vedtaksbrev til privatpersoner eller organisasjoner. "Skriv til NAV"-meldinger som saksbehandlere har sendt til brukere arkiveres ogs?? som utg??ende dokumenter.
    N //Notat: Dokumentasjon som NAV har produsert selv, uten at form??let er ?? distribuere dette ut av NAV. Eksempler p?? notater er samtalereferater med veileder p?? kontaktsenter og interne forvaltningsnotater.
}

enum class Datotype {
    DATO_SENDT_PRINT,
    DATO_EKSPEDERT,
    DATO_JOURNALFOERT,
    DATO_REGISTRERT,
    DATO_AVS_RETUR,
    DATO_DOKUMENT,
    DATO_LEST,
}
