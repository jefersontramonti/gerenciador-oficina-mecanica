package com.pitstop.shared.security;

import com.pitstop.usuario.domain.PerfilUsuario;
import com.pitstop.usuario.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

/**
 * Service responsible for JWT token generation, validation, and claims extraction.
 *
 * <p>Supports two types of tokens:
 * <ul>
 *   <li><b>Access Token</b>: Short-lived (15 minutes), contains user claims (userId, email, perfil, oficinaId)</li>
 *   <li><b>Refresh Token</b>: Long-lived (7 days), used to generate new access tokens</li>
 * </ul>
 *
 * <p><b>Security notes:</b>
 * <ul>
 *   <li>Uses HS512 (HMAC SHA-512) algorithm with 256-bit secret key</li>
 *   <li>Secret key must be Base64-encoded and stored in environment variable JWT_SECRET</li>
 *   <li>Tokens are stateless - validation is done via signature + expiration check</li>
 *   <li>Refresh tokens are stored in Redis for revocation capability</li>
 * </ul>
 *
 * <p><b>Multi-tenancy support:</b>
 * Access tokens include an "oficinaId" claim for row-level data isolation.
 * The TenantFilter extracts this claim and sets it in TenantContext for all repository queries.
 */
@Service
@Slf4j
public class JwtService {

    /**
     * Minimum secret key length in bytes for HS512 (512 bits = 64 bytes).
     */
    private static final int MIN_SECRET_LENGTH_BYTES = 64;

    private final String secret;
    private final Long accessTokenExpiration;
    private final Long refreshTokenExpiration;
    private final SecretKey signingKey;

    public JwtService(
            @Value("${application.jwt.secret}") String secret,
            @Value("${application.jwt.access-token-expiration}") Long accessTokenExpiration,
            @Value("${application.jwt.refresh-token-expiration}") Long refreshTokenExpiration
    ) {
        this.secret = secret;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;

        // Validate and initialize signing key
        this.signingKey = initializeSigningKey(secret);
    }

    /**
     * Initializes and validates the signing key.
     *
     * @param secret Base64-encoded secret
     * @return validated SecretKey
     * @throws IllegalArgumentException if secret is too short
     */
    private SecretKey initializeSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret não configurado. Configure 'application.jwt.secret' no application.yml");
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            throw new IllegalArgumentException("JWT secret deve ser uma string Base64 válida", e);
        }

        if (keyBytes.length < MIN_SECRET_LENGTH_BYTES) {
            log.error("JWT secret tem {} bytes, mas HS512 requer mínimo de {} bytes (512 bits)",
                    keyBytes.length, MIN_SECRET_LENGTH_BYTES);
            throw new IllegalArgumentException(
                    "JWT secret muito curto. HS512 requer mínimo de 64 bytes (512 bits). Atual: " + keyBytes.length + " bytes");
        }

        log.info("JWT secret validado com sucesso ({} bytes)", keyBytes.length);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates an access token for the given user.
     *
     * <p>Contains the following claims:
     * <ul>
     *   <li><b>subject</b>: userId (UUID as string)</li>
     *   <li><b>email</b>: user email</li>
     *   <li><b>perfil</b>: user role (SUPER_ADMIN, ADMIN, GERENTE, ATENDENTE, MECANICO)</li>
     *   <li><b>oficinaId</b>: oficina/tenant ID for multi-tenancy isolation (ONLY for non-SUPER_ADMIN users)</li>
     * </ul>
     *
     * <p><b>Multi-Tenancy vs SaaS:</b></p>
     * <ul>
     *   <li>SUPER_ADMIN: NO oficinaId claim (gerencia todas as oficinas via /api/saas/*)</li>
     *   <li>Outros perfis: oficinaId claim OBRIGATÓRIO (acesso isolado aos dados da oficina)</li>
     * </ul>
     *
     * @param usuario the authenticated user
     * @return JWT access token (15 minutes validity)
     */
    public String generateAccessToken(Usuario usuario) {
        // SUPER_ADMIN não tem oficinaId (gerencia o SaaS inteiro)
        if (usuario.getPerfil() == PerfilUsuario.SUPER_ADMIN) {
            log.debug("Generating SUPER_ADMIN access token for user: {}", usuario.getEmail());

            return Jwts.builder()
                    .subject(usuario.getId().toString())
                    .claim("email", usuario.getEmail())
                    .claim("perfil", usuario.getPerfil().name())
                    // NÃO adiciona oficinaId para SUPER_ADMIN
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                    .signWith(getSigningKey(), Jwts.SIG.HS512)
                    .compact();
        }

        // Usuários normais de oficina (com multi-tenancy)
        log.debug("Generating access token for user: {} (oficina: {})",
                  usuario.getEmail(),
                  usuario.getOficina().getId());

        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("email", usuario.getEmail())
                .claim("perfil", usuario.getPerfil().name())
                .claim("oficinaId", usuario.getOficina().getId().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Generates a refresh token for the given user.
     *
     * <p>Contains only the userId as subject (minimal claims for security).
     * Must be stored in Redis for revocation capability (logout).
     *
     * @param usuario the authenticated user
     * @return JWT refresh token (7 days validity)
     */
    public String generateRefreshToken(Usuario usuario) {
        log.debug("Generating refresh token for user: {}", usuario.getEmail());

        return Jwts.builder()
                .subject(usuario.getId().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Validates a JWT token by verifying signature and expiration.
     *
     * @param token the JWT token to validate
     * @return true if token is valid and not expired, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            log.debug("Token validation successful");
            return true;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts the userId (subject) from the token.
     *
     * @param token the JWT token
     * @return userId as UUID
     * @throws IllegalArgumentException if subject is not a valid UUID
     */
    public UUID extractUserId(String token) {
        String subject = extractClaim(token, Claims::getSubject);
        return UUID.fromString(subject);
    }

    /**
     * Extracts the email claim from the token.
     *
     * @param token the JWT token
     * @return user email
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    /**
     * Extracts the perfil (role) claim from the token.
     *
     * @param token the JWT token
     * @return user profile/role
     * @throws IllegalArgumentException if perfil value is invalid
     */
    public PerfilUsuario extractPerfil(String token) {
        String perfilName = extractClaim(token, claims -> claims.get("perfil", String.class));
        return PerfilUsuario.valueOf(perfilName);
    }

    /**
     * Extracts the oficinaId claim from the token.
     *
     * <p>This claim is used for multi-tenancy data isolation.
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *   <li>SUPER_ADMIN tokens: NO oficinaId claim → returns null</li>
     *   <li>Other users: oficinaId claim present → returns UUID</li>
     * </ul>
     *
     * @param token the JWT token
     * @return oficina ID as UUID, or null if user is SUPER_ADMIN
     * @throws IllegalArgumentException if oficinaId claim exists but is not a valid UUID
     */
    public UUID extractOficinaId(String token) {
        String oficinaIdStr = extractClaim(token, claims -> claims.get("oficinaId", String.class));

        // SUPER_ADMIN não tem oficinaId no token
        if (oficinaIdStr == null || oficinaIdStr.isBlank()) {
            return null;
        }

        return UUID.fromString(oficinaIdStr);
    }

    /**
     * Checks if the user is a SUPER_ADMIN based on the perfil claim.
     *
     * <p>Used by filters to determine if tenant isolation should be bypassed.</p>
     *
     * @param token the JWT token
     * @return true if user has SUPER_ADMIN perfil, false otherwise
     */
    public boolean isSuperAdmin(String token) {
        try {
            PerfilUsuario perfil = extractPerfil(token);
            return perfil == PerfilUsuario.SUPER_ADMIN;
        } catch (Exception e) {
            log.warn("Failed to extract perfil from token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts the expiration date from the token.
     *
     * @param token the JWT token
     * @return expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Checks if the token is expired.
     *
     * @param token the JWT token
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Generic method to extract a specific claim from the token.
     *
     * @param token the JWT token
     * @param claimsResolver function to extract the desired claim
     * @param <T> the type of the claim
     * @return the extracted claim value
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from the token.
     *
     * @param token the JWT token
     * @return all claims
     * @throws io.jsonwebtoken.JwtException if token is invalid or expired
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Gets the cached signing key for HMAC SHA-512.
     *
     * <p>The key is validated and cached during service initialization.
     *
     * @return the secret key for signing/verifying tokens
     */
    private SecretKey getSigningKey() {
        return signingKey;
    }
}
