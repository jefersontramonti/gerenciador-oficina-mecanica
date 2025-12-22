package com.pitstop.saas.controller;

import com.pitstop.saas.dto.AuditFilterRequest;
import com.pitstop.saas.dto.AuditLogResponse;
import com.pitstop.saas.service.SaasAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for SaaS audit log queries and exports.
 *
 * Provides filtered access to administrative action logs with CSV export.
 * All endpoints require SUPER_ADMIN role.
 *
 * @author PitStop Team
 */
@RestController
@RequestMapping("/api/saas/audit")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class SaasAuditController {

    private final SaasAuditService auditService;

    /**
     * GET /api/saas/audit
     *
     * Returns audit logs with optional filters.
     * All filter parameters are optional and combined with AND logic.
     *
     * Query parameters:
     * - acao: action type (partial match)
     * - entidade: entity type (exact match)
     * - entidadeId: specific entity UUID
     * - usuarioEmail: user email (partial match)
     * - dataInicio: start of date range
     * - dataFim: end of date range
     * - ipAddress: IP address (exact match)
     *
     * @param filter filter criteria (query params bound to object)
     * @param pageable pagination parameters
     * @return paginated audit logs (200 OK)
     */
    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
        @ModelAttribute AuditFilterRequest filter,
        @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        log.info("SUPER_ADMIN querying audit logs with filters: {}", filter);
        Page<AuditLogResponse> logs = auditService.getAuditLogs(filter, pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * GET /api/saas/audit/export
     *
     * Exports audit logs to CSV format with applied filters.
     * Returns CSV file for download (limited to 10,000 records).
     *
     * Same filter parameters as the main GET endpoint.
     *
     * @param filter filter criteria
     * @return CSV file (200 OK)
     */
    @GetMapping("/export")
    public ResponseEntity<String> exportAuditLogs(
        @ModelAttribute AuditFilterRequest filter
    ) {
        log.info("SUPER_ADMIN exporting audit logs to CSV with filters: {}", filter);

        String csv = auditService.exportAuditLogsToCsv(filter);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "audit_logs.csv");

        return ResponseEntity.ok()
            .headers(headers)
            .body(csv);
    }

    /**
     * GET /api/saas/audit/count
     *
     * Returns count of audit logs matching filters.
     * Useful for displaying totals before pagination.
     *
     * @param filter filter criteria
     * @return count of matching logs (200 OK)
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countAuditLogs(
        @ModelAttribute AuditFilterRequest filter
    ) {
        log.debug("SUPER_ADMIN counting audit logs with filters: {}", filter);
        long count = auditService.countAuditLogs(filter);
        return ResponseEntity.ok(count);
    }
}
