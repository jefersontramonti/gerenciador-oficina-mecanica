package com.pitstop.saas.repository;

import com.pitstop.saas.domain.Fatura;
import com.pitstop.saas.domain.StatusFatura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Invoice (Fatura) entity.
 */
@Repository
public interface FaturaRepository extends JpaRepository<Fatura, UUID> {

    // =====================================
    // FIND BY SINGLE FIELD
    // =====================================

    Optional<Fatura> findByNumero(String numero);

    Page<Fatura> findByOficinaId(UUID oficinaId, Pageable pageable);

    List<Fatura> findByDataEmissaoBetween(LocalDate dataInicio, LocalDate dataFim);

    List<Fatura> findByOficinaIdAndStatus(UUID oficinaId, StatusFatura status);

    Page<Fatura> findByStatus(StatusFatura status, Pageable pageable);

    List<Fatura> findByOficinaIdOrderByDataEmissaoDesc(UUID oficinaId);

    // =====================================
    // FIND WITH FILTERS
    // =====================================

    @Query("""
        SELECT f FROM Fatura f
        WHERE (:oficinaId IS NULL OR f.oficina.id = :oficinaId)
        AND (:status IS NULL OR f.status = :status)
        AND (:dataInicio IS NULL OR f.dataEmissao >= :dataInicio)
        AND (:dataFim IS NULL OR f.dataEmissao <= :dataFim)
        ORDER BY f.dataEmissao DESC
        """)
    Page<Fatura> findWithFilters(
        @Param("oficinaId") UUID oficinaId,
        @Param("status") StatusFatura status,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim,
        Pageable pageable
    );

    // =====================================
    // EXISTENCE CHECKS
    // =====================================

    boolean existsByOficinaIdAndMesReferenciaAndStatusNot(
        UUID oficinaId,
        LocalDate mesReferencia,
        StatusFatura statusExcluido
    );

    @Query("""
        SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
        FROM Fatura f
        WHERE f.oficina.id = :oficinaId
        AND f.mesReferencia = :mesReferencia
        AND f.status != 'CANCELADO'
        """)
    boolean existsFaturaParaMes(
        @Param("oficinaId") UUID oficinaId,
        @Param("mesReferencia") LocalDate mesReferencia
    );

    // =====================================
    // OVERDUE AND PENDING QUERIES
    // =====================================

    /**
     * Find pending invoices that are overdue (past due date).
     */
    @Query("""
        SELECT f FROM Fatura f
        WHERE f.status = 'PENDENTE'
        AND f.dataVencimento < :hoje
        ORDER BY f.dataVencimento ASC
        """)
    List<Fatura> findPendentesVencidas(@Param("hoje") LocalDate hoje);

    /**
     * Find invoices due within a number of days (for reminders).
     */
    @Query("""
        SELECT f FROM Fatura f
        WHERE f.status = 'PENDENTE'
        AND f.dataVencimento BETWEEN :hoje AND :dataLimite
        ORDER BY f.dataVencimento ASC
        """)
    List<Fatura> findVencimentoProximo(
        @Param("hoje") LocalDate hoje,
        @Param("dataLimite") LocalDate dataLimite
    );

    /**
     * Find overdue invoices for a specific workshop.
     */
    @Query("""
        SELECT f FROM Fatura f
        WHERE f.oficina.id = :oficinaId
        AND f.status IN ('PENDENTE', 'VENCIDO')
        ORDER BY f.dataVencimento ASC
        """)
    List<Fatura> findPendentesOuVencidasByOficina(@Param("oficinaId") UUID oficinaId);

    // =====================================
    // STATISTICS
    // =====================================

    @Query("SELECT COUNT(f) FROM Fatura f WHERE f.status = :status")
    long countByStatus(@Param("status") StatusFatura status);

    @Query("""
        SELECT COUNT(f) FROM Fatura f
        WHERE f.status IN ('PENDENTE', 'VENCIDO')
        AND f.dataVencimento < :hoje
        """)
    long countInadimplentes(@Param("hoje") LocalDate hoje);

    @Query("""
        SELECT COALESCE(SUM(f.valorTotal), 0) FROM Fatura f
        WHERE f.status IN ('PENDENTE', 'VENCIDO')
        """)
    BigDecimal sumValorPendente();

    @Query("""
        SELECT COALESCE(SUM(f.valorTotal), 0) FROM Fatura f
        WHERE f.status = 'PAGO'
        AND f.dataPagamento >= :dataInicio
        AND f.dataPagamento < :dataFim
        """)
    BigDecimal sumValorRecebido(
        @Param("dataInicio") LocalDateTime dataInicio,
        @Param("dataFim") LocalDateTime dataFim
    );

    @Query("""
        SELECT COALESCE(SUM(f.valorTotal), 0) FROM Fatura f
        WHERE f.oficina.id = :oficinaId
        AND f.status = 'PAGO'
        """)
    BigDecimal sumValorPagoByOficina(@Param("oficinaId") UUID oficinaId);

    // =====================================
    // SEQUENCE NUMBER
    // =====================================

    @Query(value = "SELECT nextval('seq_fatura_numero')", nativeQuery = true)
    Long getNextNumeroSequence();

    @Query("""
        SELECT MAX(CAST(SUBSTRING(f.numero, 10, 5) AS integer))
        FROM Fatura f
        WHERE f.numero LIKE :prefix
        """)
    Integer findMaxNumeroByPrefix(@Param("prefix") String prefix);

    // =====================================
    // WORKSHOP SPECIFIC
    // =====================================

    /**
     * Find the most recent invoice for a workshop.
     */
    Optional<Fatura> findFirstByOficinaIdOrderByDataEmissaoDesc(UUID oficinaId);

    /**
     * Find paid invoices count for a workshop.
     */
    @Query("SELECT COUNT(f) FROM Fatura f WHERE f.oficina.id = :oficinaId AND f.status = 'PAGO'")
    long countPagasByOficina(@Param("oficinaId") UUID oficinaId);
}
