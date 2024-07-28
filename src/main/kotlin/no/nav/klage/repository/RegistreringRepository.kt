package no.nav.klage.repository

import no.nav.klage.domain.entities.Registrering
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface RegistreringRepository : JpaRepository<Registrering, UUID> {
}