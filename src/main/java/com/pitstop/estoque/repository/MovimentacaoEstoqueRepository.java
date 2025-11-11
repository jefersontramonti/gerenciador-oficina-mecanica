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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository para gerenciamento de movimentações de estoque.
 * Fornece queries para auditoria e relatórios de movimentações.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Repository
public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, UUID> {

    /**
     * Busca histórico de movimentações de uma peça específica.
     *
     * @param pecaId ID da peça
     * @param pageable paginação e ordenação
     * @return página de movimentações
     */
    Page<MovimentacaoEstoque> findByPecaIdOrderByDataMovimentacaoDesc(UUID pecaId, Pageable pageable);

    /**
     * Busca movimentações de uma Ordem de Serviço específica.
     *
     * @param ordemServicoId ID da OS
     * @return lista de movimentações vinculadas à OS
     */
    List<MovimentacaoEstoque> findByOrdemServicoIdOrderByCreatedAtDesc(UUID ordemServicoId);

    /**
     * Busca movimentações por usuário.
     *
     * @param usuarioId ID do usuário
     * @param pageable paginação
     * @return página de movimentações do usuário
     */
    Page<MovimentacaoEstoque> findByUsuarioIdOrderByDataMovimentacaoDesc(UUID usuarioId, Pageable pageable);

    /**
     * Busca movimentações por tipo.
     *
     * @param tipo tipo da movimentação
     * @param pageable paginação
     * @return página de movimentações do tipo especificado
     */
    Page<MovimentacaoEstoque> findByTipoOrderByDataMovimentacaoDesc(TipoMovimentacao tipo, Pageable pageable);

    /**
     * Busca movimentações em um período.
     *
     * @param dataInicio data inicial
     * @param dataFim data final
     * @param pageable paginação
     * @return página de movimentações no período
     */
    @Query("""
            SELECT m FROM MovimentacaoEstoque m
            WHERE m.dataMovimentacao BETWEEN :dataInicio AND :dataFim
            ORDER BY m.dataMovimentacao DESC
            """)
    Page<MovimentacaoEstoque> findByPeriodo(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            Pageable pageable
    );

    /**
     * Busca movimentações com filtros múltiplos.
     *
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
            WHERE (CAST(:pecaId AS string) IS NULL OR m.pecaId = :pecaId)
            AND (CAST(:tipo AS string) IS NULL OR m.tipo = :tipo)
            AND (CAST(:dataInicio AS string) IS NULL OR m.dataMovimentacao >= :dataInicio)
            AND (CAST(:dataFim AS string) IS NULL OR m.dataMovimentacao <= :dataFim)
            AND (CAST(:usuarioId AS string) IS NULL OR m.usuarioId = :usuarioId)
            ORDER BY m.dataMovimentacao DESC
            """)
    Page<MovimentacaoEstoque> findByFilters(
            @Param("pecaId") UUID pecaId,
            @Param("tipo") TipoMovimentacao tipo,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("usuarioId") UUID usuarioId,
            Pageable pageable
    );

    /**
     * Calcula o total movimentado (valor) em um período.
     *
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return soma dos valores das movimentações
     */
    @Query("""
            SELECT COALESCE(SUM(m.valorTotal), 0)
            FROM MovimentacaoEstoque m
            WHERE m.dataMovimentacao BETWEEN :dataInicio AND :dataFim
            """)
    BigDecimal calcularValorTotalMovimentado(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim
    );

    /**
     * Calcula o total movimentado (valor) por tipo em um período.
     *
     * @param tipo tipo da movimentação
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return soma dos valores das movimentações do tipo
     */
    @Query("""
            SELECT COALESCE(SUM(m.valorTotal), 0)
            FROM MovimentacaoEstoque m
            WHERE m.tipo = :tipo
            AND m.dataMovimentacao BETWEEN :dataInicio AND :dataFim
            """)
    BigDecimal calcularValorMovimentadoPorTipo(
            @Param("tipo") TipoMovimentacao tipo,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim
    );

    /**
     * Conta movimentações por tipo em um período.
     *
     * @param tipo tipo da movimentação
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return quantidade de movimentações
     */
    @Query("""
            SELECT COUNT(m)
            FROM MovimentacaoEstoque m
            WHERE m.tipo = :tipo
            AND m.dataMovimentacao BETWEEN :dataInicio AND :dataFim
            """)
    long countByTipoAndPeriodo(
            @Param("tipo") TipoMovimentacao tipo,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim
    );

    /**
     * Busca última movimentação de uma peça.
     *
     * @param pecaId ID da peça
     * @return última movimentação ou lista vazia
     */
    @Query("""
            SELECT m FROM MovimentacaoEstoque m
            WHERE m.pecaId = :pecaId
            ORDER BY m.dataMovimentacao DESC
            LIMIT 1
            """)
    List<MovimentacaoEstoque> findUltimaMovimentacao(@Param("pecaId") UUID pecaId);

    /**
     * Busca peças sem movimentação há mais de X dias.
     *
     * @param dataLimite data limite (peças sem movimentação desde esta data)
     * @return lista de IDs de peças sem movimentação recente
     */
    @Query("""
            SELECT DISTINCT m.pecaId FROM MovimentacaoEstoque m
            WHERE m.pecaId NOT IN (
                SELECT m2.pecaId FROM MovimentacaoEstoque m2
                WHERE m2.dataMovimentacao > :dataLimite
            )
            """)
    List<UUID> findPecasSemMovimentacaoDesde(@Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Verifica se existe movimentação para uma OS específica.
     *
     * @param ordemServicoId ID da OS
     * @return true se existe
     */
    boolean existsByOrdemServicoId(UUID ordemServicoId);
}
