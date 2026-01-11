package com.pitstop.shared.controller;

import com.pitstop.shared.dto.*;
import com.pitstop.shared.security.AuthenticationService;
import com.pitstop.shared.security.CustomUserDetails;
import com.pitstop.shared.security.PasswordResetService;
import com.pitstop.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for authentication operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Login (POST /api/auth/login)</li>
 *   <li>Register (POST /api/auth/register)</li>
 *   <li>Refresh token (POST /api/auth/refresh)</li>
 *   <li>Logout (POST /api/auth/logout)</li>
 *   <li>Get current user profile (GET /api/auth/me)</li>
 *   <li>Update profile (PUT /api/auth/profile)</li>
 *   <li>Change password (PUT /api/auth/password)</li>
 *   <li>Forgot password (POST /api/auth/forgot-password)</li>
 *   <li>Reset password (POST /api/auth/reset-password)</li>
 * </ul>
 *
 * <p><b>Security notes:</b>
 * <ul>
 *   <li>Refresh tokens are sent as HttpOnly cookies to prevent XSS attacks</li>
 *   <li>Access tokens are sent in response body (frontend stores in memory)</li>
 *   <li>CORS is configured to allow credentials (cookies)</li>
 *   <li>Cookies use SameSite=Strict to prevent CSRF attacks</li>
 * </ul>
 *
 * <p><b>Cookie security:</b>
 * <ul>
 *   <li><b>httpOnly</b>: JavaScript cannot access the cookie (prevents XSS)</li>
 *   <li><b>secure</b>: Cookie only sent over HTTPS in production</li>
 *   <li><b>sameSite=Strict</b>: Cookie not sent on cross-site requests (prevents CSRF)</li>
 *   <li><b>path=/api/auth</b>: Cookie only sent to auth endpoints (minimal exposure)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticação", description = "Endpoints de login, refresh e logout")
public class AuthController {

    /**
     * Cookie path for refresh token.
     * Using "/" for compatibility with reverse proxy configurations where
     * the external path (/auth) differs from internal path (/api/auth).
     * The cookie is still protected by httpOnly, secure, and sameSite flags.
     */
    private static final String COOKIE_PATH = "/";

    /**
     * Cookie max age in seconds (7 days = 604800 seconds).
     */
    private static final long COOKIE_MAX_AGE_SECONDS = 7 * 24 * 60 * 60;

    /**
     * SameSite policy for cookies (Lax for better compatibility).
     */
    private static final String COOKIE_SAME_SITE = "Lax";

    /**
     * Minimum characters to show at start and end of masked email.
     */
    private static final int EMAIL_MASK_VISIBLE_CHARS = 2;

    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Cookie domain for cross-subdomain authentication.
     * Set to ".domain.com" to share cookies across subdomains.
     * Leave empty for same-origin cookies (default for local development).
     */
    @Value("${app.cookie.domain:}")
    private String cookieDomain;

    /**
     * Authenticates a user and returns JWT tokens.
     *
     * <p>Returns both tokens in response body AND sets refresh token as HttpOnly cookie.
     *
     * @param request login credentials (email + password)
     * @return login response with access token, refresh token, and user data
     */
    @PostMapping("/login")
    @Operation(
            summary = "Login de usuário",
            description = "Autentica usuário com email e senha, retorna tokens JWT e dados do usuário"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login realizado com sucesso",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciais inválidas ou usuário inativo",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos",
                    content = @Content
            )
    })
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        LoginResponse response = authenticationService.login(request);

        // Cookie duration: Always 7 days for refresh token persistence
        // The "rememberMe" option controls localStorage user data, not the refresh token cookie
        // This ensures users stay logged in even if they didn't check "remember me"
        ResponseCookie cookie = buildRefreshTokenCookie(response.refreshToken(), COOKIE_MAX_AGE_SECONDS, httpRequest);

        log.info("Login successful - email: {}, rememberMe: {}", maskEmail(request.email()), request.isRememberMe());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    /**
     * Refreshes an access token using a refresh token.
     *
     * <p>Implements token rotation: both access and refresh tokens are regenerated.
     *
     * <p>The refresh token can be provided in two ways:
     * <ol>
     *   <li>In request body (for testing or mobile apps)</li>
     *   <li>As HttpOnly cookie (recommended for web apps)</li>
     * </ol>
     *
     * @param requestBody optional refresh token in request body
     * @param cookieRefreshToken optional refresh token from cookie
     * @return refresh response with new access and refresh tokens
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar access token",
            description = "Renova o access token usando um refresh token válido (token rotation)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token renovado com sucesso",
                    content = @Content(schema = @Schema(implementation = RefreshResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh token inválido, expirado ou revogado",
                    content = @Content
            )
    })
    public ResponseEntity<RefreshResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken,
            HttpServletRequest httpRequest
    ) {

        // Priority: cookie > request body (cookie is more secure)
        String refreshToken = cookieRefreshToken != null
                ? cookieRefreshToken
                : (requestBody != null ? requestBody.refreshToken() : null);

        if (refreshToken == null) {
            log.warn("Refresh token missing in both cookie and request body");
            return ResponseEntity.badRequest().build();
        }

        RefreshResponse response = authenticationService.refresh(refreshToken);

        // Update HttpOnly cookie with new refresh token (token rotation)
        ResponseCookie cookie = buildRefreshTokenCookie(response.refreshToken(), COOKIE_MAX_AGE_SECONDS, httpRequest);

        log.info("Token refresh successful");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    /**
     * Logs out a user by revoking their refresh token.
     *
     * <p>Deletes the refresh token from Redis and clears the cookie.
     *
     * <p>The access token remains valid until expiration (stateless JWT limitation).
     *
     * @param userDetails authenticated user from SecurityContext
     * @return 200 OK with cleared cookie
     */
    @PostMapping("/logout")
    @Operation(
            summary = "Logout de usuário",
            description = "Invalida o refresh token do usuário e limpa o cookie"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Logout realizado com sucesso",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content
            )
    })
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest
    ) {

        UUID userId = userDetails.getUsuario().getId();
        authenticationService.logout(userId);

        // Clear the refresh token cookie (maxAge=0 expires immediately)
        ResponseCookie cookie = buildRefreshTokenCookie("", 0, httpRequest);

        log.info("Logout successful - userId: {}", userId);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    /**
     * Registers a new user in the system.
     *
     * <p>Creates a new user account with ATENDENTE profile and immediately logs them in.
     * Returns JWT tokens for immediate authentication.
     *
     * @param request registration data (name, email, password)
     * @return login response with access token, refresh token, and user data
     */
    @PostMapping("/register")
    @Operation(
            summary = "Registro de novo usuário",
            description = "Cria uma nova conta de usuário no sistema e retorna tokens JWT"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuário registrado com sucesso",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email já está em uso",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos",
                    content = @Content
            )
    })
    public ResponseEntity<LoginResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {

        LoginResponse response = authenticationService.register(request);

        // Create HttpOnly cookie with refresh token
        ResponseCookie cookie = buildRefreshTokenCookie(response.refreshToken(), COOKIE_MAX_AGE_SECONDS, httpRequest);

        log.info("Registration successful - email: {}", maskEmail(request.email()));

        return ResponseEntity.status(201)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    /**
     * Gets the current authenticated user's profile.
     *
     * <p>Returns the profile information of the user making the request.
     *
     * @param userDetails authenticated user from SecurityContext
     * @return user profile information
     */
    @GetMapping("/me")
    @Operation(
            summary = "Obter perfil do usuário atual",
            description = "Retorna os dados do usuário autenticado"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = UsuarioResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content
            )
    })
    public ResponseEntity<UsuarioResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUsuario().getId();
        UsuarioResponse response = authenticationService.getCurrentUser(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Updates the authenticated user's profile.
     *
     * <p>Allows the user to update their name and email address.
     * Password changes must be done through the dedicated endpoint.
     *
     * @param userDetails authenticated user from SecurityContext
     * @param request update data (name, email)
     * @return updated user profile
     */
    @PutMapping("/profile")
    @Operation(
            summary = "Atualizar perfil do usuário",
            description = "Atualiza nome e email do usuário autenticado"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = UsuarioResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email já está em uso por outro usuário",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos",
                    content = @Content
            )
    })
    public ResponseEntity<UsuarioResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ) {

        UUID userId = userDetails.getUsuario().getId();
        UsuarioResponse response = authenticationService.updateProfile(userId, request);

        log.info("Profile updated - userId: {}", userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Changes the authenticated user's password.
     *
     * <p>Requires the current password for security verification.
     * The new password must meet minimum security requirements.
     *
     * @param userDetails authenticated user from SecurityContext
     * @param request password change data (current password, new password)
     * @return 200 OK if successful
     */
    @PutMapping("/password")
    @Operation(
            summary = "Trocar senha do usuário",
            description = "Altera a senha do usuário autenticado (requer senha atual)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Senha alterada com sucesso",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Senha atual incorreta ou usuário não autenticado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos",
                    content = @Content
            )
    })
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {

        UUID userId = userDetails.getUsuario().getId();
        authenticationService.changePassword(userId, request);

        log.info("Password changed - userId: {}", userId);

        return ResponseEntity.ok().build();
    }

    /**
     * Initiates password reset process by sending a reset link to user's email.
     *
     * <p>Always returns 200 OK to prevent user enumeration.
     * If email doesn't exist, no email is sent but response is the same.
     *
     * @param request forgot password request with email
     * @return 200 OK with message
     */
    @PostMapping("/forgot-password")
    @Operation(
            summary = "Solicitar reset de senha",
            description = "Envia um link de recuperação de senha para o email informado"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Se o email existir, um link de recuperação será enviado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos",
                    content = @Content
            )
    })
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {

        passwordResetService.forgotPassword(request);

        // Always return success to prevent user enumeration
        return ResponseEntity.ok().build();
    }

    /**
     * Resets user password using a valid reset token.
     *
     * @param request reset password request with token and new password
     * @return 200 OK if successful
     */
    @PostMapping("/reset-password")
    @Operation(
            summary = "Resetar senha com token",
            description = "Define uma nova senha usando o token recebido por email"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Senha alterada com sucesso",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token inválido ou expirado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos",
                    content = @Content
            )
    })
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetPassword(request);

        log.info("Password reset successful");

        return ResponseEntity.ok().build();
    }

    /**
     * Builds a refresh token cookie with proper security settings.
     *
     * <p>Cookie properties:
     * <ul>
     *   <li>httpOnly: true (prevents XSS)</li>
     *   <li>secure: true in production (HTTPS only)</li>
     *   <li>sameSite: Lax (CSRF protection while allowing navigation)</li>
     *   <li>domain: configurable for cross-subdomain auth</li>
     *   <li>path: /api/auth (minimal exposure)</li>
     * </ul>
     *
     * @param tokenValue the refresh token value (empty string to clear)
     * @param maxAge cookie max age in seconds (0 to expire immediately)
     * @param request the HTTP request
     * @return configured ResponseCookie
     */
    private ResponseCookie buildRefreshTokenCookie(String tokenValue, long maxAge, HttpServletRequest request) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("refreshToken", tokenValue)
                .httpOnly(true)
                .secure(isSecureRequest(request))
                .path(COOKIE_PATH)
                .sameSite(COOKIE_SAME_SITE)
                .maxAge(maxAge);

        // Set domain for cross-subdomain cookie sharing
        // e.g., ".pitstopai.com.br" allows cookies between app.pitstopai.com.br and api.pitstopai.com.br
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        return builder.build();
    }

    /**
     * Determines if the current request is secure (HTTPS).
     *
     * <p>Detection methods (in priority order):</p>
     * <ol>
     *   <li>Production profile always returns true (assumes HTTPS termination at load balancer)</li>
     *   <li>X-Forwarded-Proto header (set by reverse proxies/load balancers)</li>
     *   <li>Request.isSecure() (direct HTTPS connection)</li>
     * </ol>
     *
     * @param request the HTTP request
     * @return true if request is secure (HTTPS)
     */
    private boolean isSecureRequest(HttpServletRequest request) {
        // In production, always use secure cookies (HTTPS is required)
        if ("prod".equalsIgnoreCase(activeProfile)) {
            return true;
        }

        // Check X-Forwarded-Proto header (set by reverse proxies like nginx, AWS ALB)
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null) {
            return "https".equalsIgnoreCase(forwardedProto);
        }

        // Fallback to direct connection check
        return request.isSecure();
    }

    /**
     * Masks an email address for secure logging.
     *
     * <p>Example: "user@example.com" → "us***@***le.com"
     *
     * @param email the email to mask
     * @return masked email
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }

        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts.length > 1 ? parts[1] : "";

        String maskedLocal = local.length() > EMAIL_MASK_VISIBLE_CHARS
                ? local.substring(0, EMAIL_MASK_VISIBLE_CHARS) + "***"
                : "***";

        String maskedDomain = domain.length() > EMAIL_MASK_VISIBLE_CHARS
                ? "***" + domain.substring(domain.length() - EMAIL_MASK_VISIBLE_CHARS)
                : "***";

        return maskedLocal + "@" + maskedDomain;
    }
}
