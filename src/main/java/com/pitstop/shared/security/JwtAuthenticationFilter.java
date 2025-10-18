package com.pitstop.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter that intercepts HTTP requests and validates JWT tokens.
 *
 * <p><b>Filter execution flow:</b>
 * <ol>
 *   <li>Extract JWT token from {@code Authorization} header (format: "Bearer {token}")</li>
 *   <li>If no token found, skip authentication (let Spring Security handle unauthorized access)</li>
 *   <li>Validate token signature and expiration using {@link JwtService}</li>
 *   <li>Extract email from token and load full user via {@link CustomUserDetailsService}</li>
 *   <li>If valid, populate {@link SecurityContextHolder} with authenticated user</li>
 *   <li>Continue filter chain</li>
 * </ol>
 *
 * <p><b>SecurityContext lifecycle:</b>
 * <ul>
 *   <li>SecurityContext is request-scoped (cleared after each request)</li>
 *   <li>This filter runs once per request ({@link OncePerRequestFilter})</li>
 *   <li>If authentication succeeds, subsequent filters/controllers see authenticated user</li>
 * </ul>
 *
 * <p><b>Error handling:</b>
 * Invalid or expired tokens are silently ignored (no exception thrown).
 * Spring Security will deny access to protected endpoints automatically.
 *
 * <p><b>Single-tenant note:</b>
 * Currently does not extract or validate tenant information from tokens.
 * When migrating to SaaS multi-tenant, extract "tenantId" claim from token and store in
 * a ThreadLocal or custom Authentication object for tenant isolation in service layer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Main filter method that processes each HTTP request.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain filter chain to continue processing
     * @throws ServletException if servlet error occurs
     * @throws IOException if I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Extract Authorization header
        final String authHeader = request.getHeader("Authorization");

        // Skip authentication if no Bearer token present
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No JWT token found in request to: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract token (remove "Bearer " prefix)
            final String jwt = authHeader.substring(7);

            // Validate token (signature + expiration)
            if (!jwtService.validateToken(jwt)) {
                log.warn("Invalid JWT token in request to: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // Extract email from token
            final String userEmail = jwtService.extractEmail(jwt);

            // Check if user is already authenticated in SecurityContext
            // (avoid redundant database queries if filter runs multiple times)
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Load full user from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                // Create authentication token with authorities
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // credentials (password) not needed after authentication
                        userDetails.getAuthorities()
                );

                // Set additional details (IP address, session ID, etc.)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Populate SecurityContext with authenticated user
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("User authenticated successfully: {} (URI: {})", userEmail, request.getRequestURI());
            }

        } catch (Exception e) {
            // Log error but don't throw exception (let Spring Security deny access)
            log.error("JWT authentication failed for request to {}: {}", request.getRequestURI(), e.getMessage());
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}
