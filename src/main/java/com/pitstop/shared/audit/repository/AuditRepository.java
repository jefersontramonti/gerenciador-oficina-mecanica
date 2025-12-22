package com.pitstop.shared.audit.repository;

import com.pitstop.shared.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for audit log persistence.
 *
 * @author PitStop Team
 */
@Repository
public interface AuditRepository extends JpaRepository<AuditLog, UUID> {
    // Custom queries are defined in SaasAuditService using Criteria API
}
