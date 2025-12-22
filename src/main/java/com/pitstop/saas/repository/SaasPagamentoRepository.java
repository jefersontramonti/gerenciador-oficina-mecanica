package com.pitstop.saas.repository;

import com.pitstop.saas.domain.SaasPagamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing SaaS subscription payments.
 *
 * Provides queries for payment tracking, overdue detection,
 * and financial reporting.
 *
 * @author PitStop Team
 */
@Repository
public interface SaasPagamentoRepository extends JpaRepository<SaasPagamento, UUID> {

    /**
     * Finds all payments for a specific workshop.
     *
     * @param oficinaId workshop identifier
     * @param pageable pagination parameters
     * @return paginated list of payments ordered by reference month descending
     */
    Page<SaasPagamento> findByOficinaIdOrderByReferenciaMesDesc(UUID oficinaId, Pageable pageable);

    /**
     * Finds a payment for a specific workshop and month.
     *
     * @param oficinaId workshop identifier
     * @param referenciaMes month as first day of the month (e.g. 2025-01-01)
     * @return payment if found
     */
    Optional<SaasPagamento> findByOficinaIdAndReferenciaMes(UUID oficinaId, LocalDate referenciaMes);

    /**
     * Counts how many payments a workshop has made.
     *
     * @param oficinaId workshop identifier
     * @return total number of payments
     */
    long countByOficinaId(UUID oficinaId);

    /**
     * Finds the most recent payment for a workshop.
     *
     * @param oficinaId workshop identifier
     * @return most recent payment if exists
     */
    @Query("""
        SELECT p FROM SaasPagamento p
        WHERE p.oficina.id = :oficinaId
        ORDER BY p.dataPagamento DESC
        LIMIT 1
        """)
    Optional<SaasPagamento> findMostRecentByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Finds all payments made within a date range.
     *
     * @param dataInicio start date (inclusive)
     * @param dataFim end date (inclusive)
     * @param pageable pagination parameters
     * @return paginated list of payments
     */
    @Query("""
        SELECT p FROM SaasPagamento p
        WHERE p.dataPagamento BETWEEN :dataInicio AND :dataFim
        ORDER BY p.dataPagamento DESC
        """)
    Page<SaasPagamento> findByDataPagamentoBetween(
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim,
        Pageable pageable
    );

    /**
     * Finds all overdue payments (paid after due date).
     *
     * @param pageable pagination parameters
     * @return paginated list of late payments
     */
    @Query("""
        SELECT p FROM SaasPagamento p
        WHERE p.dataPagamento > p.dataVencimento
        ORDER BY p.dataVencimento DESC
        """)
    Page<SaasPagamento> findPagamentosAtrasados(Pageable pageable);

    /**
     * Counts total overdue payments in the system.
     *
     * @return number of late payments
     */
    @Query("""
        SELECT COUNT(p) FROM SaasPagamento p
        WHERE p.dataPagamento > p.dataVencimento
        """)
    long countPagamentosAtrasados();

    /**
     * Finds all payments for a specific month reference.
     *
     * @param referenciaMes month as first day of the month (e.g. 2025-01-01)
     * @return list of all payments for that month
     */
    List<SaasPagamento> findByReferenciaMes(LocalDate referenciaMes);
}
