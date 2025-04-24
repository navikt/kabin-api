package no.nav.klage.domain.entities

import jakarta.persistence.*
import no.nav.klage.kodeverk.*
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.kodeverk.ytelse.YtelseConverter
import org.hibernate.annotations.DynamicUpdate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "registrering", schema = "klage")
@DynamicUpdate
class Registrering(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Embedded
    @AttributeOverrides(
        value = [
            AttributeOverride(name = "type", column = Column(name = "saken_gjelder_type")),
            AttributeOverride(name = "value", column = Column(name = "saken_gjelder_value"))
        ]
    )
    var sakenGjelder: PartId?,
    @Embedded
    @AttributeOverrides(
        value = [
            AttributeOverride(name = "type", column = Column(name = "klager_type")),
            AttributeOverride(name = "value", column = Column(name = "klager_value"))
        ]
    )
    var klager: PartId?,
    @Embedded
    @AttributeOverrides(
        value = [
            AttributeOverride(name = "type", column = Column(name = "fullmektig_type")),
            AttributeOverride(name = "value", column = Column(name = "fullmektig_value"))
        ]
    )
    var fullmektig: PartId?,
    @Embedded
    @AttributeOverrides(
        value = [
            AttributeOverride(name = "type", column = Column(name = "avsender_type")),
            AttributeOverride(name = "value", column = Column(name = "avsender_value"))
        ]
    )
    var avsender: PartId?,
    @Column(name = "journalpost_id")
    var journalpostId: String?,
    @Column(name = "journalpost_dato_opprettet")
    var journalpostDatoOpprettet: LocalDate?,
    @Convert(converter = TypeConverter::class)
    @Column(name = "type_id")
    var type: Type?,
    @Column(name = "mulighet_is_based_on_journalpost")
    var mulighetIsBasedOnJournalpost: Boolean,
    @Column(name = "mulighet_id")
    var mulighetId: UUID?,
    @Column(name = "mottatt_vedtaksinstans")
    var mottattVedtaksinstans: LocalDate?,
    @Column(name = "mottatt_klageinstans")
    var mottattKlageinstans: LocalDate?,
    @Column(name = "behandlingstid_units")
    var behandlingstidUnits: Int,
    @Column(name = "behandlingstid_unit_type_id")
    @Convert(converter = TimeUnitTypeConverter::class)
    var behandlingstidUnitType: TimeUnitType,
    @Convert(converter = StringListConverter::class)
    @Column(name = "hjemmel_id_list")
    var hjemmelIdList: List<String>,
    @Convert(converter = YtelseConverter::class)
    @Column(name = "ytelse_id")
    var ytelse: Ytelse?,
    @Column(name = "saksbehandler_ident")
    var saksbehandlerIdent: String?,
    //TODO: Rename databasekolonne
    @Column(name = "oppgave_id")
    var gosysOppgaveId: Long?,
    @Column(name = "send_svarbrev")
    var sendSvarbrev: Boolean?,
    @Column(name = "svarbrev_title")
    var svarbrevTitle: String,
    @Column(name = "override_svarbrev_custom_text")
    var overrideSvarbrevCustomText: Boolean,
    @Column(name = "svarbrev_custom_text")
    var svarbrevCustomText: String?,
    @Column(name = "svarbrev_initial_custom_text")
    var svarbrevInitialCustomText: String?,
    @Column(name = "override_svarbrev_behandlingstid")
    var overrideSvarbrevBehandlingstid: Boolean,
    @Column(name = "svarbrev_behandlingstid_units")
    var svarbrevBehandlingstidUnits: Int?,
    @Column(name = "svarbrev_behandlingstid_unit_type_id")
    @Convert(converter = TimeUnitTypeConverter::class)
    var svarbrevBehandlingstidUnitType: TimeUnitType?,
    @Column(name = "svarbrev_fullmektig_fritekst")
    var svarbrevFullmektigFritekst: String?,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "registrering_id", referencedColumnName = "id", nullable = false)
    val svarbrevReceivers: MutableSet<SvarbrevReceiver> = mutableSetOf(),
    @Column(name = "created")
    val created: LocalDateTime = LocalDateTime.now(),
    @Column(name = "modified")
    var modified: LocalDateTime = LocalDateTime.now(),
    @Column(name = "created_by")
    val createdBy: String,
    @Column(name = "finished")
    var finished: LocalDateTime?,
    @Column(name = "behandling_id")
    var behandlingId: UUID?,
    @Column(name = "will_create_new_journalpost")
    var willCreateNewJournalpost: Boolean,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "registrering_id", referencedColumnName = "id", nullable = false)
    val muligheter: MutableSet<Mulighet> = mutableSetOf(),
    @Column(name = "muligheter_fetched")
    var muligheterFetched: LocalDateTime?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Registrering

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Registrering(id=$id, sakenGjelder=$sakenGjelder, klager=$klager, fullmektig=$fullmektig, avsender=$avsender, journalpostId=$journalpostId, journalpostDatoOpprettet=$journalpostDatoOpprettet, type=$type, mulighetId=$mulighetId, mottattVedtaksinstans=$mottattVedtaksinstans, mottattKlageinstans=$mottattKlageinstans, behandlingstidUnits=$behandlingstidUnits, behandlingstidUnitType=$behandlingstidUnitType, hjemmelIdList=$hjemmelIdList, ytelse=$ytelse, saksbehandlerIdent=$saksbehandlerIdent, oppgaveId=$gosysOppgaveId, sendSvarbrev=$sendSvarbrev, svarbrevTitle='$svarbrevTitle', overrideSvarbrevCustomText=$overrideSvarbrevCustomText, svarbrevCustomText=$svarbrevCustomText, overrideSvarbrevBehandlingstid=$overrideSvarbrevBehandlingstid, svarbrevBehandlingstidUnits=$svarbrevBehandlingstidUnits, svarbrevBehandlingstidUnitType=$svarbrevBehandlingstidUnitType, svarbrevFullmektigFritekst=$svarbrevFullmektigFritekst, svarbrevReceivers=$svarbrevReceivers, created=$created, modified=$modified, createdBy='$createdBy', finished=$finished, behandlingId=$behandlingId, willCreateNewJournalpost=$willCreateNewJournalpost, muligheter=$muligheter, muligheterFetched=$muligheterFetched)"
    }

    fun handleSvarbrevReceivers() {
        val existingReceivers = svarbrevReceivers
        val existingParts = setOf(
            sakenGjelder?.value,
            klager?.value,
            fullmektig?.value
        ).filterNotNull()

        if (existingParts.size == 1 && existingReceivers.size == 0) {
            svarbrevReceivers.add(
                SvarbrevReceiver(
                    part = PartId(
                        value = sakenGjelder!!.value,
                        type = PartIdType.PERSON
                    ),
                    overriddenAddress = null,
                    handling = HandlingEnum.AUTO,
                )
            )
        }
    }
}

@Converter
class StringListConverter : AttributeConverter<List<String>, String> {
    override fun convertToDatabaseColumn(attribute: List<String>?): String? {
        return if (attribute.isNullOrEmpty()) {
            null
        } else {
            attribute.joinToString(",")
        }
    }

    override fun convertToEntityAttribute(dbData: String?): List<String> {
        return dbData?.split(",") ?: emptyList()
    }
}