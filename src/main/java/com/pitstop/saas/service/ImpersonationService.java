package com.pitstop.saas.service;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.saas.dto.ImpersonateResponse;
import com.pitstop.shared.audit.service.AuditService;
import com.pitstop.shared.exception.ResourceNotFoundException;
import com.pitstop.usuario.domain.PerfilUsuario;
import com.pitstop.usuario.domain.Usuario;
import com.pitstop.usuario.repository.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

/**
 * Service for SUPER_ADMIN impersonation functionality.
 *
 * Allows SUPER_ADMIN to generate temporary access tokens to access
 * the system as if they were a user from a specific workshop.
 * This is essential for support and troubleshooting.
 *
 * Security:
 * - All impersonation actions are logged to audit trail
 * - Impersonation tokens have limited validity (1 hour)
 * - Token includes 'impersonated' flag for audit purposes
 * - Original SUPER_ADMIN identity is preserved in audit logs
 *
 * @author PitStop Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImpersonationService {

    private final OficinaRepository oficinaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;

    @Value("${application.jwt.secret}")
    private String jwtSecret;

    // Impersonation tokens are valid for 1 hour
    private static final long IMPERSONATION_TOKEN_VALIDITY = 60 * 60 * 1000L; // 1 hour

    /**
     * Generates an impersonation token for accessing a workshop as its admin.
     *
     * The generated token allows the SUPER_ADMIN to access the workshop's
     * dashboard and data as if they were the workshop's admin user.
     *
     * @param oficinaId the ID of the workshop to impersonate
     * @return impersonation response with token and redirect URL
     * @throws ResourceNotFoundException if workshop or admin user not found
     */
    @Transactional
    public ImpersonateResponse impersonate(UUID oficinaId) {
        log.warn("SUPER_ADMIN initiating impersonation for oficina: {}", oficinaId);

        // Get current SUPER_ADMIN identity for audit
        String superAdminEmail = getCurrentUserEmail();

        // Find the workshop
        Oficina oficina = oficinaRepository.findById(oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada"));

        // Find an active admin user for the workshop
        Usuario adminUser = usuarioRepository.findFirstByOficinaIdAndPerfilAndAtivoTrue(
                oficinaId,
                PerfilUsuario.ADMIN
            )
            .orElseThrow(() -> new ResourceNotFoundException(
                "Nenhum usuário administrador ativo encontrado para esta oficina"
            ));

        // Generate impersonation token
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        String token = generateImpersonationToken(adminUser, superAdminEmail, expiresAt);

        // Log the impersonation action
        auditService.log(
            "INICIAR_IMPERSONACAO",
            "Oficina",
            oficinaId,
            String.format(
                "SUPER_ADMIN %s iniciou sessão de impersonação na oficina %s (como usuário %s)",
                superAdminEmail,
                oficina.getNomeFantasia(),
                adminUser.getEmail()
            )
        );

        log.info(
            "Impersonation token generated for oficina {} (user: {}), expires at: {}",
            oficina.getNomeFantasia(),
            adminUser.getEmail(),
            expiresAt
        );

        return new ImpersonateResponse(
            token,
            "/", // Redirect to dashboard after impersonation
            expiresAt,
            oficinaId.toString(),
            oficina.getNomeFantasia(),
            adminUser.getEmail()
        );
    }

    /**
     * Generates a JWT token for impersonation.
     *
     * The token includes:
     * - User claims (id, email, perfil, oficinaId)
     * - Impersonation flag
     * - Original SUPER_ADMIN email for audit
     * - Limited validity (1 hour)
     */
    private String generateImpersonationToken(
        Usuario targetUser,
        String superAdminEmail,
        LocalDateTime expiresAt
    ) {
        Date expiration = java.sql.Timestamp.valueOf(expiresAt);

        return Jwts.builder()
            .subject(targetUser.getId().toString())
            .claim("email", targetUser.getEmail())
            .claim("perfil", targetUser.getPerfil().name())
            .claim("oficinaId", targetUser.getOficina().getId().toString())
            .claim("impersonated", true) // Flag indicating this is an impersonation session
            .claim("impersonatedBy", superAdminEmail) // Original SUPER_ADMIN for audit
            .issuedAt(new Date())
            .expiration(expiration)
            .signWith(getSigningKey(), Jwts.SIG.HS512)
            .compact();
    }

    /**
     * Gets the signing key for JWT.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Gets the current authenticated user's email.
     */
    private String getCurrentUserEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario usuario) {
            return usuario.getEmail();
        }
        return "unknown";
    }
}
