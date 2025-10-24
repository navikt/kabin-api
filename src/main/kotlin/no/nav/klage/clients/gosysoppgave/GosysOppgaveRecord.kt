package no.nav.klage.clients.gosysoppgave

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.LocalDate
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class GosysOppgaveRecord(
    val id: Long,
    val versjon: Int,
    val journalpostId: String?,
    val saksreferanse: String?,
    val mappeId: Long?,
    val status: Status?,
    val tildeltEnhetsnr: String?,
    val opprettetAvEnhetsnr: String?,
    val endretAvEnhetsnr: String?,
    val tema: String,
    val temagruppe: String?,
    val behandlingstema: String?,
    val oppgavetype: String?,
    val behandlingstype: String?,
    val prioritet: Prioritet?,
    val tilordnetRessurs: String?,
    val beskrivelse: String?,
    val fristFerdigstillelse: LocalDate?,
    val aktivDato: String?,
    val opprettetAv: String?,
    val endretAv: String?,
    @JsonDeserialize(using = OffsetDateTimeToLocalDateTimeDeserializer::class)
    val opprettetTidspunkt: LocalDateTime,
    @JsonDeserialize(using = OffsetDateTimeToLocalDateTimeDeserializer::class)
    val endretTidspunkt: LocalDateTime?,
    @JsonDeserialize(using = OffsetDateTimeToLocalDateTimeDeserializer::class)
    val ferdigstiltTidspunkt: LocalDateTime?,
    val behandlesAvApplikasjon: String?,
    val journalpostkilde: String?,
    val metadata: Map<String, String>?,
    val bnr: String?,
    val samhandlernr: String?,
    val aktoerId: String?,
    val orgnr: String?,
)

enum class Status(val statusId: Long) {

    OPPRETTET(1),
    AAPNET(2),
    UNDER_BEHANDLING(3),
    FERDIGSTILT(4),
    FEILREGISTRERT(5);

    companion object {

        fun of(statusId: Long): Status {
            return entries.firstOrNull { it.statusId == statusId }
                ?: throw IllegalArgumentException("No status with $statusId exists")
        }
    }
}

enum class Prioritet {
    HOY,
    NORM,
    LAV
}

enum class Statuskategori {
    AAPEN,
    AVSLUTTET;
}

data class GosysOppgaveResponse(
    val antallTreffTotalt: Int,
    val oppgaver: List<GosysOppgaveRecord>
)