package com.pitstop.shared.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for password reset token operations.
 *
 * <p>Provides methods to:
 * <ul>
 *   <li>Find tokens by token string</li>
 *   <li>Delete expired tokens (cleanup)</li>
 *   <li>Delete tokens by user (for security)</li>
 * </ul>
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Finds a password reset token by its token string.
     *
     * @param token the token string
     * @return optional containing the token if found
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Deletes all expired tokens.
     *
     * <p>Should be called periodically to clean up old tokens.
     *
     * @param now current timestamp
     * @return number of deleted tokens
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Deletes all tokens for a specific user.
     *
     * <p>Useful when:
     * <ul>
     *   <li>User successfully resets password (invalidate all pending tokens)</li>
     *   <li>User requests new reset (invalidate previous tokens)</li>
     * </ul>
     *
     * @param usuarioId the user ID
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") UUID usuarioId);
}
