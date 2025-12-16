package com.pitstop.financeiro.repository;

import com.pitstop.financeiro.domain.Pagamento;
import com.pitstop.financeiro.domain.StatusPagamento;
import com.pitstop.financeiro.domain.TipoPagamento;
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
import java.util.UUID;

/**
 * Repositório JPA para operações de persistência da entidade {@link Pagamento}.
 *
 * <p>Inclui queries customizadas para:</p>
 * <ul>
 *   <li>Busca por ordem de serviço e status</li>
 *   <li>Agregações financeiras (total pago, total pendente)</li>
 *   <li>Relatórios de pagamentos vencidos</li>
 *   <li>Estatísticas por tipo de pagamento</li>
 * </ul>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {

    /**
     * Busca todos os pagamentos de uma ordem de serviço.
     *
     * @param ordemServicoId ID da OS
     * @return lista de pagamentos
     */
    List<Pagamento> findByOrdemServicoId(UUID ordemServicoId);

    /**
     * Busca pagamentos por status.
     *
     * @param status status do pagamento
     * @param pageable paginação
     * @return página de pagamentos
     */
    Page<Pagamento> findByStatus(StatusPagamento status, Pageable pageable);

    /**
     * Busca pagamentos por tipo.
     *
     * @param tipo tipo de pagamento
     * @param pageable paginação
     * @return página de pagamentos
     */
    Page<Pagamento> findByTipo(TipoPagamento tipo, Pageable pageable);

    /**
     * Busca pagamentos vencidos (data vencimento passou e status = PENDENTE).
     *
     * @param dataReferencia data de referência (geralmente hoje)
     * @param pageable paginação
     * @return página de pagamentos vencidos
     */
    @Query("""
        SELECT p FROM Pagamento p
        WHERE p.status = 'PENDENTE'
        AND p.dataVencimento < :dataReferencia
        ORDER BY p.dataVencimento ASC
        """)
    Page<Pagamento> findVencidos(@Param("dataReferencia") LocalDate dataReferencia, Pageable pageable);

    /**
     * Calcula o total pago de uma ordem de serviço.
     *
     * @param ordemServicoId ID da OS
     * @return valor total pago
     */
    @Query("""
        SELECT COALESCE(SUM(p.valor), 0)
        FROM Pagamento p
        WHERE p.ordemServicoId = :ordemServicoId
        AND p.status = 'PAGO'
        """)
    BigDecimal calcularTotalPago(@Param("ordemServicoId") UUID ordemServicoId);

    /**
     * Calcula o total pendente de uma ordem de serviço.
     *
     * @param ordemServicoId ID da OS
     * @return valor total pendente
     */
    @Query("""
        SELECT COALESCE(SUM(p.valor), 0)
        FROM Pagamento p
        WHERE p.ordemServicoId = :ordemServicoId
        AND p.status IN ('PENDENTE', 'VENCIDO')
        """)
    BigDecimal calcularTotalPendente(@Param("ordemServicoId") UUID ordemServicoId);

    /**
     * Verifica se uma OS está totalmente paga.
     *
     * @param ordemServicoId ID da OS
     * @return true se não há pagamentos pendentes
     */
    @Query("""
        SELECT CASE WHEN COUNT(p) = 0 THEN true ELSE false END
        FROM Pagamento p
        WHERE p.ordemServicoId = :ordemServicoId
        AND p.status IN ('PENDENTE', 'VENCIDO')
        """)
    boolean isOrdemServicoQuitada(@Param("ordemServicoId") UUID ordemServicoId);

    /**
     * Busca pagamentos em um período.
     *
     * @param dataInicio data inicial
     * @param dataFim data final
     * @param pageable paginação
     * @return página de pagamentos
     */
    @Query("""
        SELECT p FROM Pagamento p
        WHERE p.dataPagamento BETWEEN :dataInicio AND :dataFim
        AND p.status = 'PAGO'
        ORDER BY p.dataPagamento DESC
        """)
    Page<Pagamento> findByPeriodo(
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim,
        Pageable pageable
    );

    /**
     * Calcula total de pagamentos no período.
     *
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return valor total
     */
    @Query("""
        SELECT COALESCE(SUM(p.valor), 0)
        FROM Pagamento p
        WHERE p.dataPagamento BETWEEN :dataInicio AND :dataFim
        AND p.status = 'PAGO'
        """)
    BigDecimal calcularTotalPeriodo(
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Estatísticas de pagamentos por tipo.
     * Retorna [TipoPagamento, quantidade, valorTotal].
     *
     * @return lista de arrays com estatísticas
     */
    @Query("""
        SELECT p.tipo, COUNT(p), SUM(p.valor)
        FROM Pagamento p
        WHERE p.status = 'PAGO'
        GROUP BY p.tipo
        ORDER BY SUM(p.valor) DESC
        """)
    List<Object[]> estatisticasPorTipo();

    /**
     * Estatísticas de pagamentos por status.
     * Retorna [StatusPagamento, quantidade, valorTotal].
     *
     * @return lista de arrays com estatísticas
     */
    @Query("""
        SELECT p.status, COUNT(p), SUM(p.valor)
        FROM Pagamento p
        GROUP BY p.status
        ORDER BY CASE p.status
            WHEN 'PENDENTE' THEN 1
            WHEN 'VENCIDO' THEN 2
            WHEN 'PAGO' THEN 3
            WHEN 'CANCELADO' THEN 4
            WHEN 'ESTORNADO' THEN 5
        END
        """)
    List<Object[]> estatisticasPorStatus();

    /**
     * Busca pagamentos com filtros avançados.
     *
     * @param tipo tipo de pagamento (null para ignorar)
     * @param status status (null para ignorar)
     * @param dataInicio data inicial (null para ignorar)
     * @param dataFim data final (null para ignorar)
     * @param pageable paginação
     * @return página de pagamentos
     */
    @Query("""
        SELECT p FROM Pagamento p
        WHERE (:tipo IS NULL OR p.tipo = :tipo)
        AND (:status IS NULL OR CAST(p.status AS string) = :status)
        AND p.createdAt >= COALESCE(:dataInicio, CAST('1900-01-01 00:00:00' AS timestamp))
        AND p.createdAt <= COALESCE(:dataFim, CAST('9999-12-31 23:59:59' AS timestamp))
        ORDER BY p.createdAt DESC
        """)
    Page<Pagamento> findByFiltros(
        @Param("tipo") TipoPagamento tipo,
        @Param("status") String status,
        @Param("dataInicio") LocalDateTime dataInicio,
        @Param("dataFim") LocalDateTime dataFim,
        Pageable pageable
    );
}
