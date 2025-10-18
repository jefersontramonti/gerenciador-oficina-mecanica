package com.pitstop.shared.security;

import com.pitstop.shared.dto.LoginRequest;
import com.pitstop.shared.dto.LoginResponse;
import com.pitstop.shared.dto.RefreshResponse;
import com.pitstop.usuario.domain.Usuario;
import com.pitstop.usuario.exception.InvalidCredentialsException;
import com.pitstop.usuario.exception.UsuarioInativoException;
import com.pitstop.usuario.exception.UsuarioNotFoundException;
import com.pitstop.usuario.mapper.UsuarioMapper;
import com.pitstop.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service responsible for authentication operations (login, refresh, logout).
 *
 * <p><b>Authentication flow:</b>
 * <ol>
 *   <li>User sends POST /api/auth/login with email + password</li>
 *   <li>Service validates credentials (email exists, password matches, user is active)</li>
 *   <li>Generate access token (15 min) and refresh token (7 days)</li>
 *   <li>Store refresh token in Redis for revocation capability</li>
 *   <li>Update user's last access timestamp</li>
 *   <li>Return tokens + user information</li>
 * </ol>
 *
 * <p><b>Token refresh flow:</b>
 * <ol>
 *   <li>User sends POST /api/auth/refresh with refresh token</li>
 *   <li>Service validates token (signature, expiration, exists in Redis)</li>
 *   <li>Generate NEW access token and NEW refresh token (token rotation)</li>
 *   <li>Replace old refresh token in Redis with new one</li>
 *   <li>Return new tokens</li>
 * </ol>
 *
 * <p><b>Logout flow:</b>
 * <ol>
 *   <li>User sends POST /api/auth/logout</li>
 *   <li>Service deletes refresh token from Redis</li>
 *   <li>User's access token remains valid until expiration (stateless)</li>
 *   <li>User cannot refresh access token anymore (refresh token revoked)</li>
 * </ol>
 *
 * <p><b>Single-tenant note:</b>
 * Currently does not validate or store tenant information.
 * When migrating to SaaS multi-tenant, add tenant validation in login:
 * <ul>
 *   <li>User belongs to tenant</li>
 *   <li>Tenant is active</li>
 *   <li>Store tenantId in JWT claims</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UsuarioMapper usuarioMapper;

    /**
     * Authenticates a user and generates JWT tokens.
     *
     * @param request login request containing email and password
     * @return login response with access token, refresh token, and user data
     * @throws InvalidCredentialsException if email not found or password incorrect
     * @throws UsuarioInativoException if user account is deactivated
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.debug("Login attempt for user: {}", request.email());

        // 1. Validate credentials - find user by email
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found - {}", request.email());
                    return new InvalidCredentialsException();
                });

        // 2. Check if user is active
        if (!usuario.isAtivo()) {
            log.warn("Login failed: user inactive - {}", request.email());
            throw new UsuarioInativoException(usuario.getEmail());
        }

        // 3. Validate password (BCrypt comparison)
        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            log.warn("Login failed: incorrect password - {}", request.email());
            throw new InvalidCredentialsException();
        }

        // 4. Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(usuario);
        String refreshToken = jwtService.generateRefreshToken(usuario);

        log.debug("Tokens generated for user: {}", usuario.getEmail());

        // 5. Store refresh token in Redis (for revocation capability)
        refreshTokenService.storeRefreshToken(usuario.getId(), refreshToken);

        // 6. Update user's last access timestamp
        usuario.atualizarUltimoAcesso();
        usuarioRepository.save(usuario);

        log.info("Login successful for user: {} (Perfil: {})", usuario.getEmail(), usuario.getPerfil());

        // 7. Return response with tokens and user data (without password)
        return new LoginResponse(
                accessToken,
                refreshToken,
                usuarioMapper.toResponse(usuario)
        );
    }

    /**
     * Refreshes an access token using a valid refresh token.
     *
     * <p>Implements token rotation for security: both access and refresh tokens are regenerated.
     *
     * @param refreshToken the refresh token from login or previous refresh
     * @return refresh response with new access and refresh tokens
     * @throws InvalidCredentialsException if refresh token is invalid or expired
     * @throws UsuarioNotFoundException if user not found
     * @throws UsuarioInativoException if user account was deactivated since last login
     */
    @Transactional
    public RefreshResponse refresh(String refreshToken) {
        log.debug("Token refresh attempt");

        // 1. Validate refresh token (signature + expiration)
        if (!jwtService.validateToken(refreshToken)) {
            log.warn("Token refresh failed: invalid or expired token");
            throw new InvalidCredentialsException();
        }

        // 2. Extract userId and verify token exists in Redis
        UUID userId = jwtService.extractUserId(refreshToken);

        if (!refreshTokenService.isRefreshTokenValid(userId, refreshToken)) {
            log.warn("Token refresh failed: token not found in Redis for user {}", userId);
            throw new InvalidCredentialsException();
        }

        // 3. Load user from database
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Token refresh failed: user not found - {}", userId);
                    return new UsuarioNotFoundException(userId);
                });

        // 4. Check if user is still active
        if (!usuario.isAtivo()) {
            log.warn("Token refresh failed: user inactive - {}", usuario.getEmail());
            throw new UsuarioInativoException(usuario.getEmail());
        }

        // 5. Generate NEW tokens (token rotation)
        String newAccessToken = jwtService.generateAccessToken(usuario);
        String newRefreshToken = jwtService.generateRefreshToken(usuario);

        log.debug("New tokens generated for user: {}", usuario.getEmail());

        // 6. Update refresh token in Redis (replace old with new)
        refreshTokenService.storeRefreshToken(usuario.getId(), newRefreshToken);

        log.info("Token refresh successful for user: {}", usuario.getEmail());

        // 7. Return new tokens
        return new RefreshResponse(newAccessToken, newRefreshToken);
    }

    /**
     * Logs out a user by revoking their refresh token.
     *
     * <p>The access token remains valid until expiration (stateless JWT limitation).
     * However, the user cannot obtain a new access token without logging in again.
     *
     * @param userId the ID of the user to logout
     */
    @Transactional
    public void logout(UUID userId) {
        log.debug("Logout request for user: {}", userId);

        // Delete refresh token from Redis
        refreshTokenService.deleteRefreshToken(userId);

        log.info("Logout successful for user: {}", userId);
    }
}
