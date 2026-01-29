package no.nav.klage.domain.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "mulighet_existing_behandling", schema = "klage")
class ExistingBehandling(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "type_id")
    val typeId: String,
    @Column(name = "behandling_id")
    val behandlingId: UUID,
    @Column(name = "created")
    val created: LocalDateTime,
    @Column(name = "completed")
    val completed: LocalDateTime?,
)