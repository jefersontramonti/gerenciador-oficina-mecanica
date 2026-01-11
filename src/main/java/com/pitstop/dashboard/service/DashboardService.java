package com.pitstop.dashboard.service;

import com.pitstop.cliente.repository.ClienteRepository;
import com.pitstop.dashboard.dto.DashboardStatsDTO;
import com.pitstop.dashboard.dto.FaturamentoMensalDTO;
import com.pitstop.dashboard.dto.OSStatusCountDTO;
import com.pitstop.dashboard.dto.RecentOSDTO;
import com.pitstop.ordemservico.domain.StatusOS;
import com.pitstop.ordemservico.repository.OrdemServicoRepository;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.veiculo.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Service para agregação de dados do dashboard.
 * Consolida informações de múltiplos módulos (Clientes, Veículos, Ordens de Serviço).
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final OrdemServicoRepository ordemServicoRepository;

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
}
