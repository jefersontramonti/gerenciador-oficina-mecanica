package com.pitstop.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base class for auditable entities.
 *
 * <p>Provides automatic tracking of creation and modification metadata:</p>
 * <ul>
 *   <li><b>createdAt</b>: Timestamp when entity was created</li>
 *   <li><b>updatedAt</b>: Timestamp of last update</li>
 *   <li><b>createdBy</b>: Email of user who created the entity</li>
 *   <li><b>updatedBy</b>: Email of user who last updated the entity</li>
 * </ul>
 *
 * <p><b>Configuration required:</b> Enable JPA Auditing with {@code @EnableJpaAuditing}
 * in your configuration class and provide an {@code AuditorAware<String>} bean.</p>
 *
 * @since 1.0.0
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class AuditableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Timestamp when the entity was created.
     * Automatically set by Spring Data JPA on persist.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the entity was last updated.
     * Automatically updated by Spring Data JPA on merge.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Email of the user who created this entity.
     * Automatically set from SecurityContext via AuditorAware bean.
     */
    @CreatedBy
    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    /**
     * Email of the user who last updated this entity.
     * Automatically updated from SecurityContext via AuditorAware bean.
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
