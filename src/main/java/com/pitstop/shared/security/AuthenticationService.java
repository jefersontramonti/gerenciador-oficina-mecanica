package com.pitstop.shared.security;

import com.pitstop.shared.dto.*;
import com.pitstop.usuario.domain.PerfilUsuario;
import com.pitstop.usuario.domain.Usuario;
import com.pitstop.usuario.dto.UsuarioResponse;
import com.pitstop.usuario.exception.*;
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

        // 3. Load user from database (with oficina for JWT generation)
        Usuario usuario = usuarioRepository.findByIdWithOficina(userId)
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

    /**
     * Registers a new user in the system.
     *
     * <p><b>SECURITY NOTE:</b> This method is DISABLED for public registration.
     * User registration must be done through one of these secure methods:</p>
     * <ul>
     *   <li>POST /api/public/oficinas/register - Creates oficina + admin user (SaaS onboarding)</li>
     *   <li>POST /api/usuarios - Admin creates users within their oficina (authenticated)</li>
     * </ul>
     *
     * <p>The reason this is disabled: public registration without oficina context
     * would create orphan users without tenant association, breaking multi-tenancy
     * and causing JWT generation to fail (NullPointerException on oficina.getId()).</p>
     *
     * @param request registration request containing name, email, and password
     * @return never returns - always throws exception
     * @throws UnsupportedOperationException always - public registration is disabled
     * @deprecated Use /api/public/oficinas/register for SaaS onboarding or /api/usuarios for user creation
     */
    @Deprecated
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        log.warn("SECURITY: Attempt to use disabled public registration endpoint for email: {}", request.email());

        throw new UnsupportedOperationException(
            "Registro público desabilitado por segurança. " +
            "Use /api/public/oficinas/register para criar uma nova oficina ou " +
            "solicite ao administrador da oficina que crie seu usuário."
        );
    }

    /**
     * Gets the current authenticated user's profile.
     *
     * @param userId the ID of the authenticated user (from JWT)
     * @return user response with profile information
     * @throws UsuarioNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UsuarioResponse getCurrentUser(UUID userId) {
        log.debug("Get profile request for user: {}", userId);

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Get profile failed: user not found - {}", userId);
                    return new UsuarioNotFoundException(userId);
                });

        return usuarioMapper.toResponse(usuario);
    }

    /**
     * Updates the authenticated user's profile (name and email).
     *
     * <p>If the email is changed, verifies that the new email is not already in use.
     *
     * @param userId the ID of the authenticated user (from JWT)
     * @param request update request containing new name and email
     * @return updated user response
     * @throws UsuarioNotFoundException if user not found
     * @throws EmailAlreadyExistsException if new email is already in use by another user
     */
    @Transactional
    public UsuarioResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        log.debug("Update profile request for user: {}", userId);

        // 1. Load user from database
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Update profile failed: user not found - {}", userId);
                    return new UsuarioNotFoundException(userId);
                });

        // 2. If email is being changed, verify new email is not in use
        if (request.email() != null && !request.email().isBlank() && !usuario.getEmail().equals(request.email())) {
            usuarioRepository.findByEmail(request.email())
                    .ifPresent(existingUser -> {
                        log.warn("Update profile failed: email already in use - {}", request.email());
                        throw new EmailAlreadyExistsException(request.email());
                    });
            usuario.setEmail(request.email());
        }

        // 3. Update user data
        usuario.setNome(request.nome());

        // 4. Save changes
        Usuario updatedUsuario = usuarioRepository.save(usuario);

        log.info("Profile updated successfully for user: {} (ID: {})", updatedUsuario.getEmail(), updatedUsuario.getId());

        return usuarioMapper.toResponse(updatedUsuario);
    }

    /**
     * Changes the authenticated user's password.
     *
     * <p>Requires the current password for security verification.
     * The new password is hashed using BCrypt before storage.
     *
     * @param userId the ID of the authenticated user (from JWT)
     * @param request change password request with current and new passwords
     * @throws UsuarioNotFoundException if user not found
     * @throws InvalidCredentialsException if current password is incorrect
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.debug("Change password request for user: {}", userId);

        // 1. Load user from database
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Change password failed: user not found - {}", userId);
                    return new UsuarioNotFoundException(userId);
                });

        // 2. Verify current password is correct
        if (!passwordEncoder.matches(request.currentPassword(), usuario.getSenha())) {
            log.warn("Change password failed: incorrect current password for user {}", userId);
            throw new InvalidCredentialsException();
        }

        // 3. Hash and update password
        usuario.setSenha(passwordEncoder.encode(request.newPassword()));

        // 4. Save changes
        usuarioRepository.save(usuario);

        log.info("Password changed successfully for user: {} (ID: {})", usuario.getEmail(), usuario.getId());
    }
}
