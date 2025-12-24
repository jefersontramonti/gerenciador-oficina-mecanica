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
 * <p><strong>Multi-tenancy:</strong> Todos os métodos agora exigem {@code oficinaId} como
 * primeiro parâmetro para garantir isolamento de dados entre oficinas.</p>
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
     * Busca OS por número sequencial em uma oficina específica.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param numero número da OS
     * @return Optional contendo a OS se encontrada
     */
    @Query("SELECT os FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.numero = :numero")
    Optional<OrdemServico> findByOficinaIdAndNumero(@Param("oficinaId") UUID oficinaId, @Param("numero") Long numero);

    /**
     * Verifica se existe OS com número específico em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param numero número da OS
     * @return true se existe
     */
    @Query("SELECT CASE WHEN COUNT(os) > 0 THEN true ELSE false END FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.numero = :numero")
    boolean existsByOficinaIdAndNumero(@Param("oficinaId") UUID oficinaId, @Param("numero") Long numero);

    /**
     * Busca OS por status em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param status status da OS
     * @param pageable configuração de paginação e ordenação
     * @return página de OS no status especificado
     */
    @Query("SELECT os FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.status = :status")
    Page<OrdemServico> findByOficinaIdAndStatus(@Param("oficinaId") UUID oficinaId, @Param("status") StatusOS status, Pageable pageable);

    /**
     * Busca todas as OS de um veículo específico em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param veiculoId ID do veículo
     * @param pageable configuração de paginação e ordenação
     * @return página de OS do veículo
     */
    @Query("SELECT os FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.veiculoId = :veiculoId")
    Page<OrdemServico> findByOficinaIdAndVeiculoId(@Param("oficinaId") UUID oficinaId, @Param("veiculoId") UUID veiculoId, Pageable pageable);

    /**
     * Busca todas as OS de um mecânico específico em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param usuarioId ID do usuário (mecânico)
     * @param pageable configuração de paginação e ordenação
     * @return página de OS do mecânico
     */
    @Query("SELECT os FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.usuarioId = :usuarioId")
    Page<OrdemServico> findByOficinaIdAndUsuarioId(@Param("oficinaId") UUID oficinaId, @Param("usuarioId") UUID usuarioId, Pageable pageable);

    /**
     * Busca OS de um cliente através do veículo em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param clienteId ID do cliente
     * @param pageable configuração de paginação e ordenação
     * @return página de OS do cliente
     */
    @Query(value = """
        SELECT os.* FROM ordem_servico os
        INNER JOIN veiculos v ON os.veiculo_id = v.id
        WHERE os.oficina_id = CAST(:oficinaId AS UUID)
        AND v.cliente_id = CAST(:clienteId AS UUID)
        ORDER BY os.data_abertura DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM ordem_servico os
        INNER JOIN veiculos v ON os.veiculo_id = v.id
        WHERE os.oficina_id = CAST(:oficinaId AS UUID)
        AND v.cliente_id = CAST(:clienteId AS UUID)
        """,
        nativeQuery = true)
    Page<OrdemServico> findByOficinaIdAndClienteId(@Param("oficinaId") UUID oficinaId, @Param("clienteId") UUID clienteId, Pageable pageable);

    /**
     * Busca avançada com múltiplos filtros opcionais em uma oficina.
     * Usa COALESCE para evitar problemas com inferência de tipos do PostgreSQL.
     *
     * @param oficinaId ID da oficina (tenant)
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
        WHERE os.oficina.id = :oficinaId
        AND (:status IS NULL OR CAST(os.status AS string) = :status)
        AND (:veiculoId IS NULL OR os.veiculoId = :veiculoId)
        AND (:usuarioId IS NULL OR os.usuarioId = :usuarioId)
        AND os.dataAbertura >= COALESCE(:dataInicio, CAST('1900-01-01 00:00:00' AS timestamp))
        AND os.dataAbertura <= COALESCE(:dataFim, CAST('9999-12-31 23:59:59' AS timestamp))
        ORDER BY os.dataAbertura DESC
        """)
    Page<OrdemServico> findByFiltros(
        @Param("oficinaId") UUID oficinaId,
        @Param("status") String status,
        @Param("veiculoId") UUID veiculoId,
        @Param("usuarioId") UUID usuarioId,
        @Param("dataInicio") LocalDateTime dataInicio,
        @Param("dataFim") LocalDateTime dataFim,
        Pageable pageable
    );

    /**
     * Conta OS agrupadas por status em uma oficina.
     * Útil para dashboard (KPIs).
     *
     * @param oficinaId ID da oficina (tenant)
     * @return mapa com status e quantidade
     */
    @Query("SELECT os.status, COUNT(os) FROM OrdemServico os WHERE os.oficina.id = :oficinaId GROUP BY os.status")
    List<Object[]> countByOficinaIdAndStatus(@Param("oficinaId") UUID oficinaId);

    /**
     * Conta OS em aberto (não finalizadas/entregues/canceladas) em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return quantidade de OS em aberto
     */
    @Query("SELECT COUNT(os) FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.status NOT IN ('FINALIZADO', 'ENTREGUE', 'CANCELADO')")
    long countOSEmAbertoByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Conta OS finalizadas no período em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return quantidade de OS finalizadas
     */
    @Query("SELECT COUNT(os) FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.status = 'FINALIZADO' AND os.dataFinalizacao BETWEEN :dataInicio AND :dataFim")
    long countOSFinalizadasByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("dataInicio") LocalDateTime dataInicio, @Param("dataFim") LocalDateTime dataFim);

    /**
     * Calcula faturamento total no período em uma oficina.
     * Considera apenas OS entregues (status = ENTREGUE).
     *
     * @param oficinaId ID da oficina (tenant)
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return valor total faturado
     */
    @Query("SELECT COALESCE(SUM(os.valorFinal), 0) FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.status = 'ENTREGUE' AND os.dataEntrega BETWEEN :dataInicio AND :dataFim")
    BigDecimal calcularFaturamentoByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("dataInicio") LocalDateTime dataInicio, @Param("dataFim") LocalDateTime dataFim);

    /**
     * Calcula ticket médio das OS no período em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return ticket médio
     */
    @Query("SELECT COALESCE(AVG(os.valorFinal), 0) FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.status = 'ENTREGUE' AND os.dataEntrega BETWEEN :dataInicio AND :dataFim")
    BigDecimal calcularTicketMedioByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("dataInicio") LocalDateTime dataInicio, @Param("dataFim") LocalDateTime dataFim);

    /**
     * Calcula valor total pendente (OS abertas) em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return valor total em OS abertas
     */
    @Query("SELECT COALESCE(SUM(os.valorFinal), 0) FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.status NOT IN ('ENTREGUE', 'CANCELADO')")
    BigDecimal calcularValorPendenteByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Busca OS com valor final acima de determinado limite em uma oficina.
     * Útil para identificar OS de alto valor.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param valorMinimo valor mínimo
     * @param pageable configuração de paginação e ordenação
     * @return página de OS com valor alto
     */
    @Query("SELECT os FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.valorFinal >= :valorMinimo ORDER BY os.valorFinal DESC")
    Page<OrdemServico> findByOficinaIdAndValorFinalGreaterThanEqual(@Param("oficinaId") UUID oficinaId, @Param("valorMinimo") BigDecimal valorMinimo, Pageable pageable);

    /**
     * Busca OS atrasadas (data de previsão vencida e não finalizadas) em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param dataReferencia data para comparação (geralmente hoje)
     * @param pageable configuração de paginação e ordenação
     * @return página de OS atrasadas
     */
    @Query("SELECT os FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.dataPrevisao < :dataReferencia AND os.status IN ('APROVADO', 'EM_ANDAMENTO', 'AGUARDANDO_PECA') ORDER BY os.dataPrevisao ASC")
    Page<OrdemServico> findOSAtrasadasByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("dataReferencia") LocalDate dataReferencia, Pageable pageable);

    /**
     * Busca OS aguardando peças há mais de X dias em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param dataLimite data limite (X dias atrás)
     * @param pageable configuração de paginação e ordenação
     * @return página de OS paradas há muito tempo
     */
    @Query("SELECT os FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.status = 'AGUARDANDO_PECA' AND os.updatedAt < :dataLimite ORDER BY os.updatedAt ASC")
    Page<OrdemServico> findOSAguardandoPecaAntigaByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("dataLimite") LocalDateTime dataLimite, Pageable pageable);

    /**
     * Busca OS mais antigas ainda em aberto em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable configuração de paginação e ordenação
     * @return página de OS antigas em aberto
     */
    @Query("SELECT os FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.status NOT IN ('FINALIZADO', 'ENTREGUE', 'CANCELADO') ORDER BY os.dataAbertura ASC")
    Page<OrdemServico> findOSMaisAntigasEmAbertoByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Ranking de mecânicos por quantidade de OS finalizadas no período em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return lista de [usuarioId, quantidade]
     */
    @Query("SELECT os.usuarioId, COUNT(os) FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.status = 'ENTREGUE' AND os.dataEntrega BETWEEN :dataInicio AND :dataFim GROUP BY os.usuarioId ORDER BY COUNT(os) DESC")
    List<Object[]> rankingMecanicosPorQuantidadeByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("dataInicio") LocalDateTime dataInicio, @Param("dataFim") LocalDateTime dataFim);

    /**
     * Ranking de mecânicos por faturamento no período em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return lista de [usuarioId, valorTotal]
     */
    @Query("SELECT os.usuarioId, SUM(os.valorFinal) FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.status = 'ENTREGUE' AND os.dataEntrega BETWEEN :dataInicio AND :dataFim GROUP BY os.usuarioId ORDER BY SUM(os.valorFinal) DESC")
    List<Object[]> rankingMecanicosPorFaturamentoByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("dataInicio") LocalDateTime dataInicio, @Param("dataFim") LocalDateTime dataFim);

    /**
     * Busca últimas OS de um veículo (histórico) em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param veiculoId ID do veículo
     * @param pageable configuração de paginação e ordenação
     * @return página de OS do veículo ordenadas por data decrescente
     */
    @Query("SELECT os FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.veiculoId = :veiculoId ORDER BY os.dataAbertura DESC")
    Page<OrdemServico> findHistoricoVeiculoByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("veiculoId") UUID veiculoId, Pageable pageable);

    // ========== QUERIES PARA DASHBOARD ==========

    /**
     * Conta OS ativas (não canceladas nem entregues) em uma oficina.
     * Útil para dashboard - mostra OS em progresso.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return quantidade de OS ativas
     */
    @Query("SELECT COUNT(os) FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.status NOT IN ('CANCELADO', 'ENTREGUE')")
    long countOSAtivasByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Calcula faturamento do mês atual em uma oficina.
     * Considera apenas OS entregues (status = ENTREGUE) no mês corrente.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return valor total faturado no mês atual
     */
    @Query("""
        SELECT COALESCE(SUM(os.valorFinal), 0)
        FROM OrdemServico os
        WHERE os.oficina.id = :oficinaId
        AND os.status = 'ENTREGUE'
        AND YEAR(os.dataEntrega) = YEAR(CURRENT_DATE)
        AND MONTH(os.dataEntrega) = MONTH(CURRENT_DATE)
        """)
    BigDecimal calcularFaturamentoMesAtualByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Busca OS recentes com dados de cliente e veículo em uma oficina.
     * Usa native query com JOINs para performance otimizada.
     *
     * @param oficinaId ID da oficina (tenant)
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
        WHERE os.oficina_id = CAST(:oficinaId AS UUID)
        ORDER BY os.data_abertura DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findRecentOSByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("limit") int limit);

    /**
     * Calcula faturamento mensal dos últimos N meses em uma oficina.
     * Retorna array com [ano, mês, valor] ordenado do mais antigo para o mais recente.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param meses quantidade de meses para buscar
     * @return lista de arrays [ano, mês, valorTotal]
     */
    @Query(value = """
        SELECT
            EXTRACT(YEAR FROM os.data_entrega) AS ano,
            EXTRACT(MONTH FROM os.data_entrega) AS mes,
            COALESCE(SUM(os.valor_final), 0) AS valor_total
        FROM ordem_servico os
        WHERE os.oficina_id = CAST(:oficinaId AS UUID)
        AND os.status = 'ENTREGUE'
        AND os.data_entrega >= CURRENT_DATE - CAST(:meses || ' months' AS INTERVAL)
        GROUP BY EXTRACT(YEAR FROM os.data_entrega), EXTRACT(MONTH FROM os.data_entrega)
        ORDER BY ano, mes
        """, nativeQuery = true)
    List<Object[]> calcularFaturamentoMensalByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("meses") int meses);

    /**
     * Busca ordem de serviço por ID em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param id ID da ordem de serviço
     * @return Optional contendo a OS se encontrada
     */
    @Query("SELECT os FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.id = :id")
    Optional<OrdemServico> findByOficinaIdAndId(@Param("oficinaId") UUID oficinaId, @Param("id") UUID id);

    /**
     * Busca todas as ordens de serviço de uma oficina com paginação.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable configuração de paginação e ordenação
     * @return página de ordens de serviço
     */
    @Query("SELECT os FROM OrdemServico os WHERE os.oficina.id = :oficinaId")
    Page<OrdemServico> findByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Conta ordens de serviço com status na lista fornecida em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param statusList lista de status
     * @return quantidade de OS com os status fornecidos
     */
    @Query("SELECT COUNT(os) FROM OrdemServico os WHERE os.oficina.id = :oficinaId AND os.status IN :statusList")
    long countOSByStatusIn(@Param("oficinaId") UUID oficinaId, @Param("statusList") List<StatusOS> statusList);

    /**
     * Alias para findHistoricoVeiculoByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @param veiculoId ID do veículo
     * @param pageable paginação
     * @return página de OS
     */
    default Page<OrdemServico> findHistoricoVeiculo(UUID oficinaId, UUID veiculoId, Pageable pageable) {
        return findHistoricoVeiculoByOficinaId(oficinaId, veiculoId, pageable);
    }

    /**
     * Alias para countByOficinaIdAndStatus (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de contadores por status
     */
    default List<Object[]> countByStatus(UUID oficinaId) {
        return countByOficinaIdAndStatus(oficinaId);
    }

    /**
     * Alias para calcularFaturamentoByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return valor do faturamento
     */
    default BigDecimal calcularFaturamento(UUID oficinaId, LocalDateTime dataInicio, LocalDateTime dataFim) {
        return calcularFaturamentoByOficinaId(oficinaId, dataInicio, dataFim);
    }

    /**
     * Alias para calcularTicketMedioByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return ticket médio
     */
    default BigDecimal calcularTicketMedio(UUID oficinaId, LocalDateTime dataInicio, LocalDateTime dataFim) {
        return calcularTicketMedioByOficinaId(oficinaId, dataInicio, dataFim);
    }

    /**
     * Alias para countOSAtivasByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @return quantidade de OS ativas
     */
    default long countOSAtivas(UUID oficinaId) {
        return countOSAtivasByOficinaId(oficinaId);
    }

    /**
     * Alias para calcularFaturamentoMesAtualByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @return valor do faturamento do mês atual
     */
    default BigDecimal calcularFaturamentoMesAtual(UUID oficinaId) {
        return calcularFaturamentoMesAtualByOficinaId(oficinaId);
    }

    /**
     * Alias para findRecentOSByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @param limit limite de resultados
     * @return lista de OS recentes
     */
    default List<Object[]> findRecentOS(UUID oficinaId, int limit) {
        return findRecentOSByOficinaId(oficinaId, limit);
    }

    /**
     * Alias para calcularFaturamentoMensalByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @param meses quantidade de meses
     * @return lista de faturamento mensal
     */
    default List<Object[]> calcularFaturamentoMensal(UUID oficinaId, int meses) {
        return calcularFaturamentoMensalByOficinaId(oficinaId, meses);
    }

    /**
     * Busca OS por token de aprovação (para aprovação pública pelo cliente).
     * Este método é usado sem verificação de tenant pois é acesso público.
     *
     * @param tokenAprovacao token único de aprovação
     * @return Optional contendo a OS se encontrada
     */
    @Query("SELECT os FROM OrdemServico os WHERE os.tokenAprovacao = :tokenAprovacao")
    Optional<OrdemServico> findByTokenAprovacao(@Param("tokenAprovacao") String tokenAprovacao);
}
