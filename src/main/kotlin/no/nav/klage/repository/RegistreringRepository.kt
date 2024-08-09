package no.nav.klage.repository

import no.nav.klage.domain.entities.Registrering
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.util.*

interface RegistreringRepository : JpaRepository<Registrering, UUID> {

    @Query(
        """
            SELECT r FROM Registrering r 
            WHERE r.createdBy = :navIdent
             AND r.finished is null
        """
    )
    fun findUferdigeRegistreringer(navIdent: String): List<Registrering>

    @Query(
        """
            SELECT r FROM Registrering r 
            WHERE r.createdBy = :navIdent
            AND r.finished is not null
            AND r.finished >= :finishedFrom
        """
    )
    fun findFerdigeRegistreringer(navIdent: String, finishedFrom: LocalDateTime): List<Registrering>

}