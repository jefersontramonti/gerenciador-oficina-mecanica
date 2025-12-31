package com.pitstop.config;

import com.pitstop.shared.security.JwtAuthenticationFilter;
import com.pitstop.shared.security.tenant.TenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration for PitStop application.
 *
 * <p>Configures:
 * <ul>
 *   <li><b>JWT authentication</b>: Stateless authentication via JWT tokens</li>
 *   <li><b>Multi-tenancy</b>: Row-level tenant isolation via TenantFilter</li>
 *   <li><b>CORS</b>: Cross-Origin Resource Sharing for frontend (React/Vite)</li>
 *   <li><b>Authorization</b>: Role-Based Access Control (RBAC) via @PreAuthorize</li>
 *   <li><b>Password encoding</b>: BCrypt with 12 rounds</li>
 * </ul>
 *
 * <p><b>Security & Multi-Tenancy Flow:</b>
 * <ol>
 *   <li>Request arrives at server</li>
 *   <li>CORS filter checks if origin is allowed</li>
 *   <li>JwtAuthenticationFilter extracts and validates JWT token</li>
 *   <li>If valid, populates SecurityContext with authenticated user</li>
 *   <li><b>TenantFilter extracts oficinaId from JWT and sets TenantContext</b></li>
 *   <li>Spring Security checks @PreAuthorize annotations on controllers</li>
 *   <li>If authorized, request reaches controller method</li>
 *   <li>Services/Repositories use TenantContext.getTenantId() for data isolation</li>
 *   <li>TenantFilter clears context in finally block (prevents thread pollution)</li>
 * </ol>
 *
 * <p><b>Public endpoints (no authentication required):</b>
 * <ul>
 *   <li>/api/auth/login - User login</li>
 *   <li>/api/auth/register - User registration</li>
 *   <li>/api/auth/refresh - Token refresh</li>
 *   <li>/swagger-ui/** - API documentation</li>
 *   <li>/v3/api-docs/** - OpenAPI specs</li>
 *   <li>/actuator/health - Health check</li>
 * </ul>
 *
 * <p><b>Protected endpoints (authentication + tenant isolation):</b>
 * <ul>
 *   <li>/api/auth/logout - User logout (requires valid JWT)</li>
 *   <li>/api/usuarios/** - User management (requires ADMIN role)</li>
 *   <li>All other /api/** endpoints - Automatically filtered by oficinaId</li>
 * </ul>
 *
 * <p><b>Multi-tenancy implementation:</b>
 * JWT tokens contain an "oficinaId" claim that identifies the tenant (oficina).
 * Every database query is automatically filtered by this oficinaId to ensure
 * complete data isolation between different oficinas in the SaaS model.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TenantFilter tenantFilter;

    /**
     * Configures the security filter chain.
     *
     * @param http HttpSecurity configuration
     * @return configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (not needed for stateless JWT authentication)
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS with custom configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Stateless session management (no session cookies)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no authentication required)
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/public/**",
                                "/api/health",
                                "/api/webhooks/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/ws/**"
                        ).permitAll()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Add JWT filter before Spring Security's UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Add Tenant filter after JWT filter (requires JWT to extract oficinaId)
                .addFilterAfter(tenantFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing) for frontend access.
     *
     * <p>Allows requests from:
     * <ul>
     *   <li>http://localhost:3000 (Create React App default)</li>
     *   <li>http://localhost:5173 (Vite default)</li>
     * </ul>
     *
     * <p>In production, replace with actual frontend domain.
     *
     * @return CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed origins (frontend URLs)
        // In production, add actual frontend domain via environment variable
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:5174"
        ));

        // Allowed HTTP methods
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Allowed headers
        configuration.setAllowedHeaders(List.of("*"));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Expose Authorization header to frontend
        configuration.setExposedHeaders(List.of("Authorization"));

        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Provides the AuthenticationManager bean.
     *
     * <p>Used by Spring Security for authentication operations.
     *
     * @param config authentication configuration
     * @return AuthenticationManager
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configures password encoding with BCrypt.
     *
     * <p>Uses strength 12 (2^12 = 4096 iterations) for security vs performance balance.
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
