package com.pitstop.saas.service;

import com.pitstop.saas.dto.AuditFilterRequest;
import com.pitstop.saas.dto.AuditLogResponse;
import com.pitstop.shared.audit.domain.AuditLog;
import com.pitstop.shared.audit.repository.AuditRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for SaaS audit log queries and reporting.
 *
 * Provides filtered access to audit logs with export capabilities.
 * Wraps the shared AuditService with SaaS-specific functionality.
 *
 * @author PitStop Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SaasAuditService {

    private final AuditRepository auditRepository;
    private final EntityManager entityManager;

    /**
     * Gets audit logs with optional filters.
     *
     * @param filter filter criteria (all fields optional)
     * @param pageable pagination parameters
     * @return paginated audit logs
     */
    public Page<AuditLogResponse> getAuditLogs(AuditFilterRequest filter, Pageable pageable) {
        log.debug("Fetching audit logs with filters: {}", filter);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditLog> query = cb.createQuery(AuditLog.class);
        Root<AuditLog> root = query.from(AuditLog.class);

        // Build predicates from filters
        List<Predicate> predicates = buildPredicates(filter, cb, root);

        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        // Order by timestamp descending (most recent first)
        query.orderBy(cb.desc(root.get("timestamp")));

        // Execute query with pagination
        List<AuditLog> results = entityManager.createQuery(query)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        // Count total results
        long total = countAuditLogs(filter, cb);

        // Convert to DTOs
        List<AuditLogResponse> dtos = results.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, total);
    }

    /**
     * Exports audit logs to CSV format.
     *
     * Returns CSV string with all filtered logs (no pagination).
     * WARNING: Use with caution on large datasets.
     *
     * @param filter filter criteria
     * @return CSV string with headers
     */
    public String exportAuditLogsToCsv(AuditFilterRequest filter) {
        log.info("Exporting audit logs to CSV with filters: {}", filter);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditLog> query = cb.createQuery(AuditLog.class);
        Root<AuditLog> root = query.from(AuditLog.class);

        // Build predicates
        List<Predicate> predicates = buildPredicates(filter, cb, root);
        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        query.orderBy(cb.desc(root.get("timestamp")));

        // Fetch all results (limited to 10000 for safety)
        List<AuditLog> logs = entityManager.createQuery(query)
            .setMaxResults(10000)
            .getResultList();

        // Build CSV
        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,Acao,Entidade,EntidadeId,Usuario,IP,Detalhes\n");

        for (AuditLog log : logs) {
            csv.append(escapeCsv(log.getTimestamp().toString())).append(",");
            csv.append(escapeCsv(log.getAcao())).append(",");
            csv.append(escapeCsv(log.getEntidade())).append(",");
            csv.append(escapeCsv(log.getEntidadeId() != null ? log.getEntidadeId().toString() : "")).append(",");
            csv.append(escapeCsv(log.getUsuario())).append(",");
            csv.append(escapeCsv(log.getIpAddress())).append(",");
            csv.append(escapeCsv(log.getObservacao())).append("\n");
        }

        log.info("Exported {} audit log entries to CSV", logs.size());
        return csv.toString();
    }

    /**
     * Gets count of audit logs matching filters.
     *
     * @param filter filter criteria
     * @return total count
     */
    public long countAuditLogs(AuditFilterRequest filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        return countAuditLogs(filter, cb);
    }

    /**
     * Builds JPA predicates from filter request.
     */
    private List<Predicate> buildPredicates(
        AuditFilterRequest filter,
        CriteriaBuilder cb,
        Root<AuditLog> root
    ) {
        List<Predicate> predicates = new ArrayList<>();

        if (filter.acao() != null && !filter.acao().isBlank()) {
            predicates.add(cb.like(
                cb.lower(root.get("acao")),
                "%" + filter.acao().toLowerCase() + "%"
            ));
        }

        if (filter.entidade() != null && !filter.entidade().isBlank()) {
            predicates.add(cb.equal(root.get("entidade"), filter.entidade()));
        }

        if (filter.entidadeId() != null) {
            predicates.add(cb.equal(root.get("entidadeId"), filter.entidadeId()));
        }

        if (filter.usuarioEmail() != null && !filter.usuarioEmail().isBlank()) {
            predicates.add(cb.like(
                cb.lower(root.get("usuarioEmail")),
                "%" + filter.usuarioEmail().toLowerCase() + "%"
            ));
        }

        if (filter.dataInicio() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), filter.dataInicio()));
        }

        if (filter.dataFim() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), filter.dataFim()));
        }

        if (filter.ipAddress() != null && !filter.ipAddress().isBlank()) {
            predicates.add(cb.equal(root.get("ipAddress"), filter.ipAddress()));
        }

        return predicates;
    }

    /**
     * Counts audit logs with filters.
     */
    private long countAuditLogs(AuditFilterRequest filter, CriteriaBuilder cb) {
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<AuditLog> root = countQuery.from(AuditLog.class);

        List<Predicate> predicates = buildPredicates(filter, cb, root);

        countQuery.select(cb.count(root));
        if (!predicates.isEmpty()) {
            countQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    /**
     * Converts AuditLog entity to response DTO.
     */
    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
            log.getId(),
            log.getAcao(),
            log.getEntidade(),
            log.getEntidadeId(),
            log.getUsuario(),
            log.getObservacao(),
            log.getIpAddress(),
            log.getUserAgent(),
            log.getTimestamp()
        );
    }

    /**
     * Escapes CSV special characters.
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        // If contains comma, quote, or newline - wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }
}
