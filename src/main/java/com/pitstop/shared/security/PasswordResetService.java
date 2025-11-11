package com.pitstop.shared.security;

import com.pitstop.shared.dto.ForgotPasswordRequest;
import com.pitstop.shared.dto.ResetPasswordRequest;
import com.pitstop.shared.service.EmailService;
import com.pitstop.usuario.domain.Usuario;
import com.pitstop.usuario.exception.InvalidCredentialsException;
import com.pitstop.usuario.exception.UsuarioNotFoundException;
import com.pitstop.usuario.repository.UsuarioRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.UnsupportedEncodingException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service responsible for password reset operations.
 *
 * <p><b>Forgot password flow:</b>
 * <ol>
 *   <li>User requests password reset with email</li>
 *   <li>System generates unique token (UUID)</li>
 *   <li>Token is stored in database with 15-minute expiration</li>
 *   <li>Email is sent with reset link (logged to console in dev)</li>
 *   <li>User clicks link and provides new password</li>
 *   <li>System validates token and updates password</li>
 *   <li>Token is marked as used and deleted</li>
 * </ol>
 *
 * <p><b>Security features:</b>
 * <ul>
 *   <li>Tokens expire after 15 minutes</li>
 *   <li>Tokens are single-use</li>
 *   <li>No user enumeration: always returns success even if email doesn't exist</li>
 *   <li>Previous tokens invalidated when new one is requested</li>
 *   <li>All tokens invalidated after successful password reset</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Token validity duration in minutes.
     */
    private static final long TOKEN_EXPIRATION_MINUTES = 15;

    /**
     * Initiates password reset process by generating and "sending" a reset token.
     *
     * <p><b>Security note:</b> Always returns success to prevent user enumeration.
     * If email doesn't exist, no token is generated but response is the same.
     *
     * @param request forgot password request containing email
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.debug("Password reset requested for email: {}", request.email());

        // Find user by email (but don't reveal if it doesn't exist)
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElse(null);

        if (usuario == null) {
            log.warn("Password reset requested for non-existent email: {}", request.email());
            // Return success to prevent user enumeration
            return;
        }

        if (!usuario.isAtivo()) {
            log.warn("Password reset requested for inactive user: {}", request.email());
            // Return success to prevent revealing user status
            return;
        }

        // Invalidate any existing tokens for this user
        tokenRepository.deleteByUsuarioId(usuario.getId());

        // Generate new token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_MINUTES);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .usuario(usuario)
                .expiresAt(expiresAt)
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        // Send password reset email
        try {
            emailService.sendPasswordResetEmail(usuario.getEmail(), usuario.getNome(), token);
            log.info("Password reset email sent to: {}", usuario.getEmail());
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send password reset email to: {}", usuario.getEmail(), e);
            // Note: We don't throw the exception to prevent user enumeration
            // The user will see success message even if email fails to send
        }
    }

    /**
     * Resets user password using a valid reset token.
     *
     * @param request reset password request containing token and new password
     * @throws InvalidCredentialsException if token is invalid, expired, or already used
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.debug("Password reset attempt with token: {}", request.token());

        // Find token
        PasswordResetToken resetToken = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> {
                    log.warn("Password reset failed: invalid token - {}", request.token());
                    return new InvalidCredentialsException("Token inválido ou expirado");
                });

        // Validate token
        if (!resetToken.isValid()) {
            log.warn("Password reset failed: token expired or already used - {}", request.token());
            throw new InvalidCredentialsException("Token inválido ou expirado");
        }

        // Get user
        Usuario usuario = resetToken.getUsuario();

        // Update password
        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(usuario);

        // Mark token as used
        resetToken.markAsUsed();
        tokenRepository.save(resetToken);

        // Invalidate all tokens for this user (security measure)
        tokenRepository.deleteByUsuarioId(usuario.getId());

        log.info("Password reset successful for user: {} (ID: {})", usuario.getEmail(), usuario.getId());
    }

    /**
     * Cleans up expired tokens from database.
     *
     * <p>Should be called periodically (e.g., via scheduled task).
     *
     * @return number of deleted tokens
     */
    @Transactional
    public int cleanupExpiredTokens() {
        log.debug("Cleaning up expired password reset tokens");
        int deleted = tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Deleted {} expired password reset tokens", deleted);
        return deleted;
    }
}
