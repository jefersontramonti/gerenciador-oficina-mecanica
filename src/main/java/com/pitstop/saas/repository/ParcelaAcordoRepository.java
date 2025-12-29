package com.pitstop.saas.repository;

import com.pitstop.saas.domain.ParcelaAcordo;
import com.pitstop.saas.domain.StatusParcela;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for agreement installments (ParcelaAcordo).
 */
@Repository
public interface ParcelaAcordoRepository extends JpaRepository<ParcelaAcordo, UUID> {

    List<ParcelaAcordo> findByAcordoIdOrderByNumeroParcelaAsc(UUID acordoId);

    List<ParcelaAcordo> findByAcordoIdAndStatus(UUID acordoId, StatusParcela status);

    /**
     * Find overdue installments.
     */
    @Query("""
        SELECT p FROM ParcelaAcordo p
        WHERE p.status = 'PENDENTE'
        AND p.dataVencimento < :hoje
        ORDER BY p.dataVencimento ASC
        """)
    List<ParcelaAcordo> findParcelasVencidas(@Param("hoje") LocalDate hoje);

    /**
     * Find installments due soon.
     */
    @Query("""
        SELECT p FROM ParcelaAcordo p
        WHERE p.status = 'PENDENTE'
        AND p.dataVencimento BETWEEN :hoje AND :dataLimite
        ORDER BY p.dataVencimento ASC
        """)
    List<ParcelaAcordo> findVencimentoProximo(
        @Param("hoje") LocalDate hoje,
        @Param("dataLimite") LocalDate dataLimite
    );

    /**
     * Sum of paid installments for date range.
     */
    @Query("""
        SELECT COALESCE(SUM(p.valor), 0) FROM ParcelaAcordo p
        WHERE p.status = 'PAGO'
        AND p.dataPagamento >= :dataInicio
        AND p.dataPagamento <= :dataFim
        """)
    BigDecimal sumValorPago(
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Count overdue installments.
     */
    @Query("""
        SELECT COUNT(p) FROM ParcelaAcordo p
        WHERE p.status IN ('PENDENTE', 'VENCIDO')
        AND p.dataVencimento < :hoje
        """)
    long countParcelasVencidas(@Param("hoje") LocalDate hoje);
}
