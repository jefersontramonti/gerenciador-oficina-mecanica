package com.pitstop.saas.service;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.domain.PlanoAssinatura;
import com.pitstop.oficina.domain.StatusOficina;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.saas.dto.*;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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
        ).map(oficina -> toResumoDTO(oficina, (int) ChronoUnit.DAYS.between(hoje, oficina.getDataVencimentoPlano())));
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
                return toResumoDTO(oficina, diasRestantes);
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
                return toResumoDTO(oficina, diasRestantes);
            });
    }

    /**
     * Converts Oficina entity to OficinaResumoDTO.
     */
    private OficinaResumoDTO toResumoDTO(Oficina oficina, Integer diasRestantes) {
        String email = oficina.getContato() != null ? oficina.getContato().getEmail() : null;
        String telefone = oficina.getContato() != null ? oficina.getContato().getTelefoneCelular() : null;

        return new OficinaResumoDTO(
            oficina.getId(),
            oficina.getNomeFantasia(),
            oficina.getRazaoSocial(),
            oficina.getCnpjCpf(),
            email,
            telefone,
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

    // ===== ADVANCED METRICS =====

    /**
     * Gets comprehensive dashboard metrics including financial KPIs.
     *
     * @return advanced metrics DTO
     */
    public DashboardMetricsDTO getAdvancedMetrics() {
        log.debug("Calculating advanced SaaS dashboard metrics");

        LocalDate hoje = LocalDate.now();
        LocalDateTime inicioMes = hoje.withDayOfMonth(1).atStartOfDay();
        LocalDateTime fimMes = hoje.plusMonths(1).withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicioMesAnterior = inicioMes.minusMonths(1);

        // MRR calculations
        BigDecimal mrrTotal = BigDecimal.valueOf(oficinaRepository.calculateMRR());
        BigDecimal mrrAnterior = BigDecimal.valueOf(
            Optional.ofNullable(oficinaRepository.calculateMRRAt(inicioMes)).orElse(0.0)
        );
        BigDecimal mrrGrowth = calculateGrowthPercentage(mrrAnterior, mrrTotal);
        BigDecimal arrTotal = mrrTotal.multiply(BigDecimal.valueOf(12));

        // Workshop counts
        int oficinasAtivas = (int) oficinaRepository.countByStatus(StatusOficina.ATIVA);
        int oficinasTrial = (int) oficinaRepository.countByStatus(StatusOficina.TRIAL);
        int oficinasInativas = (int) oficinaRepository.countByStatus(StatusOficina.INATIVA);
        int oficinasInadimplentes = (int) countInadimplentes();

        // Monthly changes
        Long novasOficinas30d = oficinaRepository.countCreatedBetween(inicioMes, fimMes);
        Long cancelamentos30d = oficinaRepository.countCancelledBetween(inicioMes, fimMes);

        // Churn rate (cancelamentos / ativos no início do mês * 100)
        Long ativasInicioMes = oficinaRepository.countActiveAt(inicioMes);
        BigDecimal churnRate = ativasInicioMes > 0
            ? BigDecimal.valueOf(cancelamentos30d)
                .divide(BigDecimal.valueOf(ativasInicioMes), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // LTV calculation (MRR médio * meses médios de permanência)
        // Simplified: assume average 12 months retention
        BigDecimal ltvMultiplier = BigDecimal.valueOf(12);
        BigDecimal avgMrrPerWorkshop = oficinasAtivas > 0
            ? mrrTotal.divide(BigDecimal.valueOf(oficinasAtivas), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal ltv = avgMrrPerWorkshop.multiply(ltvMultiplier);

        // CAC placeholder (would need marketing spend data)
        BigDecimal cac = BigDecimal.ZERO;

        // User metrics
        int usuariosAtivos = safeGetInt(oficinaRepository.countActiveUsers());
        int usuariosTotais = safeGetInt(oficinaRepository.countTotalUsers());
        int loginsMes = countLoginsMes();

        // General data
        long totalClientes = countTotalClientes();
        long totalVeiculos = countTotalVeiculos();
        long totalOS = countTotalOS();
        long totalOSMes = countOSMes(inicioMes, fimMes);
        BigDecimal faturamentoMes = calculateFaturamentoMes(inicioMes, fimMes);

        return new DashboardMetricsDTO(
            mrrTotal,
            mrrGrowth,
            arrTotal,
            churnRate,
            ltv,
            cac,
            oficinasAtivas,
            oficinasTrial,
            oficinasInativas,
            oficinasInadimplentes,
            novasOficinas30d.intValue(),
            cancelamentos30d.intValue(),
            usuariosAtivos,
            usuariosTotais,
            loginsMes,
            totalClientes,
            totalVeiculos,
            totalOS,
            totalOSMes,
            faturamentoMes
        );
    }

    /**
     * Gets MRR evolution over the specified number of months.
     *
     * @param months number of months to include
     * @return MRR evolution data
     */
    public MRREvolutionDTO getMRREvolution(int months) {
        log.debug("Calculating MRR evolution for {} months", months);

        List<MRREvolutionDTO.MonthlyMRRData> data = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        BigDecimal previousMrr = null;
        BigDecimal totalMrr = BigDecimal.ZERO;

        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM/yy", new Locale("pt", "BR"));

        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDateTime endOfMonth = month.atEndOfMonth().atTime(23, 59, 59);

            // For current month, use current MRR; for past months, estimate
            BigDecimal mrr;
            int oficinasAtivas;

            if (i == 0) {
                // Current month
                mrr = BigDecimal.valueOf(oficinaRepository.calculateMRR());
                oficinasAtivas = (int) oficinaRepository.countByStatus(StatusOficina.ATIVA);
            } else {
                // Historical data (approximation)
                Double historicalMrr = oficinaRepository.calculateMRRAt(endOfMonth);
                mrr = BigDecimal.valueOf(historicalMrr != null ? historicalMrr : 0);
                Long historicalActive = oficinaRepository.countActiveAt(endOfMonth);
                oficinasAtivas = historicalActive != null ? historicalActive.intValue() : 0;
            }

            BigDecimal growth = previousMrr != null
                ? calculateGrowthPercentage(previousMrr, mrr)
                : BigDecimal.ZERO;

            data.add(new MRREvolutionDTO.MonthlyMRRData(
                month.toString(),
                month.atDay(1).format(labelFormatter),
                mrr,
                growth,
                oficinasAtivas
            ));

            previousMrr = mrr;
            totalMrr = totalMrr.add(mrr);
        }

        // Calculate overall growth
        BigDecimal firstMrr = data.isEmpty() ? BigDecimal.ZERO : data.get(0).mrr();
        BigDecimal lastMrr = data.isEmpty() ? BigDecimal.ZERO : data.get(data.size() - 1).mrr();
        BigDecimal totalGrowth = calculateGrowthPercentage(firstMrr, lastMrr);

        // Average MRR
        BigDecimal averageMRR = data.isEmpty()
            ? BigDecimal.ZERO
            : totalMrr.divide(BigDecimal.valueOf(data.size()), 2, RoundingMode.HALF_UP);

        return new MRREvolutionDTO(data, totalGrowth, averageMRR);
    }

    /**
     * Gets churn rate evolution over the specified number of months.
     *
     * @param months number of months to include
     * @return churn evolution data
     */
    public ChurnEvolutionDTO getChurnEvolution(int months) {
        log.debug("Calculating churn evolution for {} months", months);

        List<ChurnEvolutionDTO.MonthlyChurnData> data = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        int totalCancelled = 0;
        BigDecimal totalChurn = BigDecimal.ZERO;

        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM/yy", new Locale("pt", "BR"));

        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDateTime startOfMonth = month.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = month.atEndOfMonth().atTime(23, 59, 59);

            Long cancelled = oficinaRepository.countCancelledBetween(startOfMonth, endOfMonth);
            Long activeAtStart = oficinaRepository.countActiveAt(startOfMonth);

            int cancelledInt = cancelled != null ? cancelled.intValue() : 0;
            int activeAtStartInt = activeAtStart != null ? activeAtStart.intValue() : 0;

            BigDecimal churnRate = activeAtStartInt > 0
                ? BigDecimal.valueOf(cancelledInt)
                    .divide(BigDecimal.valueOf(activeAtStartInt), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            data.add(new ChurnEvolutionDTO.MonthlyChurnData(
                month.toString(),
                month.atDay(1).format(labelFormatter),
                churnRate,
                cancelledInt,
                activeAtStartInt
            ));

            totalCancelled += cancelledInt;
            totalChurn = totalChurn.add(churnRate);
        }

        BigDecimal averageChurn = data.isEmpty()
            ? BigDecimal.ZERO
            : totalChurn.divide(BigDecimal.valueOf(data.size()), 2, RoundingMode.HALF_UP);

        BigDecimal currentChurn = data.isEmpty()
            ? BigDecimal.ZERO
            : data.get(data.size() - 1).churnRate();

        return new ChurnEvolutionDTO(data, averageChurn, currentChurn, totalCancelled);
    }

    /**
     * Gets signups vs cancellations comparison over the specified number of months.
     *
     * @param months number of months to include
     * @return signups vs cancellations data
     */
    public SignupsVsCancellationsDTO getSignupsVsCancellations(int months) {
        log.debug("Calculating signups vs cancellations for {} months", months);

        List<SignupsVsCancellationsDTO.MonthlySignupData> data = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        int totalSignups = 0;
        int totalCancellations = 0;

        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM/yy", new Locale("pt", "BR"));

        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDateTime startOfMonth = month.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = month.atEndOfMonth().atTime(23, 59, 59);
            LocalDate startDate = month.atDay(1);
            LocalDate endDate = month.atEndOfMonth();

            Long signups = oficinaRepository.countCreatedBetween(startOfMonth, endOfMonth);
            Long cancellations = oficinaRepository.countCancelledBetween(startOfMonth, endOfMonth);
            Long conversions = oficinaRepository.countTrialConversionsBetween(startDate, endDate);

            int signupsInt = signups != null ? signups.intValue() : 0;
            int cancellationsInt = cancellations != null ? cancellations.intValue() : 0;
            int conversionsInt = conversions != null ? conversions.intValue() : 0;

            data.add(new SignupsVsCancellationsDTO.MonthlySignupData(
                month.toString(),
                month.atDay(1).format(labelFormatter),
                signupsInt,
                cancellationsInt,
                signupsInt - cancellationsInt,
                conversionsInt
            ));

            totalSignups += signupsInt;
            totalCancellations += cancellationsInt;
        }

        return new SignupsVsCancellationsDTO(
            data,
            totalSignups,
            totalCancellations,
            totalSignups - totalCancellations
        );
    }

    // ===== HELPER METHODS =====

    private BigDecimal calculateGrowthPercentage(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0
                ? BigDecimal.valueOf(100)
                : BigDecimal.ZERO;
        }
        return current.subtract(previous)
            .divide(previous, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private long countInadimplentes() {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oficinas WHERE status = 'ATIVA' AND data_vencimento_plano < CURRENT_DATE",
                Long.class
            );
        } catch (Exception e) {
            return 0L;
        }
    }

    private int countLoginsMes() {
        // Placeholder - would need audit log integration
        return 0;
    }

    private long countTotalClientes() {
        try {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clientes", Long.class);
        } catch (Exception e) {
            return 0L;
        }
    }

    private long countTotalVeiculos() {
        try {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM veiculos", Long.class);
        } catch (Exception e) {
            return 0L;
        }
    }

    private long countTotalOS() {
        try {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ordem_servico", Long.class);
        } catch (Exception e) {
            return 0L;
        }
    }

    private long countOSMes(LocalDateTime inicio, LocalDateTime fim) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ordem_servico WHERE created_at >= ? AND created_at < ?",
                Long.class,
                inicio, fim
            );
        } catch (Exception e) {
            return 0L;
        }
    }

    private BigDecimal calculateFaturamentoMes(LocalDateTime inicio, LocalDateTime fim) {
        try {
            Double valor = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(valor_final), 0) FROM ordem_servico WHERE status = 'ENTREGUE' AND created_at >= ? AND created_at < ?",
                Double.class,
                inicio, fim
            );
            return valor != null ? BigDecimal.valueOf(valor) : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private int safeGetInt(Long value) {
        return value != null ? value.intValue() : 0;
    }
}
