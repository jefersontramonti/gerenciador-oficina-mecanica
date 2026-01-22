package com.pitstop.financeiro.repository;

import com.pitstop.financeiro.domain.CategoriaDespesa;
import com.pitstop.financeiro.domain.Despesa;
import com.pitstop.financeiro.domain.StatusDespesa;
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
 * Repository para despesas operacionais.
 */
@Repository
public interface DespesaRepository extends JpaRepository<Despesa, UUID> {

    // ==================== BUSCAS BÁSICAS ====================

    @Query("SELECT d FROM Despesa d WHERE d.oficina.id = :oficinaId AND d.id = :id")
    Optional<Despesa> findByOficinaIdAndId(@Param("oficinaId") UUID oficinaId, @Param("id") UUID id);

    @Query("SELECT d FROM Despesa d WHERE d.oficina.id = :oficinaId ORDER BY d.dataVencimento DESC")
    Page<Despesa> findByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    // ==================== FILTROS ====================

    @Query("""
        SELECT d FROM Despesa d
        WHERE d.oficina.id = :oficinaId
        AND (:status IS NULL OR d.status = :status)
        AND (:categoria IS NULL OR d.categoria = :categoria)
        AND (:dataInicio IS NULL OR d.dataVencimento >= :dataInicio)
        AND (:dataFim IS NULL OR d.dataVencimento <= :dataFim)
        ORDER BY d.dataVencimento DESC
        """)
    Page<Despesa> findByFilters(
        @Param("oficinaId") UUID oficinaId,
        @Param("status") StatusDespesa status,
        @Param("categoria") CategoriaDespesa categoria,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim,
        Pageable pageable
    );

    // ==================== QUERIES PARA FLUXO DE CAIXA ====================

    /**
     * Soma despesas pagas em um período (para fluxo de caixa - saídas efetivas).
     */
    @Query("""
        SELECT COALESCE(SUM(d.valor), 0)
        FROM Despesa d
        WHERE d.oficina.id = :oficinaId
        AND d.status = 'PAGA'
        AND d.dataPagamento BETWEEN :dataInicio AND :dataFim
        """)
    BigDecimal sumDespesasPagasByPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Busca despesas pagas agrupadas por dia (para gráfico de fluxo de caixa).
     * Retorna [dataPagamento, valorTotal].
     */
    @Query(value = """
        SELECT d.data_pagamento, COALESCE(SUM(d.valor), 0)
        FROM despesas d
        WHERE d.oficina_id = :oficinaId
        AND d.status = 'PAGA'
        AND d.data_pagamento BETWEEN :dataInicio AND :dataFim
        GROUP BY d.data_pagamento
        ORDER BY d.data_pagamento
        """, nativeQuery = true)
    List<Object[]> findDespesasDiariasByPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Busca despesas pagas agrupadas por categoria (para gráfico de pizza).
     * Retorna [categoria, valorTotal, quantidade].
     */
    @Query(value = """
        SELECT d.categoria, COALESCE(SUM(d.valor), 0), COUNT(d.id)
        FROM despesas d
        WHERE d.oficina_id = :oficinaId
        AND d.status = 'PAGA'
        AND d.data_pagamento BETWEEN :dataInicio AND :dataFim
        GROUP BY d.categoria
        ORDER BY SUM(d.valor) DESC
        """, nativeQuery = true)
    List<Object[]> findDespesasPorCategoria(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Busca despesas pagas agrupadas por grupo de categoria.
     * Retorna [grupo, valorTotal].
     */
    @Query("""
        SELECT
            CASE d.categoria
                WHEN 'SALARIOS' THEN 'PESSOAL'
                WHEN 'ENCARGOS_SOCIAIS' THEN 'PESSOAL'
                WHEN 'BENEFICIOS' THEN 'PESSOAL'
                WHEN 'PROLABORE' THEN 'PESSOAL'
                WHEN 'ALUGUEL' THEN 'INSTALACOES'
                WHEN 'CONDOMINIO' THEN 'INSTALACOES'
                WHEN 'IPTU' THEN 'INSTALACOES'
                WHEN 'MANUTENCAO_PREDIAL' THEN 'INSTALACOES'
                WHEN 'ENERGIA_ELETRICA' THEN 'UTILIDADES'
                WHEN 'AGUA' THEN 'UTILIDADES'
                WHEN 'GAS' THEN 'UTILIDADES'
                WHEN 'TELEFONE' THEN 'UTILIDADES'
                WHEN 'INTERNET' THEN 'UTILIDADES'
                WHEN 'COMPRA_PECAS' THEN 'OPERACIONAL'
                WHEN 'FERRAMENTAS' THEN 'OPERACIONAL'
                WHEN 'MATERIAL_CONSUMO' THEN 'OPERACIONAL'
                WHEN 'MATERIAL_LIMPEZA' THEN 'OPERACIONAL'
                WHEN 'DESCARTE_RESIDUOS' THEN 'OPERACIONAL'
                ELSE 'OUTROS'
            END as grupo,
            COALESCE(SUM(d.valor), 0)
        FROM Despesa d
        WHERE d.oficina.id = :oficinaId
        AND d.status = 'PAGA'
        AND d.dataPagamento BETWEEN :dataInicio AND :dataFim
        GROUP BY grupo
        ORDER BY SUM(d.valor) DESC
        """)
    List<Object[]> findDespesasPorGrupo(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    // ==================== QUERIES PARA DRE ====================

    /**
     * Soma despesas com pessoal (salários, encargos, benefícios).
     */
    @Query("""
        SELECT COALESCE(SUM(d.valor), 0)
        FROM Despesa d
        WHERE d.oficina.id = :oficinaId
        AND d.status = 'PAGA'
        AND d.dataPagamento BETWEEN :dataInicio AND :dataFim
        AND d.categoria IN ('SALARIOS', 'ENCARGOS_SOCIAIS', 'BENEFICIOS', 'PROLABORE')
        """)
    BigDecimal sumDespesasPessoal(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Soma despesas administrativas.
     */
    @Query("""
        SELECT COALESCE(SUM(d.valor), 0)
        FROM Despesa d
        WHERE d.oficina.id = :oficinaId
        AND d.status = 'PAGA'
        AND d.dataPagamento BETWEEN :dataInicio AND :dataFim
        AND d.categoria IN ('CONTABILIDADE', 'ADVOCACIA', 'SISTEMAS_SOFTWARE', 'MATERIAL_ESCRITORIO', 'TAXAS_BANCARIAS',
                           'ALUGUEL', 'CONDOMINIO', 'IPTU', 'MANUTENCAO_PREDIAL',
                           'ENERGIA_ELETRICA', 'AGUA', 'GAS', 'TELEFONE', 'INTERNET')
        """)
    BigDecimal sumDespesasAdministrativas(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Soma despesas com marketing.
     */
    @Query("""
        SELECT COALESCE(SUM(d.valor), 0)
        FROM Despesa d
        WHERE d.oficina.id = :oficinaId
        AND d.status = 'PAGA'
        AND d.dataPagamento BETWEEN :dataInicio AND :dataFim
        AND d.categoria IN ('PUBLICIDADE', 'MARKETING_DIGITAL', 'BRINDES')
        """)
    BigDecimal sumDespesasMarketing(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Soma despesas financeiras (juros, tarifas, multas).
     */
    @Query("""
        SELECT COALESCE(SUM(d.valor), 0)
        FROM Despesa d
        WHERE d.oficina.id = :oficinaId
        AND d.status = 'PAGA'
        AND d.dataPagamento BETWEEN :dataInicio AND :dataFim
        AND d.categoria IN ('JUROS_EMPRESTIMOS', 'TARIFAS_CARTAO', 'MULTAS_ATRASOS')
        """)
    BigDecimal sumDespesasFinanceiras(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Soma receitas financeiras (juros recebidos, descontos obtidos, rendimentos).
     * São lançadas como "despesas" com valor positivo nas categorias de receita financeira.
     */
    @Query("""
        SELECT COALESCE(SUM(d.valor), 0)
        FROM Despesa d
        WHERE d.oficina.id = :oficinaId
        AND d.status = 'PAGA'
        AND d.dataPagamento BETWEEN :dataInicio AND :dataFim
        AND d.categoria IN ('JUROS_RECEBIDOS', 'DESCONTOS_OBTIDOS', 'RENDIMENTOS_APLICACAO')
        """)
    BigDecimal sumReceitasFinanceiras(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Soma outras receitas (categoria OUTRAS_RECEITAS).
     * São lançadas como "despesas" com valor positivo.
     */
    @Query("""
        SELECT COALESCE(SUM(d.valor), 0)
        FROM Despesa d
        WHERE d.oficina.id = :oficinaId
        AND d.status = 'PAGA'
        AND d.dataPagamento BETWEEN :dataInicio AND :dataFim
        AND d.categoria = 'OUTRAS_RECEITAS'
        """)
    BigDecimal sumOutrasReceitas(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    // ==================== DESPESAS PENDENTES/VENCIDAS ====================

    /**
     * Conta despesas pendentes.
     */
    @Query("SELECT COUNT(d) FROM Despesa d WHERE d.oficina.id = :oficinaId AND d.status = 'PENDENTE'")
    long countPendentes(@Param("oficinaId") UUID oficinaId);

    /**
     * Soma valor de despesas pendentes.
     */
    @Query("""
        SELECT COALESCE(SUM(d.valor), 0)
        FROM Despesa d
        WHERE d.oficina.id = :oficinaId
        AND d.status = 'PENDENTE'
        """)
    BigDecimal sumDespesasPendentes(@Param("oficinaId") UUID oficinaId);

    /**
     * Busca despesas vencidas.
     */
    @Query("""
        SELECT d FROM Despesa d
        WHERE d.oficina.id = :oficinaId
        AND d.status = 'PENDENTE'
        AND d.dataVencimento < :hoje
        ORDER BY d.dataVencimento
        """)
    List<Despesa> findDespesasVencidas(
        @Param("oficinaId") UUID oficinaId,
        @Param("hoje") LocalDate hoje
    );

    /**
     * Busca despesas a vencer nos próximos N dias.
     */
    @Query("""
        SELECT d FROM Despesa d
        WHERE d.oficina.id = :oficinaId
        AND d.status = 'PENDENTE'
        AND d.dataVencimento BETWEEN :hoje AND :dataLimite
        ORDER BY d.dataVencimento
        """)
    List<Despesa> findDespesasAVencer(
        @Param("oficinaId") UUID oficinaId,
        @Param("hoje") LocalDate hoje,
        @Param("dataLimite") LocalDate dataLimite
    );

    /**
     * Verifica se já existe despesa vinculada a uma movimentação de estoque.
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Despesa d WHERE d.oficina.id = :oficinaId AND d.movimentacaoEstoqueId = :movimentacaoId")
    boolean existsByMovimentacaoEstoqueId(
        @Param("oficinaId") UUID oficinaId,
        @Param("movimentacaoId") UUID movimentacaoId
    );
}
