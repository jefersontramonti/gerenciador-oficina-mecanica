package com.pitstop.saas.service;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.domain.PlanoAssinatura;
import com.pitstop.oficina.domain.StatusOficina;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.saas.dto.DashboardStatsResponse;
import com.pitstop.saas.dto.MRRBreakdownResponse;
import com.pitstop.saas.dto.OficinaResumoDTO;
import com.pitstop.saas.repository.SaasPagamentoRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for SaaS dashboard statistics and metrics.
 *
 * Provides aggregated data for platform monitoring including MRR,
 * workshop counts, trial tracking, and usage statistics.
 *
 * @author PitStop Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SaasDashboardService {

    private final OficinaRepository oficinaRepository;
    private final SaasPagamentoRepository pagamentoRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    /**
     * Gets overall platform statistics from materialized view.
     *
     * Uses the optimized vw_saas_dashboard_stats view for performance.
     *
     * @return dashboard statistics
     */
    public DashboardStatsResponse getDashboardStats() {
        log.debug("Fetching SaaS dashboard statistics");

        // Query the materialized view
        String sql = "SELECT * FROM vw_saas_dashboard_stats";

        Map<String, Object> stats = jdbcTemplate.queryForMap(sql);

        return new DashboardStatsResponse(
            getLong(stats, "total_oficinas"),
            getLong(stats, "oficinas_ativas"),
            getLong(stats, "oficinas_trial"),
            getLong(stats, "oficinas_suspensas"),
            getLong(stats, "oficinas_canceladas"),
            getBigDecimal(stats, "mrr_total"),
            getLong(stats, "total_ordens_servico"),
            getLong(stats, "total_clientes"),
            getLong(stats, "total_veiculos"),
            getLong(stats, "pagamentos_pendentes"),
            getLong(stats, "pagamentos_atrasados")
        );
    }

    /**
     * Calculates Monthly Recurring Revenue breakdown by plan.
     *
     * @return list of MRR metrics grouped by subscription plan
     */
    public List<MRRBreakdownResponse> getMRRBreakdown() {
        log.debug("Calculating MRR breakdown by plan");

        // Get total MRR for percentage calculation
        BigDecimal mrrTotal = oficinaRepository.findAll().stream()
            .filter(o -> o.getStatus() == StatusOficina.ATIVA)
            .map(Oficina::getValorMensalidade)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group by plan and calculate MRR
        return List.of(PlanoAssinatura.values()).stream()
            .map(plano -> {
                List<BigDecimal> mensalidades = oficinaRepository.findByStatusAndPlano(
                    StatusOficina.ATIVA, plano
                ).stream()
                    .map(Oficina::getValorMensalidade)
                    .toList();

                long quantidade = mensalidades.size();
                BigDecimal mrrPlano = mensalidades.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal percentual = mrrTotal.compareTo(BigDecimal.ZERO) > 0
                    ? mrrPlano.divide(mrrTotal, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

                return new MRRBreakdownResponse(plano, quantidade, mrrPlano, percentual);
            })
            .filter(breakdown -> breakdown.quantidadeOficinas() > 0)
            .toList();
    }

    /**
     * Gets workshops with trials expiring in the next 7 days.
     *
     * Used for proactive outreach to convert trial users to paid customers.
     * TODO: Use dataFimTrial when field is added to Oficina
     *
     * @param pageable pagination parameters
     * @return paginated list of workshops with expiring trials
     */
    public Page<OficinaResumoDTO> getTrialsExpiring(Pageable pageable) {
        log.debug("Fetching workshops with trials expiring in next 7 days");

        LocalDate hoje = LocalDate.now();
        LocalDate limite = hoje.plusDays(7);

        return oficinaRepository.findByStatusAndDataVencimentoPlanoBetween(
            StatusOficina.TRIAL, hoje, limite, pageable
        ).map(oficina -> {
            long diasRestantes = ChronoUnit.DAYS.between(hoje, oficina.getDataVencimentoPlano());

            return new OficinaResumoDTO(
                oficina.getId(),
                oficina.getNomeFantasia(),
                oficina.getCnpjCpf(),
                oficina.getStatus(),
                oficina.getPlano(),
                oficina.getValorMensalidade(),
                oficina.getDataVencimentoPlano(),
                (int) diasRestantes,
                countUsuarios(oficina.getId()),
                countOrdensServico(oficina.getId()),
                countClientes(oficina.getId()),
                oficina.getCreatedAt()
            );
        });
    }

    /**
     * Gets summary information for all workshops.
     *
     * @param pageable pagination parameters
     * @return paginated list of workshop summaries
     */
    public Page<OficinaResumoDTO> getAllOficinas(Pageable pageable) {
        log.debug("Fetching all workshops summary");

        LocalDate hoje = LocalDate.now();

        return oficinaRepository.findAll(pageable)
            .map(oficina -> {
                Integer diasRestantes = null;
                if (oficina.getStatus() == StatusOficina.TRIAL && oficina.getDataVencimentoPlano() != null) {
                    diasRestantes = (int) ChronoUnit.DAYS.between(hoje, oficina.getDataVencimentoPlano());
                }

                return new OficinaResumoDTO(
                    oficina.getId(),
                    oficina.getNomeFantasia(),
                    oficina.getCnpjCpf(),
                    oficina.getStatus(),
                    oficina.getPlano(),
                    oficina.getValorMensalidade(),
                    oficina.getDataVencimentoPlano(),
                    diasRestantes,
                    countUsuarios(oficina.getId()),
                    countOrdensServico(oficina.getId()),
                    countClientes(oficina.getId()),
                    oficina.getCreatedAt()
                );
            });
    }

    /**
     * Gets workshops filtered by status.
     *
     * @param status filter by subscription status
     * @param pageable pagination parameters
     * @return paginated list of workshops
     */
    public Page<OficinaResumoDTO> getOficinasByStatus(StatusOficina status, Pageable pageable) {
        log.debug("Fetching workshops with status: {}", status);

        LocalDate hoje = LocalDate.now();

        return oficinaRepository.findByStatus(status, pageable)
            .map(oficina -> {
                Integer diasRestantes = null;
                if (status == StatusOficina.TRIAL && oficina.getDataVencimentoPlano() != null) {
                    diasRestantes = (int) ChronoUnit.DAYS.between(hoje, oficina.getDataVencimentoPlano());
                }

                return new OficinaResumoDTO(
                    oficina.getId(),
                    oficina.getNomeFantasia(),
                    oficina.getCnpjCpf(),
                    oficina.getStatus(),
                    oficina.getPlano(),
                    oficina.getValorMensalidade(),
                    oficina.getDataVencimentoPlano(),
                    diasRestantes,
                    countUsuarios(oficina.getId()),
                    countOrdensServico(oficina.getId()),
                    countClientes(oficina.getId()),
                    oficina.getCreatedAt()
                );
            });
    }

    // Helper methods for counting related entities
    private Long countUsuarios(UUID oficinaId) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE oficina_id = ?",
                Long.class,
                oficinaId
            );
        } catch (Exception e) {
            // Table may not exist yet during initial setup
            log.trace("Could not count usuarios for oficina {}: {}", oficinaId, e.getMessage());
            return 0L;
        }
    }

    private Long countOrdensServico(UUID oficinaId) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ordem_servico WHERE oficina_id = ?",
                Long.class,
                oficinaId
            );
        } catch (Exception e) {
            // Table may not exist yet during initial setup
            log.trace("Could not count ordem_servico for oficina {}: {}", oficinaId, e.getMessage());
            return 0L;
        }
    }

    private Long countClientes(UUID oficinaId) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clientes WHERE oficina_id = ?",
                Long.class,
                oficinaId
            );
        } catch (Exception e) {
            // Table may not exist yet during initial setup
            log.trace("Could not count clientes for oficina {}: {}", oficinaId, e.getMessage());
            return 0L;
        }
    }

    // Safe type conversion helpers
    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0L;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return BigDecimal.ZERO;
    }
}
