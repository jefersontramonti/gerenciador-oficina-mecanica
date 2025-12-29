package com.pitstop.saas.controller;

import com.pitstop.oficina.domain.StatusOficina;
import com.pitstop.saas.dto.*;
import com.pitstop.saas.service.SaasDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for SaaS dashboard metrics and statistics.
 *
 * Provides endpoints for platform-wide monitoring and reporting.
 * All endpoints require SUPER_ADMIN role.
 *
 * @author PitStop Team
 */
@RestController
@RequestMapping("/api/saas/dashboard")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class SaasDashboardController {

    private final SaasDashboardService dashboardService;

    /**
     * GET /api/saas/dashboard/stats
     *
     * Returns overall platform statistics from materialized view.
     * Includes workshop counts by status, MRR, usage metrics, and payment stats.
     *
     * @return dashboard statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        log.info("SUPER_ADMIN requested dashboard statistics");
        DashboardStatsResponse stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/saas/dashboard/mrr
     *
     * Returns Monthly Recurring Revenue breakdown by subscription plan.
     * Useful for financial analysis and revenue forecasting.
     *
     * @return list of MRR metrics per plan
     */
    @GetMapping("/mrr")
    public ResponseEntity<List<MRRBreakdownResponse>> getMRRBreakdown() {
        log.info("SUPER_ADMIN requested MRR breakdown");
        List<MRRBreakdownResponse> breakdown = dashboardService.getMRRBreakdown();
        return ResponseEntity.ok(breakdown);
    }

    /**
     * GET /api/saas/dashboard/trials-expiring
     *
     * Returns workshops with trial periods expiring in the next 7 days.
     * Enables proactive outreach for conversion to paid subscriptions.
     *
     * @param pageable pagination parameters
     * @return paginated list of workshops with expiring trials
     */
    @GetMapping("/trials-expiring")
    public ResponseEntity<Page<OficinaResumoDTO>> getTrialsExpiring(
        @PageableDefault(size = 20, sort = "dataVencimentoPlano", direction = Sort.Direction.ASC)
        Pageable pageable
    ) {
        log.info("SUPER_ADMIN requested workshops with expiring trials");
        Page<OficinaResumoDTO> workshops = dashboardService.getTrialsExpiring(pageable);
        return ResponseEntity.ok(workshops);
    }

    /**
     * GET /api/saas/dashboard/oficinas
     *
     * Returns summary of all workshops with optional status filter.
     *
     * @param status optional status filter
     * @param pageable pagination parameters
     * @return paginated workshop summaries
     */
    @GetMapping("/oficinas")
    public ResponseEntity<Page<OficinaResumoDTO>> getAllOficinas(
        @RequestParam(required = false) StatusOficina status,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        log.info("SUPER_ADMIN requested all workshops (status filter: {})", status);

        Page<OficinaResumoDTO> workshops = status != null
            ? dashboardService.getOficinasByStatus(status, pageable)
            : dashboardService.getAllOficinas(pageable);

        return ResponseEntity.ok(workshops);
    }

    // ===== ADVANCED METRICS ENDPOINTS =====

    /**
     * GET /api/saas/dashboard/metrics
     *
     * Returns comprehensive dashboard metrics including financial KPIs.
     * Includes MRR, ARR, churn rate, LTV, CAC, and growth indicators.
     *
     * @return advanced dashboard metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<DashboardMetricsDTO> getAdvancedMetrics() {
        log.info("SUPER_ADMIN requested advanced dashboard metrics");
        DashboardMetricsDTO metrics = dashboardService.getAdvancedMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/saas/dashboard/mrr-evolution
     *
     * Returns MRR evolution data for the specified number of months.
     * Useful for trend analysis and revenue forecasting.
     *
     * @param months number of months to include (default: 12)
     * @return MRR evolution data for charts
     */
    @GetMapping("/mrr-evolution")
    public ResponseEntity<MRREvolutionDTO> getMRREvolution(
        @RequestParam(defaultValue = "12") int months
    ) {
        log.info("SUPER_ADMIN requested MRR evolution for {} months", months);

        // Limit to reasonable range
        months = Math.min(Math.max(months, 1), 24);

        MRREvolutionDTO evolution = dashboardService.getMRREvolution(months);
        return ResponseEntity.ok(evolution);
    }

    /**
     * GET /api/saas/dashboard/churn-evolution
     *
     * Returns churn rate evolution data for the specified number of months.
     * Helps identify retention trends and potential issues.
     *
     * @param months number of months to include (default: 12)
     * @return churn evolution data for charts
     */
    @GetMapping("/churn-evolution")
    public ResponseEntity<ChurnEvolutionDTO> getChurnEvolution(
        @RequestParam(defaultValue = "12") int months
    ) {
        log.info("SUPER_ADMIN requested churn evolution for {} months", months);

        // Limit to reasonable range
        months = Math.min(Math.max(months, 1), 24);

        ChurnEvolutionDTO evolution = dashboardService.getChurnEvolution(months);
        return ResponseEntity.ok(evolution);
    }

    /**
     * GET /api/saas/dashboard/signups-vs-cancellations
     *
     * Returns comparison of new signups vs cancellations per month.
     * Visualizes net growth trends over time.
     *
     * @param months number of months to include (default: 12)
     * @return signups vs cancellations data for charts
     */
    @GetMapping("/signups-vs-cancellations")
    public ResponseEntity<SignupsVsCancellationsDTO> getSignupsVsCancellations(
        @RequestParam(defaultValue = "12") int months
    ) {
        log.info("SUPER_ADMIN requested signups vs cancellations for {} months", months);

        // Limit to reasonable range
        months = Math.min(Math.max(months, 1), 24);

        SignupsVsCancellationsDTO data = dashboardService.getSignupsVsCancellations(months);
        return ResponseEntity.ok(data);
    }
}
