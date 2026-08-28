package no.nav.klage.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import no.nav.klage.domain.entities.Registrering
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

interface RegistreringRepository : JpaRepository<Registrering, UUID> {
    @Query(
        """
            SELECT r FROM Registrering r 
            WHERE r.createdBy = :navIdent
             AND r.finished is null
        """,
    )
    fun findUferdigeRegistreringer(navIdent: String): List<Registrering>

    @Query(
        """
            SELECT r FROM Registrering r 
            WHERE r.createdBy = :navIdent
            AND r.finished is not null
            AND r.finished >= :finishedFrom
        """,
    )
    fun findFerdigeRegistreringer(
        navIdent: String,
        finishedFrom: LocalDateTime,
    ): List<Registrering>

    fun findByBehandlingId(behandlingId: UUID): Registrering

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(
        QueryHint(name = "jakarta.persistence.lock.timeout", value = "20000"),
    )
    override fun findById(id: UUID): Optional<Registrering>

    /**
     * Plain read without the pessimistic lock that [findById] takes. Needed for read-only
     * transactions, where Postgres rejects `SELECT ... FOR NO KEY UPDATE`.
     */
    @Lock(LockModeType.NONE)
    @Query(
        """
            SELECT r FROM Registrering r 
            WHERE r.id = :id
        """,
    )
    fun findByIdWithoutLock(id: UUID): Registrering?
}
