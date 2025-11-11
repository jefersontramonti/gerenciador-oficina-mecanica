package com.pitstop.shared.security;

import com.pitstop.usuario.domain.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a password reset token.
 *
 * <p>Tokens are single-use and expire after 15 minutes.
 * After successful password reset or expiration, tokens should be deleted.
 *
 * <p><b>Security considerations:</b>
 * <ul>
 *   <li>Tokens are random UUIDs (cryptographically secure)</li>
 *   <li>15-minute expiration to limit attack window</li>
 *   <li>Single-use: deleted after successful reset</li>
 *   <li>Associated with specific user email</li>
 * </ul>
 */
@Entity
@Table(name = "password_reset_tokens", indexes = {
        @Index(name = "idx_password_reset_token", columnList = "token", unique = true),
        @Index(name = "idx_password_reset_usuario", columnList = "usuario_id"),
        @Index(name = "idx_password_reset_expires", columnList = "expires_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Random UUID token sent to user's email.
     * Single-use and expires in 15 minutes.
     */
    @Column(name = "token", nullable = false, unique = true, length = 36)
    private String token;

    /**
     * User who requested the password reset.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Timestamp when token expires (15 minutes after creation).
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Whether the token has been used.
     * Tokens are single-use.
     */
    @Builder.Default
    @Column(name = "used", nullable = false)
    private Boolean used = false;

    /**
     * Timestamp when token was created.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Checks if the token is expired.
     *
     * @return true if token is expired, false otherwise
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * Checks if the token is valid (not used and not expired).
     *
     * @return true if token is valid, false otherwise
     */
    public boolean isValid() {
        return !Boolean.TRUE.equals(this.used) && !isExpired();
    }

    /**
     * Marks the token as used.
     */
    public void markAsUsed() {
        this.used = true;
    }
}
