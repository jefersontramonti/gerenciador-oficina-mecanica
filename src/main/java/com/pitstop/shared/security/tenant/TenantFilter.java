package com.pitstop.shared.security.tenant;

import com.pitstop.shared.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that extracts oficinaId from JWT token and sets it in TenantContext.
 *
 * <p><b>Execution order:</b> Runs AFTER JwtAuthenticationFilter</p>
 * <ol>
 *   <li>JwtAuthenticationFilter validates JWT and sets SecurityContext</li>
 *   <li>TenantFilter extracts oficinaId from JWT</li>
 *   <li>TenantFilter sets TenantContext for current thread</li>
 *   <li>Controllers/Services use TenantContext.getTenantId()</li>
 *   <li>TenantFilter clears context in finally block</li>
 * </ol>
 *
 * <p><b>Multi-Tenancy vs SaaS Admin:</b></p>
 * <ul>
 *   <li><b>Regular users (ADMIN, GERENTE, etc.):</b> oficinaId is extracted and set in TenantContext</li>
 *   <li><b>SUPER_ADMIN:</b> NO oficinaId in token → TenantContext is NOT set → bypasses tenant isolation</li>
 * </ul>
 *
 * <p><b>Security:</b> SUPER_ADMIN can ONLY access /api/saas/* endpoints (enforced by @PreAuthorize annotations)</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2) // Run AFTER JwtAuthenticationFilter (order=1)
public class TenantFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Extract JWT token from Authorization header
            String token = extractTokenFromRequest(request);

            if (token != null) {
                // Check if user is SUPER_ADMIN (no tenant isolation)
                if (jwtService.isSuperAdmin(token)) {
                    log.debug("SUPER_ADMIN request detected for {}: bypassing tenant isolation", request.getRequestURI());
                    // DO NOT set TenantContext for SUPER_ADMIN
                    // They only have access to /api/saas/* (enforced by @PreAuthorize)
                } else {
                    // Regular oficina user: extract oficinaId and set TenantContext
                    UUID oficinaId = jwtService.extractOficinaId(token);

                    if (oficinaId == null) {
                        log.warn("Token has no oficinaId claim but user is not SUPER_ADMIN. Request: {}", request.getRequestURI());
                        // Let it continue - Spring Security will deny access if needed
                    } else {
                        TenantContext.setTenantId(oficinaId);
                        log.debug("Tenant context set for request {}: oficinaId={}", request.getRequestURI(), oficinaId);
                    }
                }
            }

            // Continue filter chain
            filterChain.doFilter(request, response);

        } finally {
            // CRITICAL: Always clear context to prevent:
            // - Memory leaks (ThreadLocal not released)
            // - Cross-request contamination (thread pool reuse)
            // - Security issues (wrong tenant data)
            TenantContext.clear();
        }
    }

    /**
     * Extracts Bearer token from Authorization header.
     *
     * @param request HTTP request
     * @return JWT token without "Bearer " prefix, or null if not found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " prefix
        }

        return null;
    }

    /**
     * Determines if this filter should be skipped for certain endpoints.
     *
     * <p>Public endpoints don't need tenant context:</p>
     * <ul>
     *   <li>/api/auth/* - Login, register, password reset</li>
     *   <li>/api/public/* - Public APIs</li>
     *   <li>/swagger-ui/** - API documentation</li>
     *   <li>/v3/api-docs/** - OpenAPI spec</li>
     *   <li>/actuator/** - Health checks, metrics</li>
     * </ul>
     *
     * @param request HTTP request
     * @return true if filter should NOT execute
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/actuator/");
    }
}
