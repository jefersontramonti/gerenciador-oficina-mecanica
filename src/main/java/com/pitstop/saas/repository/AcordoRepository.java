package com.pitstop.saas.repository;

import com.pitstop.saas.domain.Acordo;
import com.pitstop.saas.domain.StatusAcordo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for payment agreements (Acordo).
 */
@Repository
public interface AcordoRepository extends JpaRepository<Acordo, UUID> {

    // =====================================
    // FIND BY SINGLE FIELD
    // =====================================

    Optional<Acordo> findByNumero(String numero);

    Page<Acordo> findByOficinaId(UUID oficinaId, Pageable pageable);

    Page<Acordo> findByStatus(StatusAcordo status, Pageable pageable);

    List<Acordo> findByOficinaIdOrderByDataAcordoDesc(UUID oficinaId);

    // =====================================
    // FIND WITH FILTERS
    // =====================================

    @Query("""
        SELECT a FROM Acordo a
        WHERE (:oficinaId IS NULL OR a.oficina.id = :oficinaId)
        AND (:status IS NULL OR a.status = :status)
        ORDER BY a.dataAcordo DESC
        """)
    Page<Acordo> findWithFilters(
        @Param("oficinaId") UUID oficinaId,
        @Param("status") StatusAcordo status,
        Pageable pageable
    );

    // =====================================
    // EXISTENCE CHECKS
    // =====================================

    /**
     * Check if workshop has an active agreement.
     */
    boolean existsByOficinaIdAndStatus(UUID oficinaId, StatusAcordo status);

    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
        FROM Acordo a
        WHERE a.oficina.id = :oficinaId
        AND a.status = 'ATIVO'
        """)
    boolean hasActiveAgreement(@Param("oficinaId") UUID oficinaId);

    // =====================================
    // ACTIVE AGREEMENTS
    // =====================================

    /**
     * Find active agreement for a workshop.
     */
    Optional<Acordo> findByOficinaIdAndStatus(UUID oficinaId, StatusAcordo status);

    /**
     * Find all active agreements.
     */
    List<Acordo> findByStatusOrderByDataAcordoDesc(StatusAcordo status);

    /**
     * Find active agreements with overdue installments.
     */
    @Query("""
        SELECT DISTINCT a FROM Acordo a
        JOIN a.parcelas p
        WHERE a.status = 'ATIVO'
        AND p.status = 'PENDENTE'
        AND p.dataVencimento < :hoje
        ORDER BY a.dataAcordo ASC
        """)
    List<Acordo> findAcordosComParcelasVencidas(@Param("hoje") LocalDate hoje);

    // =====================================
    // STATISTICS
    // =====================================

    @Query("SELECT COUNT(a) FROM Acordo a WHERE a.status = :status")
    long countByStatus(@Param("status") StatusAcordo status);

    @Query("""
        SELECT COALESCE(SUM(a.valorAcordado), 0) FROM Acordo a
        WHERE a.status = 'ATIVO'
        """)
    BigDecimal sumValorAcordadoAtivo();

    @Query("""
        SELECT COALESCE(SUM(a.valorPago), 0) FROM Acordo a
        WHERE a.status IN ('ATIVO', 'QUITADO')
        AND a.dataAcordo >= :dataInicio
        """)
    BigDecimal sumValorRecuperado(@Param("dataInicio") LocalDate dataInicio);

    @Query("""
        SELECT COALESCE(SUM(a.valorAcordado - a.valorPago), 0) FROM Acordo a
        WHERE a.status = 'ATIVO'
        """)
    BigDecimal sumValorRestanteAtivo();

    // =====================================
    // SEQUENCE NUMBER
    // =====================================

    @Query(value = "SELECT nextval('acordo_numero_seq')", nativeQuery = true)
    Long getNextNumeroSequence();

    @Query("""
        SELECT MAX(CAST(SUBSTRING(a.numero, 10, 5) AS integer))
        FROM Acordo a
        WHERE a.numero LIKE :prefix
        """)
    Integer findMaxNumeroByPrefix(@Param("prefix") String prefix);

    // =====================================
    // WORKSHOP SPECIFIC
    // =====================================

    /**
     * Find the most recent agreement for a workshop.
     */
    Optional<Acordo> findFirstByOficinaIdOrderByDataAcordoDesc(UUID oficinaId);

    /**
     * Count completed agreements for a workshop.
     */
    @Query("SELECT COUNT(a) FROM Acordo a WHERE a.oficina.id = :oficinaId AND a.status = 'QUITADO'")
    long countQuitadosByOficina(@Param("oficinaId") UUID oficinaId);

    /**
     * Count broken agreements for a workshop.
     */
    @Query("SELECT COUNT(a) FROM Acordo a WHERE a.oficina.id = :oficinaId AND a.status = 'QUEBRADO'")
    long countQuebradosByOficina(@Param("oficinaId") UUID oficinaId);
}
