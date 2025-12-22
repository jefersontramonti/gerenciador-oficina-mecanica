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
import java.util.Optional;
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
 * <p><strong>Multi-tenancy:</strong> Todos os métodos agora exigem {@code oficinaId} como
 * primeiro parâmetro para garantir isolamento de dados entre oficinas.</p>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {

    /**
     * Busca todos os pagamentos de uma ordem de serviço em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param ordemServicoId ID da OS
     * @return lista de pagamentos
     */
    @Query("SELECT p FROM Pagamento p WHERE p.oficina.id = :oficinaId AND p.ordemServicoId = :ordemServicoId")
    List<Pagamento> findByOficinaIdAndOrdemServicoId(@Param("oficinaId") UUID oficinaId, @Param("ordemServicoId") UUID ordemServicoId);

    /**
     * Busca pagamentos por status em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param status status do pagamento
     * @param pageable paginação
     * @return página de pagamentos
     */
    @Query("SELECT p FROM Pagamento p WHERE p.oficina.id = :oficinaId AND p.status = :status")
    Page<Pagamento> findByOficinaIdAndStatus(@Param("oficinaId") UUID oficinaId, @Param("status") StatusPagamento status, Pageable pageable);

    /**
     * Busca pagamentos por tipo em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param tipo tipo de pagamento
     * @param pageable paginação
     * @return página de pagamentos
     */
    @Query("SELECT p FROM Pagamento p WHERE p.oficina.id = :oficinaId AND p.tipo = :tipo")
    Page<Pagamento> findByOficinaIdAndTipo(@Param("oficinaId") UUID oficinaId, @Param("tipo") TipoPagamento tipo, Pageable pageable);

    /**
     * Busca pagamentos vencidos (data vencimento passou e status = PENDENTE) em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param dataReferencia data de referência (geralmente hoje)
     * @param pageable paginação
     * @return página de pagamentos vencidos
     */
    @Query("""
        SELECT p FROM Pagamento p
        WHERE p.oficina.id = :oficinaId
        AND p.status = 'PENDENTE'
        AND p.dataVencimento < :dataReferencia
        ORDER BY p.dataVencimento ASC
        """)
    Page<Pagamento> findVencidosByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("dataReferencia") LocalDate dataReferencia, Pageable pageable);

    /**
     * Calcula o total pago de uma ordem de serviço em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param ordemServicoId ID da OS
     * @return valor total pago
     */
    @Query("""
        SELECT COALESCE(SUM(p.valor), 0)
        FROM Pagamento p
        WHERE p.oficina.id = :oficinaId
        AND p.ordemServicoId = :ordemServicoId
        AND p.status = 'PAGO'
        """)
    BigDecimal calcularTotalPagoByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("ordemServicoId") UUID ordemServicoId);

    /**
     * Calcula o total pendente de uma ordem de serviço em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param ordemServicoId ID da OS
     * @return valor total pendente
     */
    @Query("""
        SELECT COALESCE(SUM(p.valor), 0)
        FROM Pagamento p
        WHERE p.oficina.id = :oficinaId
        AND p.ordemServicoId = :ordemServicoId
        AND p.status IN ('PENDENTE', 'VENCIDO')
        """)
    BigDecimal calcularTotalPendenteByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("ordemServicoId") UUID ordemServicoId);

    /**
     * Verifica se uma OS está totalmente paga em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param ordemServicoId ID da OS
     * @return true se não há pagamentos pendentes
     */
    @Query("""
        SELECT CASE WHEN COUNT(p) = 0 THEN true ELSE false END
        FROM Pagamento p
        WHERE p.oficina.id = :oficinaId
        AND p.ordemServicoId = :ordemServicoId
        AND p.status IN ('PENDENTE', 'VENCIDO')
        """)
    boolean isOrdemServicoQuitadaByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("ordemServicoId") UUID ordemServicoId);

    /**
     * Busca pagamentos em um período em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param dataInicio data inicial
     * @param dataFim data final
     * @param pageable paginação
     * @return página de pagamentos
     */
    @Query("""
        SELECT p FROM Pagamento p
        WHERE p.oficina.id = :oficinaId
        AND p.dataPagamento BETWEEN :dataInicio AND :dataFim
        AND p.status = 'PAGO'
        ORDER BY p.dataPagamento DESC
        """)
    Page<Pagamento> findByOficinaIdAndPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim,
        Pageable pageable
    );

    /**
     * Calcula total de pagamentos no período em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return valor total
     */
    @Query("""
        SELECT COALESCE(SUM(p.valor), 0)
        FROM Pagamento p
        WHERE p.oficina.id = :oficinaId
        AND p.dataPagamento BETWEEN :dataInicio AND :dataFim
        AND p.status = 'PAGO'
        """)
    BigDecimal calcularTotalPeriodoByOficinaId(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Estatísticas de pagamentos por tipo em uma oficina.
     * Retorna [TipoPagamento, quantidade, valorTotal].
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de arrays com estatísticas
     */
    @Query("""
        SELECT p.tipo, COUNT(p), SUM(p.valor)
        FROM Pagamento p
        WHERE p.oficina.id = :oficinaId AND p.status = 'PAGO'
        GROUP BY p.tipo
        ORDER BY SUM(p.valor) DESC
        """)
    List<Object[]> estatisticasPorTipoByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Estatísticas de pagamentos por status em uma oficina.
     * Retorna [StatusPagamento, quantidade, valorTotal].
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de arrays com estatísticas
     */
    @Query("""
        SELECT p.status, COUNT(p), SUM(p.valor)
        FROM Pagamento p
        WHERE p.oficina.id = :oficinaId
        GROUP BY p.status
        ORDER BY CASE p.status
            WHEN 'PENDENTE' THEN 1
            WHEN 'VENCIDO' THEN 2
            WHEN 'PAGO' THEN 3
            WHEN 'CANCELADO' THEN 4
            WHEN 'ESTORNADO' THEN 5
        END
        """)
    List<Object[]> estatisticasPorStatusByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Busca pagamentos com filtros avançados em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param tipo tipo de pagamento (null para ignorar)
     * @param status status (null para ignorar)
     * @param dataInicio data inicial (null para ignorar)
     * @param dataFim data final (null para ignorar)
     * @param pageable paginação
     * @return página de pagamentos
     */
    @Query("""
        SELECT p FROM Pagamento p
        WHERE p.oficina.id = :oficinaId
        AND (:tipo IS NULL OR p.tipo = :tipo)
        AND (:status IS NULL OR CAST(p.status AS string) = :status)
        AND p.createdAt >= COALESCE(:dataInicio, CAST('1900-01-01 00:00:00' AS timestamp))
        AND p.createdAt <= COALESCE(:dataFim, CAST('9999-12-31 23:59:59' AS timestamp))
        ORDER BY p.createdAt DESC
        """)
    Page<Pagamento> findByFiltros(
        @Param("oficinaId") UUID oficinaId,
        @Param("tipo") TipoPagamento tipo,
        @Param("status") String status,
        @Param("dataInicio") LocalDateTime dataInicio,
        @Param("dataFim") LocalDateTime dataFim,
        Pageable pageable
    );

    /**
     * Busca pagamento por ID em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param id ID do pagamento
     * @return Optional contendo o pagamento se encontrado
     */
    @Query("SELECT p FROM Pagamento p WHERE p.oficina.id = :oficinaId AND p.id = :id")
    Optional<Pagamento> findByOficinaIdAndId(@Param("oficinaId") UUID oficinaId, @Param("id") UUID id);

    /**
     * Busca todos os pagamentos de uma oficina com paginação.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable paginação
     * @return página de pagamentos
     */
    @Query("SELECT p FROM Pagamento p WHERE p.oficina.id = :oficinaId ORDER BY p.createdAt DESC")
    Page<Pagamento> findByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Alias para calcularTotalPagoByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @param ordemServicoId ID da OS
     * @return valor total pago
     */
    default BigDecimal calcularTotalPago(UUID oficinaId, UUID ordemServicoId) {
        return calcularTotalPagoByOficinaId(oficinaId, ordemServicoId);
    }

    /**
     * Alias para calcularTotalPendenteByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @param ordemServicoId ID da OS
     * @return valor total pendente
     */
    default BigDecimal calcularTotalPendente(UUID oficinaId, UUID ordemServicoId) {
        return calcularTotalPendenteByOficinaId(oficinaId, ordemServicoId);
    }

    /**
     * Alias para isOrdemServicoQuitadaByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @param ordemServicoId ID da OS
     * @return true se está quitada
     */
    default boolean isOrdemServicoQuitada(UUID oficinaId, UUID ordemServicoId) {
        return isOrdemServicoQuitadaByOficinaId(oficinaId, ordemServicoId);
    }

    /**
     * Alias para findVencidosByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @param dataReferencia data de referência
     * @param pageable paginação
     * @return página de pagamentos vencidos
     */
    default Page<Pagamento> findVencidos(UUID oficinaId, LocalDate dataReferencia, Pageable pageable) {
        return findVencidosByOficinaId(oficinaId, dataReferencia, pageable);
    }
}
