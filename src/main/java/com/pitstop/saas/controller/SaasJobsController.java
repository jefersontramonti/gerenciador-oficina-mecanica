package com.pitstop.saas.controller;

import com.pitstop.saas.scheduler.SaasScheduledJobs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for manually triggering scheduled jobs.
 *
 * Allows SUPER_ADMIN to manually execute scheduled jobs for testing
 * or immediate execution when needed.
 *
 * @author PitStop Team
 */
@RestController
@RequestMapping("/api/saas/jobs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class SaasJobsController {

    private final SaasScheduledJobs scheduledJobs;

    /**
     * POST /api/saas/jobs/suspend-overdue
     *
     * Manually triggers the job to suspend workshops with overdue payments.
     *
     * @return execution result (200 OK)
     */
    @PostMapping("/suspend-overdue")
    public ResponseEntity<Map<String, Object>> suspendOverdueWorkshops() {
        log.info("SUPER_ADMIN manually triggered: Suspend overdue workshops");

        try {
            scheduledJobs.suspendOverdueWorkshops();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Job executed successfully");
            response.put("job", "suspendOverdueWorkshops");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error executing job: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error executing job: " + e.getMessage());
            response.put("job", "suspendOverdueWorkshops");

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * POST /api/saas/jobs/alert-trials
     *
     * Manually triggers the job to alert trials expiring soon.
     *
     * @return execution result (200 OK)
     */
    @PostMapping("/alert-trials")
    public ResponseEntity<Map<String, Object>> alertTrialsExpiring() {
        log.info("SUPER_ADMIN manually triggered: Alert trials expiring");

        try {
            scheduledJobs.alertTrialsExpiring();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Job executed successfully");
            response.put("job", "alertTrialsExpiring");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error executing job: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error executing job: " + e.getMessage());
            response.put("job", "alertTrialsExpiring");

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * POST /api/saas/jobs/refresh-stats
     *
     * Manually triggers the job to refresh dashboard statistics.
     *
     * @return execution result (200 OK)
     */
    @PostMapping("/refresh-stats")
    public ResponseEntity<Map<String, Object>> refreshDashboardStats() {
        log.info("SUPER_ADMIN manually triggered: Refresh dashboard statistics");

        try {
            scheduledJobs.refreshDashboardStats();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Job executed successfully");
            response.put("job", "refreshDashboardStats");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error executing job: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error executing job: " + e.getMessage());
            response.put("job", "refreshDashboardStats");

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * POST /api/saas/jobs/daily-metrics
     *
     * Manually triggers the job to log daily metrics summary.
     *
     * @return execution result (200 OK)
     */
    @PostMapping("/daily-metrics")
    public ResponseEntity<Map<String, Object>> dailyMetricsSummary() {
        log.info("SUPER_ADMIN manually triggered: Daily metrics summary");

        try {
            scheduledJobs.dailyMetricsSummary();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Job executed successfully");
            response.put("job", "dailyMetricsSummary");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error executing job: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error executing job: " + e.getMessage());
            response.put("job", "dailyMetricsSummary");

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * POST /api/saas/jobs/run-all
     *
     * Manually triggers all scheduled jobs sequentially.
     *
     * @return execution result (200 OK)
     */
    @PostMapping("/run-all")
    public ResponseEntity<Map<String, Object>> runAllJobs() {
        log.info("SUPER_ADMIN manually triggered: Run all jobs");

        Map<String, Object> response = new HashMap<>();
        Map<String, Boolean> results = new HashMap<>();

        try {
            scheduledJobs.alertTrialsExpiring();
            results.put("alertTrialsExpiring", true);
        } catch (Exception e) {
            log.error("Error in alertTrialsExpiring: {}", e.getMessage());
            results.put("alertTrialsExpiring", false);
        }

        try {
            scheduledJobs.suspendOverdueWorkshops();
            results.put("suspendOverdueWorkshops", true);
        } catch (Exception e) {
            log.error("Error in suspendOverdueWorkshops: {}", e.getMessage());
            results.put("suspendOverdueWorkshops", false);
        }

        try {
            scheduledJobs.refreshDashboardStats();
            results.put("refreshDashboardStats", true);
        } catch (Exception e) {
            log.error("Error in refreshDashboardStats: {}", e.getMessage());
            results.put("refreshDashboardStats", false);
        }

        try {
            scheduledJobs.dailyMetricsSummary();
            results.put("dailyMetricsSummary", true);
        } catch (Exception e) {
            log.error("Error in dailyMetricsSummary: {}", e.getMessage());
            results.put("dailyMetricsSummary", false);
        }

        response.put("results", results);
        response.put("message", "All jobs executed");

        return ResponseEntity.ok(response);
    }
}
