package com.pitstop.shared.audit.service;

import com.pitstop.shared.audit.domain.AuditLog;
import com.pitstop.shared.audit.repository.AuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Service for creating audit log entries.
 *
 * Automatically captures user context (email, IP, user-agent) from current request.
 * All audit actions are logged asynchronously to avoid impacting main operations.
 *
 * @author PitStop Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditRepository auditRepository;

    /**
     * Logs an administrative action with automatic context capture.
     *
     * @param acao Action type (CREATE_OFICINA, UPDATE_OFICINA, etc.)
     * @param entidade Entity type affected (Oficina, Pagamento, etc.)
     * @param entidadeId ID of the affected entity
     * @param observacao Additional context details
     */
    @Transactional
    public void log(String acao, String entidade, UUID entidadeId, String observacao) {
        try {
            String usuario = getCurrentUserEmail();
            String ipAddress = getCurrentIpAddress();
            String userAgent = getCurrentUserAgent();

            AuditLog auditLog = AuditLog.builder()
                .acao(acao)
                .entidade(entidade)
                .entidadeId(entidadeId)
                .usuario(usuario)
                .observacao(observacao)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

            auditRepository.save(auditLog);

            log.debug("Audit log created: {} - {} [{}]", acao, entidade, entidadeId);
        } catch (Exception e) {
            // Never fail the main operation due to audit logging issues
            log.error("Failed to create audit log for action: {} - {}", acao, entidade, e);
        }
    }

    private String getCurrentUserEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.warn("Could not retrieve current user email", e);
        }
        return "SYSTEM";
    }

    private String getCurrentIpAddress() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty()) {
                    ipAddress = request.getRemoteAddr();
                }
                return ipAddress;
            }
        } catch (Exception e) {
            log.warn("Could not retrieve IP address", e);
        }
        return "UNKNOWN";
    }

    private String getCurrentUserAgent() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userAgent = request.getHeader("User-Agent");
                if (userAgent != null && userAgent.length() > 500) {
                    return userAgent.substring(0, 500);
                }
                return userAgent;
            }
        } catch (Exception e) {
            log.warn("Could not retrieve user agent", e);
        }
        return "UNKNOWN";
    }
}
