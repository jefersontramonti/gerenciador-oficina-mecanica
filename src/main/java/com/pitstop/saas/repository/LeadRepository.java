package com.pitstop.saas.repository;

import com.pitstop.saas.domain.Lead;
import com.pitstop.saas.domain.StatusLead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Lead entity.
 *
 * @author PitStop Team
 */
@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    /**
     * Find all leads by status with pagination.
     */
    Page<Lead> findByStatus(StatusLead status, Pageable pageable);

    /**
     * Find all leads created between two dates.
     */
    Page<Lead> findByCreatedAtBetween(LocalDateTime inicio, LocalDateTime fim, Pageable pageable);

    /**
     * Count leads by status.
     */
    long countByStatus(StatusLead status);

    /**
     * Check if email already exists.
     */
    boolean existsByEmail(String email);

    /**
     * Find leads with filters.
     * Usa native query para evitar problema com lower(bytea).
     */
    @Query(value = """
        SELECT * FROM leads l
        WHERE (:status IS NULL OR l.status = CAST(:status AS VARCHAR))
          AND (:origem IS NULL OR l.origem = CAST(:origem AS VARCHAR))
          AND (:nome IS NULL OR LOWER(CAST(l.nome AS VARCHAR)) LIKE LOWER(CONCAT('%', CAST(:nome AS VARCHAR), '%')))
          AND (:email IS NULL OR LOWER(CAST(l.email AS VARCHAR)) LIKE LOWER(CONCAT('%', CAST(:email AS VARCHAR), '%')))
        ORDER BY l.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM leads l
        WHERE (:status IS NULL OR l.status = CAST(:status AS VARCHAR))
          AND (:origem IS NULL OR l.origem = CAST(:origem AS VARCHAR))
          AND (:nome IS NULL OR LOWER(CAST(l.nome AS VARCHAR)) LIKE LOWER(CONCAT('%', CAST(:nome AS VARCHAR), '%')))
          AND (:email IS NULL OR LOWER(CAST(l.email AS VARCHAR)) LIKE LOWER(CONCAT('%', CAST(:email AS VARCHAR), '%')))
        """,
        nativeQuery = true)
    Page<Lead> findWithFilters(
        @Param("status") String status,
        @Param("origem") String origem,
        @Param("nome") String nome,
        @Param("email") String email,
        Pageable pageable
    );

    /**
     * Get leads statistics grouped by status.
     */
    @Query("""
        SELECT l.status as status, COUNT(l) as total
        FROM Lead l
        GROUP BY l.status
    """)
    List<Object[]> countByStatusGrouped();
}
