package no.nav.klage.domain.entities

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "svarbrev_receiver", schema = "klage")
class SvarbrevReceiver(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Embedded
    @AttributeOverrides(
        value = [
            AttributeOverride(name = "type", column = Column(name = "part_type")),
            AttributeOverride(name = "value", column = Column(name = "part_value"))
        ]
    )
    val part: PartId,
    //TODO: should be kodeverk?
    @Column(name = "handling")
    @Enumerated(EnumType.STRING)
    var handling: HandlingEnum?,
    @Embedded
    var overriddenAddress: Address?
) {
    override fun toString(): String {
        return "SvarbrevReceiver(id=$id, part=$part, handling=$handling, overriddenAddress=$overriddenAddress)"
    }
}

enum class HandlingEnum {
    AUTO,
    LOCAL_PRINT,
    CENTRAL_PRINT
}