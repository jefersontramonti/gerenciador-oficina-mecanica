package com.pitstop.saas.scheduler;

import com.pitstop.notificacao.domain.TemplateNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import com.pitstop.notificacao.dto.NotificacaoRequest;
import com.pitstop.notificacao.service.NotificacaoService;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.domain.StatusOficina;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.shared.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduled jobs for SaaS platform automation.
 *
 * Handles automatic suspension of overdue workshops, trial period management,
 * and dashboard statistics refresh.
 *
 * @author PitStop Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SaasScheduledJobs {

    private final OficinaRepository oficinaRepository;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;
    private final NotificacaoService notificacaoService;

    private static final int OVERDUE_GRACE_PERIOD_DAYS = 5;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Suspends workshops with overdue payments (past grace period).
     *
     * Runs daily at 2 AM.
     * Grace period: 5 days after due date.
     */
    @Scheduled(cron = "0 0 2 * * *") // Every day at 2 AM
    @Transactional
    public void suspendOverdueWorkshops() {
        log.info("Starting scheduled job: Suspend overdue workshops");

        LocalDate hoje = LocalDate.now();
        LocalDate limiteOverdue = hoje.minusDays(OVERDUE_GRACE_PERIOD_DAYS);

        // Find active workshops with due date before grace period
        List<Oficina> oficinasVencidas = oficinaRepository.findVencidas(limiteOverdue);

        int suspended = 0;
        for (Oficina oficina : oficinasVencidas) {
            if (oficina.getStatus() == StatusOficina.ATIVA) {
                log.warn("Suspending workshop {} (ID: {}) - overdue since {}",
                    oficina.getNomeFantasia(), oficina.getId(), oficina.getDataVencimentoPlano());

                oficina.setStatus(StatusOficina.SUSPENSA);
                oficina.setAtivo(false);
                oficinaRepository.save(oficina);

                // Audit log
                auditService.log(
                    "AUTO_SUSPEND_OFICINA",
                    "Oficina",
                    oficina.getId(),
                    String.format("Workshop automatically suspended due to overdue payment (due date: %s)",
                        oficina.getDataVencimentoPlano())
                );

                // Enviar notificação de suspensão
                enviarNotificacaoSuspensao(oficina);

                suspended++;
            }
        }

        log.info("Scheduled job completed: {} workshops suspended", suspended);
    }

    /**
     * Logs warnings for workshops with trials expiring soon.
     *
     * Runs daily at 9 AM.
     * Alerts for trials expiring in the next 3 days.
     */
    @Scheduled(cron = "0 0 9 * * *") // Every day at 9 AM
    public void alertTrialsExpiring() {
        log.info("Starting scheduled job: Alert trials expiring");

        LocalDate hoje = LocalDate.now();
        LocalDate limite = hoje.plusDays(3);

        List<Oficina> trialsExpiring = oficinaRepository.findByStatusAndDataVencimentoPlanoBetween(
            StatusOficina.TRIAL,
            hoje,
            limite,
            org.springframework.data.domain.PageRequest.of(0, 100)
        ).getContent();

        log.info("Found {} workshops with trials expiring in the next 3 days", trialsExpiring.size());

        for (Oficina oficina : trialsExpiring) {
            long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(hoje, oficina.getDataVencimentoPlano());

            log.warn("TRIAL EXPIRING: {} (ID: {}) - {} days remaining (expires: {})",
                oficina.getNomeFantasia(), oficina.getId(), diasRestantes, oficina.getDataVencimentoPlano());

            // Enviar notificação de trial expirando
            enviarNotificacaoTrialExpiring(oficina, diasRestantes);

            // Audit log
            auditService.log(
                "TRIAL_EXPIRING_ALERT",
                "Oficina",
                oficina.getId(),
                String.format("Trial expiring alert: %d days remaining", diasRestantes)
            );
        }

        log.info("Scheduled job completed: {} trial expiration alerts generated", trialsExpiring.size());
    }

    /**
     * Refreshes materialized view for dashboard statistics.
     *
     * Runs every hour.
     * Ensures dashboard shows up-to-date metrics.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void refreshDashboardStats() {
        log.info("Starting scheduled job: Refresh dashboard statistics");

        try {
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW vw_saas_dashboard_stats");
            log.info("Scheduled job completed: Dashboard statistics refreshed successfully");
        } catch (Exception e) {
            log.error("Error refreshing dashboard statistics: {}", e.getMessage(), e);
        }
    }

    /**
     * Logs daily summary of platform metrics.
     *
     * Runs daily at 11 PM.
     * Provides daily snapshot for monitoring.
     */
    @Scheduled(cron = "0 0 23 * * *") // Every day at 11 PM
    public void dailyMetricsSummary() {
        log.info("Starting scheduled job: Daily metrics summary");

        long totalOficinas = oficinaRepository.count();
        long oficinasAtivas = oficinaRepository.countByStatus(StatusOficina.ATIVA);
        long oficinasTrial = oficinaRepository.countByStatus(StatusOficina.TRIAL);
        long oficinasSuspensas = oficinaRepository.countByStatus(StatusOficina.SUSPENSA);
        long oficinasCanceladas = oficinaRepository.countByStatus(StatusOficina.CANCELADA);
        Double mrr = oficinaRepository.calculateMRR();

        log.info("=== DAILY SAAS METRICS ===");
        log.info("Total Workshops: {}", totalOficinas);
        log.info("Active: {} | Trial: {} | Suspended: {} | Cancelled: {}",
            oficinasAtivas, oficinasTrial, oficinasSuspensas, oficinasCanceladas);
        log.info("MRR: R$ {}", String.format("%.2f", mrr != null ? mrr : 0.0));
        log.info("==========================");

        // Audit log
        auditService.log(
            "DAILY_METRICS",
            "System",
            null,
            String.format("Daily metrics: Total=%d, Active=%d, Trial=%d, MRR=%.2f",
                totalOficinas, oficinasAtivas, oficinasTrial, mrr != null ? mrr : 0.0)
        );

        log.info("Scheduled job completed: Daily metrics summary logged");
    }

    /**
     * Envia notificação de trial expirando para a oficina.
     *
     * @param oficina Oficina com trial expirando
     * @param diasRestantes Dias restantes do trial
     */
    private void enviarNotificacaoTrialExpiring(Oficina oficina, long diasRestantes) {
        try {
            if (oficina.getContato() == null || oficina.getContato().getEmail() == null) {
                log.warn("Oficina {} não possui email cadastrado. Notificação não enviada.", oficina.getNomeFantasia());
                return;
            }

            Map<String, Object> variaveis = new HashMap<>();
            variaveis.put("nomeOficina", oficina.getNomeFantasia());
            variaveis.put("diasRestantes", diasRestantes);
            variaveis.put("dataVencimento", oficina.getDataVencimentoPlano().format(DATE_FORMATTER));
            variaveis.put("pagamentoUrl", "https://pitstop.com.br/pagamento"); // TODO: URL real
            variaveis.put("loginUrl", "https://pitstop.com.br/login"); // TODO: URL real

            NotificacaoRequest notificacao = NotificacaoRequest.emailComTemplate(
                oficina.getContato().getEmail(),
                TemplateNotificacao.TRIAL_EXPIRING,
                variaveis
            );

            notificacaoService.enviar(notificacao);

            log.info("Notificação de trial expirando enviada para: {}", oficina.getContato().getEmail());
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de trial expirando para oficina {}: {}",
                oficina.getNomeFantasia(), e.getMessage(), e);
        }
    }

    /**
     * Envia notificação de suspensão para a oficina.
     *
     * @param oficina Oficina suspensa
     */
    private void enviarNotificacaoSuspensao(Oficina oficina) {
        try {
            if (oficina.getContato() == null || oficina.getContato().getEmail() == null) {
                log.warn("Oficina {} não possui email cadastrado. Notificação não enviada.", oficina.getNomeFantasia());
                return;
            }

            long diasAtraso = java.time.temporal.ChronoUnit.DAYS.between(
                oficina.getDataVencimentoPlano(),
                LocalDate.now()
            );

            Map<String, Object> variaveis = new HashMap<>();
            variaveis.put("nomeOficina", oficina.getNomeFantasia());
            variaveis.put("nomePlano", oficina.getPlano().name());
            variaveis.put("valor", String.format("R$ %.2f", oficina.getValorMensalidade()));
            variaveis.put("dataVencimento", oficina.getDataVencimentoPlano().format(DATE_FORMATTER));
            variaveis.put("diasAtraso", diasAtraso);
            variaveis.put("pagamentoUrl", "https://pitstop.com.br/pagamento"); // TODO: URL real

            NotificacaoRequest notificacao = NotificacaoRequest.emailComTemplate(
                oficina.getContato().getEmail(),
                TemplateNotificacao.OFICINA_SUSPENDED,
                variaveis
            );

            notificacaoService.enviar(notificacao);

            log.info("Notificação de suspensão enviada para: {}", oficina.getContato().getEmail());
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de suspensão para oficina {}: {}",
                oficina.getNomeFantasia(), e.getMessage(), e);
        }
    }
}
