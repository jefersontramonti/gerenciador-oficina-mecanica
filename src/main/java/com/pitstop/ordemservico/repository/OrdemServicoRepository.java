package com.pitstop.ordemservico.repository;

import com.pitstop.ordemservico.domain.OrdemServico;
import com.pitstop.ordemservico.domain.StatusOS;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA para operações de persistência da entidade {@link OrdemServico}.
 *
 * <p>Inclui queries customizadas para:</p>
 * <ul>
 *   <li>Geração de número sequencial</li>
 *   <li>Busca por filtros múltiplos</li>
 *   <li>Agregações para dashboard (contagem por status, faturamento)</li>
 *   <li>Relatórios financeiros</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Repository
public interface OrdemServicoRepository extends JpaRepository<OrdemServico, UUID> {

    /**
     * Gera o próximo número sequencial de OS.
     * Usa a sequence do PostgreSQL (ordem_servico_numero_seq).
     *
     * @return próximo número disponível
     */
    @Query(value = "SELECT nextval('ordem_servico_numero_seq')", nativeQuery = true)
    Long getNextNumero();

    /**
     * Busca OS por número sequencial.
     *
     * @param numero número da OS
     * @return Optional contendo a OS se encontrada
     */
    Optional<OrdemServico> findByNumero(Long numero);

    /**
     * Verifica se existe OS com número específico.
     *
     * @param numero número da OS
     * @return true se existe
     */
    boolean existsByNumero(Long numero);

    /**
     * Busca OS por status.
     *
     * @param status status da OS
     * @param pageable configuração de paginação e ordenação
     * @return página de OS no status especificado
     */
    Page<OrdemServico> findByStatus(StatusOS status, Pageable pageable);

    /**
     * Busca todas as OS de um veículo específico.
     *
     * @param veiculoId ID do veículo
     * @param pageable configuração de paginação e ordenação
     * @return página de OS do veículo
     */
    Page<OrdemServico> findByVeiculoId(UUID veiculoId, Pageable pageable);

    /**
     * Busca todas as OS de um mecânico específico.
     *
     * @param usuarioId ID do usuário (mecânico)
     * @param pageable configuração de paginação e ordenação
     * @return página de OS do mecânico
     */
    Page<OrdemServico> findByUsuarioId(UUID usuarioId, Pageable pageable);

    /**
     * Busca OS de um cliente através do veículo.
     *
     * @param clienteId ID do cliente
     * @param pageable configuração de paginação e ordenação
     * @return página de OS do cliente
     */
    @Query(value = """
        SELECT os.* FROM ordem_servico os
        INNER JOIN veiculos v ON os.veiculo_id = v.id
        WHERE v.cliente_id = CAST(:clienteId AS UUID)
        ORDER BY os.data_abertura DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM ordem_servico os
        INNER JOIN veiculos v ON os.veiculo_id = v.id
        WHERE v.cliente_id = CAST(:clienteId AS UUID)
        """,
        nativeQuery = true)
    Page<OrdemServico> findByClienteId(@Param("clienteId") UUID clienteId, Pageable pageable);

    /**
     * Busca avançada com múltiplos filtros opcionais.
     * Usa COALESCE para evitar problemas com inferência de tipos do PostgreSQL.
     *
     * @param status status da OS (null para ignorar)
     * @param veiculoId ID do veículo (null para ignorar)
     * @param usuarioId ID do mecânico (null para ignorar)
     * @param dataInicio data inicial do período (null para ignorar)
     * @param dataFim data final do período (null para ignorar)
     * @param pageable configuração de paginação e ordenação
     * @return página de OS filtradas
     */
    @Query("""
        SELECT os FROM OrdemServico os
        WHERE (:status IS NULL OR CAST(os.status AS string) = :status)
        AND (:veiculoId IS NULL OR os.veiculoId = :veiculoId)
        AND (:usuarioId IS NULL OR os.usuarioId = :usuarioId)
        AND os.dataAbertura >= COALESCE(:dataInicio, CAST('1900-01-01 00:00:00' AS timestamp))
        AND os.dataAbertura <= COALESCE(:dataFim, CAST('9999-12-31 23:59:59' AS timestamp))
        ORDER BY os.dataAbertura DESC
        """)
    Page<OrdemServico> findByFiltros(
        @Param("status") String status,
        @Param("veiculoId") UUID veiculoId,
        @Param("usuarioId") UUID usuarioId,
        @Param("dataInicio") LocalDateTime dataInicio,
        @Param("dataFim") LocalDateTime dataFim,
        Pageable pageable
    );

    /**
     * Conta OS agrupadas por status.
     * Útil para dashboard (KPIs).
     *
     * @return mapa com status e quantidade
     */
    @Query("SELECT os.status, COUNT(os) FROM OrdemServico os GROUP BY os.status")
    List<Object[]> countByStatus();

    /**
     * Conta OS em aberto (não finalizadas/entregues/canceladas).
     *
     * @return quantidade de OS em aberto
     */
    @Query("SELECT COUNT(os) FROM OrdemServico os WHERE os.status NOT IN ('FINALIZADO', 'ENTREGUE', 'CANCELADO')")
    long countOSEmAberto();

    /**
     * Conta OS finalizadas no período.
     *
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return quantidade de OS finalizadas
     */
    @Query("SELECT COUNT(os) FROM OrdemServico os WHERE os.status = 'FINALIZADO' AND os.dataFinalizacao BETWEEN :dataInicio AND :dataFim")
    long countOSFinalizadas(@Param("dataInicio") LocalDateTime dataInicio, @Param("dataFim") LocalDateTime dataFim);

    /**
     * Calcula faturamento total no período.
     * Considera apenas OS entregues (status = ENTREGUE).
     *
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return valor total faturado
     */
    @Query("SELECT COALESCE(SUM(os.valorFinal), 0) FROM OrdemServico os WHERE os.status = 'ENTREGUE' AND os.dataEntrega BETWEEN :dataInicio AND :dataFim")
    BigDecimal calcularFaturamento(@Param("dataInicio") LocalDateTime dataInicio, @Param("dataFim") LocalDateTime dataFim);

    /**
     * Calcula ticket médio das OS no período.
     *
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return ticket médio
     */
    @Query("SELECT COALESCE(AVG(os.valorFinal), 0) FROM OrdemServico os WHERE os.status = 'ENTREGUE' AND os.dataEntrega BETWEEN :dataInicio AND :dataFim")
    BigDecimal calcularTicketMedio(@Param("dataInicio") LocalDateTime dataInicio, @Param("dataFim") LocalDateTime dataFim);

    /**
     * Calcula valor total pendente (OS abertas).
     *
     * @return valor total em OS abertas
     */
    @Query("SELECT COALESCE(SUM(os.valorFinal), 0) FROM OrdemServico os WHERE os.status NOT IN ('ENTREGUE', 'CANCELADO')")
    BigDecimal calcularValorPendente();

    /**
     * Busca OS com valor final acima de determinado limite.
     * Útil para identificar OS de alto valor.
     *
     * @param valorMinimo valor mínimo
     * @param pageable configuração de paginação e ordenação
     * @return página de OS com valor alto
     */
    @Query("SELECT os FROM OrdemServico os WHERE os.valorFinal >= :valorMinimo ORDER BY os.valorFinal DESC")
    Page<OrdemServico> findByValorFinalGreaterThanEqual(@Param("valorMinimo") BigDecimal valorMinimo, Pageable pageable);

    /**
     * Busca OS atrasadas (data de previsão vencida e não finalizadas).
     *
     * @param dataReferencia data para comparação (geralmente hoje)
     * @param pageable configuração de paginação e ordenação
     * @return página de OS atrasadas
     */
    @Query("SELECT os FROM OrdemServico os WHERE os.dataPrevisao < :dataReferencia AND os.status IN ('APROVADO', 'EM_ANDAMENTO', 'AGUARDANDO_PECA') ORDER BY os.dataPrevisao ASC")
    Page<OrdemServico> findOSAtrasadas(@Param("dataReferencia") LocalDate dataReferencia, Pageable pageable);

    /**
     * Busca OS aguardando peças há mais de X dias.
     *
     * @param dataLimite data limite (X dias atrás)
     * @param pageable configuração de paginação e ordenação
     * @return página de OS paradas há muito tempo
     */
    @Query("SELECT os FROM OrdemServico os WHERE os.status = 'AGUARDANDO_PECA' AND os.updatedAt < :dataLimite ORDER BY os.updatedAt ASC")
    Page<OrdemServico> findOSAguardandoPecaAntiga(@Param("dataLimite") LocalDateTime dataLimite, Pageable pageable);

    /**
     * Busca OS mais antigas ainda em aberto.
     *
     * @param pageable configuração de paginação e ordenação
     * @return página de OS antigas em aberto
     */
    @Query("SELECT os FROM OrdemServico os WHERE os.status NOT IN ('FINALIZADO', 'ENTREGUE', 'CANCELADO') ORDER BY os.dataAbertura ASC")
    Page<OrdemServico> findOSMaisAntigasEmAberto(Pageable pageable);

    /**
     * Ranking de mecânicos por quantidade de OS finalizadas no período.
     *
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return lista de [usuarioId, quantidade]
     */
    @Query("SELECT os.usuarioId, COUNT(os) FROM OrdemServico os WHERE os.status = 'ENTREGUE' AND os.dataEntrega BETWEEN :dataInicio AND :dataFim GROUP BY os.usuarioId ORDER BY COUNT(os) DESC")
    List<Object[]> rankingMecanicosPorQuantidade(@Param("dataInicio") LocalDateTime dataInicio, @Param("dataFim") LocalDateTime dataFim);

    /**
     * Ranking de mecânicos por faturamento no período.
     *
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return lista de [usuarioId, valorTotal]
     */
    @Query("SELECT os.usuarioId, SUM(os.valorFinal) FROM OrdemServico os WHERE os.status = 'ENTREGUE' AND os.dataEntrega BETWEEN :dataInicio AND :dataFim GROUP BY os.usuarioId ORDER BY SUM(os.valorFinal) DESC")
    List<Object[]> rankingMecanicosPorFaturamento(@Param("dataInicio") LocalDateTime dataInicio, @Param("dataFim") LocalDateTime dataFim);

    /**
     * Busca últimas OS de um veículo (histórico).
     *
     * @param veiculoId ID do veículo
     * @param pageable configuração de paginação e ordenação
     * @return página de OS do veículo ordenadas por data decrescente
     */
    @Query("SELECT os FROM OrdemServico os WHERE os.veiculoId = :veiculoId ORDER BY os.dataAbertura DESC")
    Page<OrdemServico> findHistoricoVeiculo(@Param("veiculoId") UUID veiculoId, Pageable pageable);

    // ========== QUERIES PARA DASHBOARD ==========

    /**
     * Conta OS ativas (não canceladas nem entregues).
     * Útil para dashboard - mostra OS em progresso.
     *
     * @return quantidade de OS ativas
     */
    @Query("SELECT COUNT(os) FROM OrdemServico os WHERE os.status NOT IN ('CANCELADO', 'ENTREGUE')")
    long countOSAtivas();

    /**
     * Calcula faturamento do mês atual.
     * Considera apenas OS entregues (status = ENTREGUE) no mês corrente.
     *
     * @return valor total faturado no mês atual
     */
    @Query("""
        SELECT COALESCE(SUM(os.valorFinal), 0)
        FROM OrdemServico os
        WHERE os.status = 'ENTREGUE'
        AND YEAR(os.dataEntrega) = YEAR(CURRENT_DATE)
        AND MONTH(os.dataEntrega) = MONTH(CURRENT_DATE)
        """)
    BigDecimal calcularFaturamentoMesAtual();

    /**
     * Busca OS recentes com dados de cliente e veículo.
     * Usa native query com JOINs para performance otimizada.
     *
     * @param limit quantidade máxima de resultados
     * @return lista de arrays com dados da OS [id, numero, status, clienteNome, veiculoPlaca, dataAbertura, valorFinal]
     */
    @Query(value = """
        SELECT
            os.id,
            os.numero,
            os.status,
            c.nome AS cliente_nome,
            v.placa AS veiculo_placa,
            os.data_abertura,
            os.valor_final
        FROM ordem_servico os
        INNER JOIN veiculos v ON os.veiculo_id = v.id
        INNER JOIN clientes c ON v.cliente_id = c.id
        ORDER BY os.data_abertura DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findRecentOS(@Param("limit") int limit);

    /**
     * Calcula faturamento mensal dos últimos N meses.
     * Retorna array com [ano, mês, valor] ordenado do mais antigo para o mais recente.
     *
     * @param meses quantidade de meses para buscar
     * @return lista de arrays [ano, mês, valorTotal]
     */
    @Query(value = """
        SELECT
            EXTRACT(YEAR FROM os.data_entrega) AS ano,
            EXTRACT(MONTH FROM os.data_entrega) AS mes,
            COALESCE(SUM(os.valor_final), 0) AS valor_total
        FROM ordem_servico os
        WHERE os.status = 'ENTREGUE'
        AND os.data_entrega >= CURRENT_DATE - CAST(:meses || ' months' AS INTERVAL)
        GROUP BY EXTRACT(YEAR FROM os.data_entrega), EXTRACT(MONTH FROM os.data_entrega)
        ORDER BY ano, mes
        """, nativeQuery = true)
    List<Object[]> calcularFaturamentoMensal(@Param("meses") int meses);
}
