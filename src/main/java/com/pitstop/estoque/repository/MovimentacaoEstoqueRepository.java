package com.pitstop.estoque.repository;

import com.pitstop.estoque.domain.MovimentacaoEstoque;
import com.pitstop.estoque.domain.TipoMovimentacao;
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
 * Repository para gerenciamento de movimentações de estoque.
 * Fornece queries para auditoria e relatórios de movimentações.
 *
 * <p><strong>Multi-tenancy:</strong> Todos os métodos agora exigem {@code oficinaId} como
 * primeiro parâmetro para garantir isolamento de dados entre oficinas.</p>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Repository
public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, UUID> {

    /**
     * Busca histórico de movimentações de uma peça específica em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pecaId ID da peça
     * @param pageable paginação e ordenação
     * @return página de movimentações
     */
    @Query("SELECT m FROM MovimentacaoEstoque m WHERE m.oficina.id = :oficinaId AND m.pecaId = :pecaId ORDER BY m.dataMovimentacao DESC")
    Page<MovimentacaoEstoque> findByOficinaIdAndPecaIdOrderByDataMovimentacaoDesc(@Param("oficinaId") UUID oficinaId, @Param("pecaId") UUID pecaId, Pageable pageable);

    /**
     * Busca movimentações de uma Ordem de Serviço específica em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param ordemServicoId ID da OS
     * @return lista de movimentações vinculadas à OS
     */
    @Query("SELECT m FROM MovimentacaoEstoque m WHERE m.oficina.id = :oficinaId AND m.ordemServicoId = :ordemServicoId ORDER BY m.createdAt DESC")
    List<MovimentacaoEstoque> findByOficinaIdAndOrdemServicoIdOrderByCreatedAtDesc(@Param("oficinaId") UUID oficinaId, @Param("ordemServicoId") UUID ordemServicoId);

    /**
     * Busca movimentações por usuário em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param usuarioId ID do usuário
     * @param pageable paginação
     * @return página de movimentações do usuário
     */
    @Query("SELECT m FROM MovimentacaoEstoque m WHERE m.oficina.id = :oficinaId AND m.usuarioId = :usuarioId ORDER BY m.dataMovimentacao DESC")
    Page<MovimentacaoEstoque> findByOficinaIdAndUsuarioIdOrderByDataMovimentacaoDesc(@Param("oficinaId") UUID oficinaId, @Param("usuarioId") UUID usuarioId, Pageable pageable);

    /**
     * Busca movimentações por tipo em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param tipo tipo da movimentação
     * @param pageable paginação
     * @return página de movimentações do tipo especificado
     */
    @Query("SELECT m FROM MovimentacaoEstoque m WHERE m.oficina.id = :oficinaId AND m.tipo = :tipo ORDER BY m.dataMovimentacao DESC")
    Page<MovimentacaoEstoque> findByOficinaIdAndTipoOrderByDataMovimentacaoDesc(@Param("oficinaId") UUID oficinaId, @Param("tipo") TipoMovimentacao tipo, Pageable pageable);

    /**
     * Busca movimentações em um período em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param dataInicio data inicial
     * @param dataFim data final
     * @param pageable paginação
     * @return página de movimentações no período
     */
    @Query("""
            SELECT m FROM MovimentacaoEstoque m
            WHERE m.oficina.id = :oficinaId
            AND m.dataMovimentacao BETWEEN :dataInicio AND :dataFim
            ORDER BY m.dataMovimentacao DESC
            """)
    Page<MovimentacaoEstoque> findByOficinaIdAndPeriodo(
            @Param("oficinaId") UUID oficinaId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            Pageable pageable
    );

    /**
     * Busca movimentações com filtros múltiplos em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pecaId ID da peça (opcional)
     * @param tipo tipo da movimentação (opcional)
     * @param dataInicio data inicial (opcional)
     * @param dataFim data final (opcional)
     * @param usuarioId ID do usuário (opcional)
     * @param pageable paginação
     * @return página de movimentações filtradas
     */
    @Query("""
            SELECT m FROM MovimentacaoEstoque m
            WHERE m.oficina.id = :oficinaId
            AND (CAST(:pecaId AS string) IS NULL OR m.pecaId = :pecaId)
            AND (CAST(:tipo AS string) IS NULL OR m.tipo = :tipo)
            AND (CAST(:dataInicio AS string) IS NULL OR m.dataMovimentacao >= :dataInicio)
            AND (CAST(:dataFim AS string) IS NULL OR m.dataMovimentacao <= :dataFim)
            AND (CAST(:usuarioId AS string) IS NULL OR m.usuarioId = :usuarioId)
            ORDER BY m.dataMovimentacao DESC
            """)
    Page<MovimentacaoEstoque> findByFilters(
            @Param("oficinaId") UUID oficinaId,
            @Param("pecaId") UUID pecaId,
            @Param("tipo") TipoMovimentacao tipo,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("usuarioId") UUID usuarioId,
            Pageable pageable
    );

    /**
     * Calcula o total movimentado (valor) em um período em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return soma dos valores das movimentações
     */
    @Query("""
            SELECT COALESCE(SUM(m.valorTotal), 0)
            FROM MovimentacaoEstoque m
            WHERE m.oficina.id = :oficinaId
            AND m.dataMovimentacao BETWEEN :dataInicio AND :dataFim
            """)
    BigDecimal calcularValorTotalMovimentadoByOficinaId(
            @Param("oficinaId") UUID oficinaId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim
    );

    /**
     * Calcula o total movimentado (valor) por tipo em um período em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param tipo tipo da movimentação
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return soma dos valores das movimentações do tipo
     */
    @Query("""
            SELECT COALESCE(SUM(m.valorTotal), 0)
            FROM MovimentacaoEstoque m
            WHERE m.oficina.id = :oficinaId
            AND m.tipo = :tipo
            AND m.dataMovimentacao BETWEEN :dataInicio AND :dataFim
            """)
    BigDecimal calcularValorMovimentadoPorTipoByOficinaId(
            @Param("oficinaId") UUID oficinaId,
            @Param("tipo") TipoMovimentacao tipo,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim
    );

    /**
     * Conta movimentações por tipo em um período em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param tipo tipo da movimentação
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return quantidade de movimentações
     */
    @Query("""
            SELECT COUNT(m)
            FROM MovimentacaoEstoque m
            WHERE m.oficina.id = :oficinaId
            AND m.tipo = :tipo
            AND m.dataMovimentacao BETWEEN :dataInicio AND :dataFim
            """)
    long countByOficinaIdAndTipoAndPeriodo(
            @Param("oficinaId") UUID oficinaId,
            @Param("tipo") TipoMovimentacao tipo,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim
    );

    /**
     * Busca última movimentação de uma peça em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pecaId ID da peça
     * @return última movimentação ou lista vazia
     */
    @Query("""
            SELECT m FROM MovimentacaoEstoque m
            WHERE m.oficina.id = :oficinaId AND m.pecaId = :pecaId
            ORDER BY m.dataMovimentacao DESC
            LIMIT 1
            """)
    List<MovimentacaoEstoque> findUltimaMovimentacaoByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("pecaId") UUID pecaId);

    /**
     * Busca peças sem movimentação há mais de X dias em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param dataLimite data limite (peças sem movimentação desde esta data)
     * @return lista de IDs de peças sem movimentação recente
     */
    @Query("""
            SELECT DISTINCT m.pecaId FROM MovimentacaoEstoque m
            WHERE m.oficina.id = :oficinaId
            AND m.pecaId NOT IN (
                SELECT m2.pecaId FROM MovimentacaoEstoque m2
                WHERE m2.oficina.id = :oficinaId
                AND m2.dataMovimentacao > :dataLimite
            )
            """)
    List<UUID> findPecasSemMovimentacaoDesdeByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Verifica se existe movimentação para uma OS específica em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param ordemServicoId ID da OS
     * @return true se existe
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM MovimentacaoEstoque m WHERE m.oficina.id = :oficinaId AND m.ordemServicoId = :ordemServicoId")
    boolean existsByOficinaIdAndOrdemServicoId(@Param("oficinaId") UUID oficinaId, @Param("ordemServicoId") UUID ordemServicoId);

    /**
     * Busca movimentação por ID em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param id ID da movimentação
     * @return Optional contendo a movimentação se encontrada
     */
    @Query("SELECT m FROM MovimentacaoEstoque m WHERE m.oficina.id = :oficinaId AND m.id = :id")
    Optional<MovimentacaoEstoque> findByOficinaIdAndId(@Param("oficinaId") UUID oficinaId, @Param("id") UUID id);

    /**
     * Busca todas as movimentações de uma oficina com paginação.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable paginação
     * @return página de movimentações
     */
    @Query("SELECT m FROM MovimentacaoEstoque m WHERE m.oficina.id = :oficinaId ORDER BY m.dataMovimentacao DESC")
    Page<MovimentacaoEstoque> findByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    // ==================== QUERIES PARA FLUXO DE CAIXA ====================

    /**
     * Busca movimentações de ENTRADA (compras) agrupadas por dia.
     * Usado para despesas no fluxo de caixa.
     * Retorna [data, valorTotal].
     */
    @Query(value = """
        SELECT DATE(m.data_movimentacao), COALESCE(SUM(m.valor_total), 0)
        FROM movimentacao_estoque m
        WHERE m.oficina_id = :oficinaId
        AND m.tipo = 'ENTRADA'
        AND DATE(m.data_movimentacao) BETWEEN :dataInicio AND :dataFim
        GROUP BY DATE(m.data_movimentacao)
        ORDER BY DATE(m.data_movimentacao)
        """, nativeQuery = true)
    List<Object[]> findComprasDiariasByPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Busca movimentações de BAIXA_OS (custo das peças vendidas) agrupadas por dia.
     * Usado para calcular CMV real no fluxo de caixa.
     * Retorna [data, valorTotal].
     */
    @Query(value = """
        SELECT DATE(m.data_movimentacao), COALESCE(SUM(m.valor_total), 0)
        FROM movimentacao_estoque m
        WHERE m.oficina_id = :oficinaId
        AND m.tipo = 'BAIXA_OS'
        AND DATE(m.data_movimentacao) BETWEEN :dataInicio AND :dataFim
        GROUP BY DATE(m.data_movimentacao)
        ORDER BY DATE(m.data_movimentacao)
        """, nativeQuery = true)
    List<Object[]> findCMVDiarioByPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Soma total de compras (ENTRADA) em um período.
     */
    @Query("""
        SELECT COALESCE(SUM(m.valorTotal), 0)
        FROM MovimentacaoEstoque m
        WHERE m.oficina.id = :oficinaId
        AND m.tipo = 'ENTRADA'
        AND CAST(m.dataMovimentacao AS localdate) BETWEEN :dataInicio AND :dataFim
        """)
    BigDecimal sumComprasByPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Soma total de CMV (BAIXA_OS) em um período.
     * CMV = Custo das Mercadorias Vendidas (peças usadas nas OS).
     */
    @Query("""
        SELECT COALESCE(SUM(m.valorTotal), 0)
        FROM MovimentacaoEstoque m
        WHERE m.oficina.id = :oficinaId
        AND m.tipo = 'BAIXA_OS'
        AND CAST(m.dataMovimentacao AS localdate) BETWEEN :dataInicio AND :dataFim
        """)
    BigDecimal sumCMVByPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Soma total de devoluções em um período.
     */
    @Query("""
        SELECT COALESCE(SUM(m.valorTotal), 0)
        FROM MovimentacaoEstoque m
        WHERE m.oficina.id = :oficinaId
        AND m.tipo = 'DEVOLUCAO'
        AND CAST(m.dataMovimentacao AS localdate) BETWEEN :dataInicio AND :dataFim
        """)
    BigDecimal sumDevolucoesByPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );
}
