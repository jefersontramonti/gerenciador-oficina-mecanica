package com.pitstop.shared.controller;

import com.pitstop.shared.dto.LoginRequest;
import com.pitstop.shared.dto.LoginResponse;
import com.pitstop.shared.dto.RefreshResponse;
import com.pitstop.shared.dto.RefreshTokenRequest;
import com.pitstop.shared.security.AuthenticationService;
import com.pitstop.shared.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 *   <li>Refresh token (POST /api/auth/refresh)</li>
 *   <li>Logout (POST /api/auth/logout)</li>
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

    private final AuthenticationService authenticationService;

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
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("POST /api/auth/login - email: {}", request.email());

        LoginResponse response = authenticationService.login(request);

        // Create HttpOnly cookie with refresh token (7 days)
        ResponseCookie cookie = ResponseCookie.from("refreshToken", response.refreshToken())
                .httpOnly(true)
                .secure(false) // TODO: Set to true in production (requires HTTPS)
                .path("/api/auth")
                .maxAge(7 * 24 * 60 * 60) // 7 days in seconds
                .sameSite("Strict")
                .build();

        log.info("Login successful - email: {}", request.email());

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
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken
    ) {
        log.debug("POST /api/auth/refresh");

        // Priority: cookie > request body (cookie is more secure)
        String refreshToken = cookieRefreshToken != null
                ? cookieRefreshToken
                : (requestBody != null ? requestBody.refreshToken() : null);

        if (refreshToken == null) {
            log.warn("Refresh token missing in both cookie and request body");
            return ResponseEntity.badRequest().build();
        }

        RefreshResponse response = authenticationService.refresh(refreshToken);

        // Update HttpOnly cookie with new refresh token
        ResponseCookie cookie = ResponseCookie.from("refreshToken", response.refreshToken())
                .httpOnly(true)
                .secure(false) // TODO: Set to true in production
                .path("/api/auth")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

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
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.debug("POST /api/auth/logout");

        UUID userId = userDetails.getUsuario().getId();
        authenticationService.logout(userId);

        // Clear the refresh token cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false) // TODO: Set to true in production
                .path("/api/auth")
                .maxAge(0) // Expire immediately
                .sameSite("Strict")
                .build();

        log.info("Logout successful - userId: {}", userId);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}
