package com.pitstop.dashboard.service;

import com.pitstop.cliente.repository.ClienteRepository;
import com.pitstop.dashboard.dto.*;
import com.pitstop.estoque.repository.PecaRepository;
import com.pitstop.financeiro.domain.TipoPagamento;
import com.pitstop.financeiro.repository.NotaFiscalRepository;
import com.pitstop.financeiro.repository.PagamentoRepository;
import com.pitstop.manutencaopreventiva.domain.PlanoManutencaoPreventiva;
import com.pitstop.manutencaopreventiva.repository.AlertaManutencaoRepository;
import com.pitstop.manutencaopreventiva.repository.PlanoManutencaoRepository;
import com.pitstop.ordemservico.domain.StatusOS;
import com.pitstop.ordemservico.repository.OrdemServicoRepository;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.veiculo.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service para agregação de dados do dashboard.
 * Consolida informações de múltiplos módulos (Clientes, Veículos, Ordens de Serviço,
 * Pagamentos, Manutenção Preventiva, Estoque, Notas Fiscais).
 *
 * @author PitStop Team
 * @version 2.0
 * @since 2025-11-11
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final PecaRepository pecaRepository;
    private final PlanoManutencaoRepository planoManutencaoRepository;
    private final AlertaManutencaoRepository alertaManutencaoRepository;
    private final NotaFiscalRepository notaFiscalRepository;

    // Cores para gráfico de pagamentos por tipo
    private static final Map<TipoPagamento, String> TIPO_PAGAMENTO_COLORS = Map.of(
            TipoPagamento.PIX, "#3b82f6",           // Azul
            TipoPagamento.DINHEIRO, "#22c55e",      // Verde
            TipoPagamento.CARTAO_CREDITO, "#f59e0b", // Amarelo
            TipoPagamento.CARTAO_DEBITO, "#8b5cf6",  // Roxo
            TipoPagamento.BOLETO, "#ec4899",         // Rosa
            TipoPagamento.TRANSFERENCIA, "#06b6d4", // Ciano
            TipoPagamento.CHEQUE, "#6b7280"         // Cinza
    );

    private static final Map<TipoPagamento, String> TIPO_PAGAMENTO_LABELS = Map.of(
            TipoPagamento.PIX, "PIX",
            TipoPagamento.DINHEIRO, "Dinheiro",
            TipoPagamento.CARTAO_CREDITO, "Cartão Crédito",
            TipoPagamento.CARTAO_DEBITO, "Cartão Débito",
            TipoPagamento.BOLETO, "Boleto",
            TipoPagamento.TRANSFERENCIA, "Transferência",
            TipoPagamento.CHEQUE, "Cheque"
    );

    /**
     * Obtém estatísticas gerais do dashboard.
     * Agrega dados de clientes, veículos e ordens de serviço.
     *
     * @return estatísticas consolidadas
     */
    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats() {

        UUID oficinaId = TenantContext.getTenantId();

        // Conta clientes ativos da oficina (devido ao @Where na entidade, count() retorna apenas ativos)
        Long totalClientes = clienteRepository.countByOficinaId(oficinaId);

        // Conta todos os veículos da oficina
        Long totalVeiculos = veiculoRepository.countByOficinaId(oficinaId);

        // Conta OS ativas (não canceladas nem entregues)
        Long osAtivas = ordemServicoRepository.countOSAtivas(oficinaId);

        // Calcula faturamento do mês atual
        BigDecimal faturamentoMes = ordemServicoRepository.calcularFaturamentoMesAtual(oficinaId);

        log.info("Stats calculadas - Clientes: {}, Veículos: {}, OS Ativas: {}, Faturamento Mês: R$ {}",
                totalClientes, totalVeiculos, osAtivas, faturamentoMes);

        return new DashboardStatsDTO(
                totalClientes,
                totalVeiculos,
                osAtivas,
                faturamentoMes
        );
    }

    /**
     * Obtém estatísticas com variação percentual vs mês anterior.
     *
     * @return estatísticas com trends
     */
    @Transactional(readOnly = true)
    public DashboardStatsComTrendDTO getDashboardStatsComTrend() {
        UUID oficinaId = TenantContext.getTenantId();

        // Stats básicos
        Long totalClientes = clienteRepository.countByOficinaId(oficinaId);
        Long totalVeiculos = veiculoRepository.countByOficinaId(oficinaId);
        Long osAtivas = ordemServicoRepository.countOSAtivas(oficinaId);

        // Faturamento mês atual
        BigDecimal faturamentoMes = ordemServicoRepository.calcularFaturamentoMesAtual(oficinaId);

        // Faturamento mês anterior
        LocalDate inicioMesAnterior = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate fimMesAnterior = LocalDate.now().withDayOfMonth(1).minusDays(1);
        BigDecimal faturamentoMesAnterior = pagamentoRepository.sumRecebidoNoPeriodo(
                oficinaId, inicioMesAnterior, fimMesAnterior
        );

        // Calcular variação do faturamento
        Double variacaoFaturamento = calcularVariacaoPercentual(faturamentoMes, faturamentoMesAnterior);

        // Ticket médio (faturamento / quantidade de OS entregues no mês)
        // Simplificado: usar o faturamento / OS ativas como aproximação
        BigDecimal ticketMedio = osAtivas > 0 ?
                faturamentoMes.divide(BigDecimal.valueOf(osAtivas), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Ticket médio mês anterior (simplificado)
        BigDecimal ticketMedioAnterior = BigDecimal.ZERO;
        Double variacaoTicketMedio = 0.0;

        log.info("Stats com trend - Faturamento: R$ {} ({}%), Ticket Médio: R$ {}",
                faturamentoMes, variacaoFaturamento, ticketMedio);

        return new DashboardStatsComTrendDTO(
                totalClientes,
                totalVeiculos,
                osAtivas,
                faturamentoMes,
                faturamentoMesAnterior,
                variacaoFaturamento,
                ticketMedio,
                ticketMedioAnterior,
                variacaoTicketMedio
        );
    }

    /**
     * Obtém alertas dinâmicos que requerem atenção.
     *
     * @return alertas do dashboard
     */
    @Transactional(readOnly = true)
    public DashboardAlertasDTO getAlertas() {
        UUID oficinaId = TenantContext.getTenantId();
        LocalDate hoje = LocalDate.now();

        // Pagamentos vencidos
        long pagamentosVencidos = pagamentoRepository.countVencidos(oficinaId, hoje);

        // Alertas de manutenção pendentes (atrasadas)
        long manutencoesAtrasadas = alertaManutencaoRepository.countPendentes(oficinaId);

        // Peças críticas (estoque zerado)
        long pecasCriticas = pecaRepository.countEstoqueZerado(oficinaId);

        // Planos de manutenção ativos
        long planosAtivos = planoManutencaoRepository.countAtivos(oficinaId);

        log.info("Alertas - Pagamentos vencidos: {}, Manutenções atrasadas: {}, Peças críticas: {}, Planos ativos: {}",
                pagamentosVencidos, manutencoesAtrasadas, pecasCriticas, planosAtivos);

        return new DashboardAlertasDTO(
                pagamentosVencidos,
                manutencoesAtrasadas,
                pecasCriticas,
                planosAtivos
        );
    }

    /**
     * Obtém resumo de pagamentos para o widget expansível.
     *
     * @return resumo de pagamentos
     */
    @Transactional(readOnly = true)
    public PagamentosResumoDTO getPagamentosResumo() {
        UUID oficinaId = TenantContext.getTenantId();
        LocalDate hoje = LocalDate.now();

        // Total recebido no mês
        BigDecimal recebidoMes = pagamentoRepository.sumRecebidoNoMes(oficinaId);

        // Pagamentos pendentes
        long pendentesCount = pagamentoRepository.countPendentes(oficinaId);
        BigDecimal pendentesValor = pagamentoRepository.sumPendentes(oficinaId);

        // Pagamentos vencidos
        long vencidosCount = pagamentoRepository.countVencidos(oficinaId, hoje);
        BigDecimal vencidosValor = pagamentoRepository.sumVencidos(oficinaId, hoje);

        // Pagamentos por tipo (para gráfico)
        List<PagamentoPorTipoDTO> porTipo = getPagamentosPorTipo();

        // Lista de vencidos (últimos 5)
        List<PagamentoVencidoDTO> vencidosLista = getVencidosLista(5);

        log.info("Pagamentos resumo - Recebido: R$ {}, Pendentes: {}, Vencidos: {}",
                recebidoMes, pendentesCount, vencidosCount);

        return new PagamentosResumoDTO(
                recebidoMes != null ? recebidoMes : BigDecimal.ZERO,
                pendentesCount,
                pendentesValor != null ? pendentesValor : BigDecimal.ZERO,
                vencidosCount,
                vencidosValor != null ? vencidosValor : BigDecimal.ZERO,
                porTipo,
                vencidosLista
        );
    }

    /**
     * Obtém pagamentos agrupados por tipo para gráfico.
     *
     * @return lista de pagamentos por tipo
     */
    @Transactional(readOnly = true)
    public List<PagamentoPorTipoDTO> getPagamentosPorTipo() {
        UUID oficinaId = TenantContext.getTenantId();

        List<Object[]> resultados = pagamentoRepository.estatisticasPorTipoNoMes(oficinaId);
        List<PagamentoPorTipoDTO> porTipo = new ArrayList<>();

        for (Object[] row : resultados) {
            try {
                TipoPagamento tipo = (TipoPagamento) row[0];
                Long quantidade = ((Number) row[1]).longValue();
                BigDecimal valorTotal = (BigDecimal) row[2];

                porTipo.add(new PagamentoPorTipoDTO(
                        tipo,
                        TIPO_PAGAMENTO_LABELS.getOrDefault(tipo, tipo.name()),
                        quantidade,
                        valorTotal,
                        TIPO_PAGAMENTO_COLORS.getOrDefault(tipo, "#6b7280")
                ));
            } catch (Exception e) {
                log.error("Erro ao mapear pagamento por tipo: {}", e.getMessage(), e);
            }
        }

        return porTipo;
    }

    /**
     * Obtém lista de pagamentos vencidos com informações do cliente.
     *
     * @param limite quantidade máxima de resultados
     * @return lista de pagamentos vencidos
     */
    @Transactional(readOnly = true)
    public List<PagamentoVencidoDTO> getVencidosLista(int limite) {
        UUID oficinaId = TenantContext.getTenantId();
        LocalDate hoje = LocalDate.now();

        List<Object[]> resultados = pagamentoRepository.findVencidosComCliente(oficinaId, hoje, limite);
        List<PagamentoVencidoDTO> vencidos = new ArrayList<>();

        for (Object[] row : resultados) {
            try {
                UUID id = (UUID) row[0];
                String clienteNome = (String) row[1];
                BigDecimal valor = (BigDecimal) row[2];
                LocalDate dataVencimento = ((java.sql.Date) row[3]).toLocalDate();
                long diasVencido = ChronoUnit.DAYS.between(dataVencimento, hoje);

                vencidos.add(new PagamentoVencidoDTO(
                        id,
                        clienteNome,
                        valor,
                        dataVencimento,
                        diasVencido
                ));
            } catch (Exception e) {
                log.error("Erro ao mapear pagamento vencido: {}", e.getMessage(), e);
            }
        }

        return vencidos;
    }

    /**
     * Obtém resumo de manutenção preventiva para o widget expansível.
     *
     * @return resumo de manutenção
     */
    @Transactional(readOnly = true)
    public ManutencaoResumoDTO getManutencaoResumo() {
        UUID oficinaId = TenantContext.getTenantId();
        LocalDate hoje = LocalDate.now();

        // Planos ativos
        long planosAtivos = planoManutencaoRepository.countAtivos(oficinaId);

        // Alertas pendentes
        long alertasPendentes = alertaManutencaoRepository.countPendentes(oficinaId);

        // Planos vencidos
        long planosVencidos = planoManutencaoRepository.countVencidos(oficinaId, hoje);

        // Próximas manutenções (7 dias)
        List<ProximaManutencaoDTO> proximasManutencoes = getProximasManutencoes(7, 5);

        log.info("Manutenção resumo - Planos ativos: {}, Alertas: {}, Vencidos: {}",
                planosAtivos, alertasPendentes, planosVencidos);

        return new ManutencaoResumoDTO(
                planosAtivos,
                alertasPendentes,
                planosVencidos,
                proximasManutencoes
        );
    }

    /**
     * Obtém lista das próximas manutenções.
     *
     * @param dias quantidade de dias à frente
     * @param limite quantidade máxima de resultados
     * @return lista de próximas manutenções
     */
    @Transactional(readOnly = true)
    public List<ProximaManutencaoDTO> getProximasManutencoes(int dias, int limite) {
        UUID oficinaId = TenantContext.getTenantId();
        LocalDate hoje = LocalDate.now();
        LocalDate dataLimite = hoje.plusDays(dias);

        List<PlanoManutencaoPreventiva> planos = planoManutencaoRepository
                .findPlanosProximosAVencerPorData(oficinaId, hoje, dataLimite);

        List<ProximaManutencaoDTO> proximas = new ArrayList<>();

        for (PlanoManutencaoPreventiva plano : planos) {
            if (proximas.size() >= limite) break;

            try {
                var veiculo = plano.getVeiculo();

                // Busca o nome do cliente via repository (Veiculo tem apenas clienteId)
                String clienteNome = "N/A";
                if (veiculo != null && veiculo.getClienteId() != null) {
                    clienteNome = clienteRepository.findById(veiculo.getClienteId())
                            .map(c -> c.getNome())
                            .orElse("N/A");
                }

                long diasRestantes = plano.getProximaPrevisaoData() != null ?
                        ChronoUnit.DAYS.between(hoje, plano.getProximaPrevisaoData()) : 0;

                proximas.add(new ProximaManutencaoDTO(
                        plano.getId(),
                        plano.getNome(),
                        plano.getTipoManutencao(),
                        veiculo != null ? veiculo.getId() : null,
                        veiculo != null ? veiculo.getPlaca() : "N/A",
                        veiculo != null ? (veiculo.getMarca() + " " + veiculo.getModelo()) : "N/A",
                        clienteNome,
                        plano.getProximaPrevisaoData(),
                        plano.getProximaPrevisaoKm(),
                        diasRestantes
                ));
            } catch (Exception e) {
                log.error("Erro ao mapear próxima manutenção: {}", e.getMessage(), e);
            }
        }

        return proximas;
    }

    /**
     * Obtém resumo de notas fiscais para o widget expansível.
     *
     * @return resumo de notas fiscais
     */
    @Transactional(readOnly = true)
    public NotasFiscaisResumoDTO getNotasFiscaisResumo() {
        UUID oficinaId = TenantContext.getTenantId();

        // Emitidas no mês
        long emitidasMes = notaFiscalRepository.countEmitidasNoMes(oficinaId);

        // Rascunhos (pendentes)
        long rascunhosCount = notaFiscalRepository.countRascunhos(oficinaId);

        // Canceladas no mês
        long canceladasMes = notaFiscalRepository.countCanceladasNoMes(oficinaId);

        log.info("Notas fiscais resumo - Emitidas: {}, Rascunhos: {}, Canceladas: {}",
                emitidasMes, rascunhosCount, canceladasMes);

        return new NotasFiscaisResumoDTO(
                emitidasMes,
                rascunhosCount,
                canceladasMes
        );
    }

    /**
     * Busca as ordens de serviço mais recentes.
     * Inclui informações de cliente e veículo para exibição rápida.
     *
     * @param limit quantidade máxima de resultados (padrão 10)
     * @return lista de OS recentes simplificadas
     */
    @Transactional(readOnly = true)
    public List<RecentOSDTO> getRecentOS(int limit) {

        UUID oficinaId = TenantContext.getTenantId();

        // Validação do limit
        if (limit < 1 || limit > 50) {
            log.warn("Limit {} inválido, usando padrão 10", limit);
            limit = 10;
        }

        List<Object[]> resultados = ordemServicoRepository.findRecentOS(oficinaId, limit);
        List<RecentOSDTO> recentOS = new ArrayList<>();

        for (Object[] row : resultados) {
            try {
                UUID id = (UUID) row[0];
                Long numero = ((Number) row[1]).longValue();
                String statusStr = (String) row[2];
                StatusOS status = StatusOS.valueOf(statusStr);
                String clienteNome = (String) row[3];
                String veiculoPlaca = (String) row[4];
                LocalDateTime dataAbertura = ((Timestamp) row[5]).toLocalDateTime();
                BigDecimal valorFinal = (BigDecimal) row[6];

                recentOS.add(new RecentOSDTO(
                        id,
                        numero,
                        status,
                        clienteNome,
                        veiculoPlaca,
                        dataAbertura,
                        valorFinal
                ));
            } catch (Exception e) {
                log.error("Erro ao mapear OS recente: {}", e.getMessage(), e);
            }
        }

        log.info("Retornando {} OS recentes", recentOS.size());
        return recentOS;
    }

    /**
     * Busca contagem de OS agrupadas por status.
     * Retorna dados formatados para exibição em gráficos.
     *
     * @return lista de contagens por status com cores e labels
     */
    @Transactional(readOnly = true)
    public List<OSStatusCountDTO> getOSByStatus() {

        UUID oficinaId = TenantContext.getTenantId();

        List<Object[]> resultados = ordemServicoRepository.countByStatus(oficinaId);
        List<OSStatusCountDTO> statusCounts = new ArrayList<>();

        // Mapear cores e labels para cada status
        Map<StatusOS, String> statusLabels = Map.of(
                StatusOS.ORCAMENTO, "Orçamento",
                StatusOS.APROVADO, "Aprovado",
                StatusOS.EM_ANDAMENTO, "Em Andamento",
                StatusOS.AGUARDANDO_PECA, "Aguardando Peça",
                StatusOS.FINALIZADO, "Finalizado",
                StatusOS.ENTREGUE, "Entregue",
                StatusOS.CANCELADO, "Cancelado"
        );

        Map<StatusOS, String> statusColors = Map.of(
                StatusOS.ORCAMENTO, "#3b82f6",      // Azul
                StatusOS.APROVADO, "#8b5cf6",       // Roxo
                StatusOS.EM_ANDAMENTO, "#f59e0b",   // Laranja/Amarelo
                StatusOS.AGUARDANDO_PECA, "#ec4899", // Rosa/Pink
                StatusOS.FINALIZADO, "#10b981",     // Verde
                StatusOS.ENTREGUE, "#06b6d4",       // Ciano/Azul claro
                StatusOS.CANCELADO, "#ef4444"       // Vermelho
        );

        for (Object[] row : resultados) {
            try {
                StatusOS status = (StatusOS) row[0];
                Long count = ((Number) row[1]).longValue();

                statusCounts.add(new OSStatusCountDTO(
                        status,
                        count,
                        statusLabels.get(status),
                        statusColors.get(status)
                ));
            } catch (Exception e) {
                log.error("Erro ao mapear contagem de OS por status: {}", e.getMessage(), e);
            }
        }

        log.info("Retornando contagem de {} status", statusCounts.size());
        return statusCounts;
    }

    /**
     * Busca faturamento mensal dos últimos N meses.
     * Retorna dados formatados para exibição em gráficos de linha/barra.
     *
     * @param meses quantidade de meses para buscar (padrão 6)
     * @return lista de faturamento mensal ordenado
     */
    @Transactional(readOnly = true)
    public List<FaturamentoMensalDTO> getFaturamentoMensal(int meses) {

        UUID oficinaId = TenantContext.getTenantId();

        // Validação do parâmetro
        if (meses < 1 || meses > 24) {
            log.warn("Meses {} inválido, usando padrão 6", meses);
            meses = 6;
        }

        List<Object[]> resultados = ordemServicoRepository.calcularFaturamentoMensal(oficinaId, meses);
        List<FaturamentoMensalDTO> faturamentoList = new ArrayList<>();

        // Criar locale PT-BR para nomes dos meses
        Locale localeBR = new Locale("pt", "BR");

        for (Object[] row : resultados) {
            try {
                int ano = ((Number) row[0]).intValue();
                int mes = ((Number) row[1]).intValue();
                BigDecimal valor = (BigDecimal) row[2];

                // Formatar como "Nov/2025"
                String mesNome = java.time.Month.of(mes)
                        .getDisplayName(TextStyle.SHORT, localeBR);
                String mesFormatado = mesNome.substring(0, 1).toUpperCase() +
                                      mesNome.substring(1).toLowerCase() +
                                      "/" + ano;

                faturamentoList.add(new FaturamentoMensalDTO(mesFormatado, valor));
            } catch (Exception e) {
                log.error("Erro ao mapear faturamento mensal: {}", e.getMessage(), e);
            }
        }

        log.info("Retornando faturamento de {} meses", faturamentoList.size());
        return faturamentoList;
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Calcula variação percentual entre dois valores.
     *
     * @param atual valor atual
     * @param anterior valor anterior
     * @return variação percentual (positivo ou negativo)
     */
    private Double calcularVariacaoPercentual(BigDecimal atual, BigDecimal anterior) {
        if (anterior == null || anterior.compareTo(BigDecimal.ZERO) == 0) {
            return atual != null && atual.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        if (atual == null) {
            return -100.0;
        }

        BigDecimal diferenca = atual.subtract(anterior);
        BigDecimal variacao = diferenca.divide(anterior, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return variacao.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
