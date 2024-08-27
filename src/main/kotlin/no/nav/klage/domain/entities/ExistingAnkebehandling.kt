package no.nav.klage.domain.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "mulighet_existing_ankebehandling", schema = "klage")
class ExistingAnkebehandling(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "ankebehandling_id")
    val ankebehandlingId: UUID,
    @Column(name = "created")
    val created: LocalDateTime,
    @Column(name = "completed")
    val completed: LocalDateTime?,
)