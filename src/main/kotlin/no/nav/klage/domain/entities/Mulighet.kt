package no.nav.klage.domain.entities

import jakarta.persistence.*
import no.nav.klage.kodeverk.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Cache of muligheter for a given registrering
 */
@AttributeOverrides(
    AttributeOverride(name = "sakenGjelder.part.type", column= Column(name="saken_gjelder_type")),
    AttributeOverride(name = "sakenGjelder.part.value", column= Column(name="saken_gjelder_value")),
    AttributeOverride(name = "sakenGjelder.address.adresselinje1", column = Column(name = "saken_gjelder_adresselinje1")),
    AttributeOverride(name = "sakenGjelder.address.adresselinje2", column = Column(name = "saken_gjelder_adresselinje2")),
    AttributeOverride(name = "sakenGjelder.address.adresselinje3", column = Column(name = "saken_gjelder_adresselinje3")),
    AttributeOverride(name = "sakenGjelder.address.postnummer", column = Column(name = "saken_gjelder_postnummer")),
    AttributeOverride(name = "sakenGjelder.address.poststed", column = Column(name = "saken_gjelder_poststed")),
    AttributeOverride(name = "sakenGjelder.address.landkode", column = Column(name = "saken_gjelder_landkode")),
    AttributeOverride(name = "sakenGjelder.name", column = Column(name = "saken_gjelder_name")),
    AttributeOverride(name = "sakenGjelder.available", column = Column(name = "saken_gjelder_available")),
    AttributeOverride(name = "sakenGjelder.language", column = Column(name = "saken_gjelder_language")),
    AttributeOverride(name = "sakenGjelder.utsendingskanal", column = Column(name = "saken_gjelder_utsendingskanal")),

    AttributeOverride(name = "klager.part.type", column= Column(name="klager_type")),
    AttributeOverride(name = "klager.part.value", column= Column(name="klager_value")),
    AttributeOverride(name = "klager.address.adresselinje1", column = Column(name = "klager_adresselinje1")),
    AttributeOverride(name = "klager.address.adresselinje2", column = Column(name = "klager_adresselinje2")),
    AttributeOverride(name = "klager.address.adresselinje3", column = Column(name = "klager_adresselinje3")),
    AttributeOverride(name = "klager.address.postnummer", column = Column(name = "klager_postnummer")),
    AttributeOverride(name = "klager.address.poststed", column = Column(name = "klager_poststed")),
    AttributeOverride(name = "klager.address.landkode", column = Column(name = "klager_landkode")),
    AttributeOverride(name = "klager.name", column = Column(name = "klager_name")),
    AttributeOverride(name = "klager.available", column = Column(name = "klager_available")),
    AttributeOverride(name = "klager.language", column = Column(name = "klager_language")),
    AttributeOverride(name = "klager.utsendingskanal", column = Column(name = "klager_utsendingskanal")),

    AttributeOverride(name = "fullmektig.part.type", column= Column(name="fullmektig_type")),
    AttributeOverride(name = "fullmektig.part.value", column= Column(name="fullmektig_value")),
    AttributeOverride(name = "fullmektig.address.adresselinje1", column = Column(name = "fullmektig_adresselinje1")),
    AttributeOverride(name = "fullmektig.address.adresselinje2", column = Column(name = "fullmektig_adresselinje2")),
    AttributeOverride(name = "fullmektig.address.adresselinje3", column = Column(name = "fullmektig_adresselinje3")),
    AttributeOverride(name = "fullmektig.address.postnummer", column = Column(name = "fullmektig_postnummer")),
    AttributeOverride(name = "fullmektig.address.poststed", column = Column(name = "fullmektig_poststed")),
    AttributeOverride(name = "fullmektig.address.landkode", column = Column(name = "fullmektig_landkode")),
    AttributeOverride(name = "fullmektig.name", column = Column(name = "fullmektig_name")),
    AttributeOverride(name = "fullmektig.available", column = Column(name = "fullmektig_available")),
    AttributeOverride(name = "fullmektig.language", column = Column(name = "fullmektig_language")),
    AttributeOverride(name = "fullmektig.utsendingskanal", column = Column(name = "fullmektig_utsendingskanal")),
)
@Entity
@Table(name = "registrering_mulighet", schema = "klage")
class Mulighet(
    @Id
    val id: UUID = UUID.randomUUID(),
    //TODO: Should complete parts be stored in db, or fetched from external service?
    @Embedded
    val sakenGjelder: PartWithUtsendingskanal,
    @Embedded
    val klager: PartWithUtsendingskanal?,
    @Embedded
    val fullmektig: PartWithUtsendingskanal?,
    @Convert(converter = FagsystemConverter::class)
    @Column(name = "current_fagsystem_id")
    val currentFagsystem: Fagsystem,
    @Convert(converter = FagsystemConverter::class)
    @Column(name = "original_fagsystem_id")
    val originalFagsystem: Fagsystem,
    @Column(name = "fagsak_id")
    val fagsakId: String,
    @Convert(converter = TemaConverter::class)
    @Column(name = "tema_id")
    val tema: Tema,
    @Column(name = "vedtak_date")
    val vedtakDate: LocalDate?,
    @Convert(converter = YtelseConverter::class)
    @Column(name = "ytelse_id")
    val ytelse: Ytelse?,
    @Convert(converter = StringListConverter::class)
    @Column(name = "hjemmel_id_list")
    var hjemmelIdList: List<String>,
    @Column(name = "previous_saksbehandler_ident")
    val previousSaksbehandlerIdent: String?,
    @Column(name = "previous_saksbehandler_name")
    val previousSaksbehandlerName: String?,
    @Convert(converter = TypeConverter::class)
    @Column(name = "type_id")
    val type: Type,
    @Column(name = "klage_behandlende_enhet")
    val klageBehandlendeEnhet: String,
    /** sakId from Infotrygd or behandlingId from Kabal */
    @Column(name = "current_fagystem_technical_id")
    val currentFagystemTechnicalId: String,

    @Column(name = "created")
    val created: LocalDateTime = LocalDateTime.now(),

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "registrering_mulighet_id", referencedColumnName = "id", nullable = false)
    val sourceOfExistingAnkebehandling: MutableSet<ExistingAnkebehandling> = mutableSetOf(),

    //TODO: Maybe move these to embedded class PartStatus
    @ElementCollection
    @CollectionTable(name = "mulighet_saken_gjelder_part_status", schema = "klage", joinColumns = [JoinColumn(name = "registrering_mulighet_id", referencedColumnName = "id", nullable = false)] )
    val sakenGjelderStatusList: MutableSet<PartStatus> = mutableSetOf(),

    @ElementCollection
    @CollectionTable(name = "mulighet_klager_part_status", schema = "klage", joinColumns = [JoinColumn(name = "registrering_mulighet_id", referencedColumnName = "id", nullable = false)] )
    val klagerStatusList: MutableSet<PartStatus> = mutableSetOf(),

    @ElementCollection
    @CollectionTable(name = "mulighet_fullmektig_part_status", schema = "klage", joinColumns = [JoinColumn(name = "registrering_mulighet_id", referencedColumnName = "id", nullable = false)] )
    val fullmektigStatusList: MutableSet<PartStatus> = mutableSetOf(),

    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Mulighet

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Mulighet(id=$id, sakenGjelder=$sakenGjelder, klager=$klager, fullmektig=$fullmektig, currentFagsystem=$currentFagsystem, originalFagsystem=$originalFagsystem, fagsakId='$fagsakId', tema=$tema, vedtakDate=$vedtakDate, ytelse=$ytelse, hjemmelIdList=$hjemmelIdList, previousSaksbehandlerIdent=$previousSaksbehandlerIdent, previousSaksbehandlerName=$previousSaksbehandlerName, type=$type, klageBehandlendeEnhet=$klageBehandlendeEnhet, currentFagystemTechnicalId=$currentFagystemTechnicalId, created=$created, sourceOfExistingAnkebehandling=$sourceOfExistingAnkebehandling, sakenGjelderStatusList=$sakenGjelderStatusList, klagerStatusList=$klagerStatusList, fullmektigStatusList=$fullmektigStatusList)"
    }

}