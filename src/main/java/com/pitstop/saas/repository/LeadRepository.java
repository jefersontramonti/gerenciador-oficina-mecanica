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
     */
    @Query("""
        SELECT l FROM Lead l
        WHERE (:status IS NULL OR l.status = :status)
        AND (:origem IS NULL OR l.origem = :origem)
        AND (:nome IS NULL OR LOWER(l.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
        AND (:email IS NULL OR LOWER(l.email) LIKE LOWER(CONCAT('%', :email, '%')))
        ORDER BY l.createdAt DESC
    """)
    Page<Lead> findWithFilters(
        @Param("status") StatusLead status,
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
