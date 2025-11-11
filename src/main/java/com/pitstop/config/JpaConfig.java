package com.pitstop.config;

import com.pitstop.shared.security.CustomUserDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Optional;

/**
 * JPA configuration for PitStop application.
 *
 * Enables:
 * - JPA Auditing for automatic @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy
 * - JPA Repositories scanning
 * - Transaction management
 *
 * Naming strategy (configured in application.yml):
 * - Physical: CamelCaseToUnderscoresNamingStrategy (e.g., createdAt → created_at)
 * - Implicit: SpringImplicitNamingStrategy
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableJpaRepositories(basePackages = "com.pitstop")
@EnableTransactionManagement
public class JpaConfig {

    /**
     * Provides the current auditor (user email) for JPA audit fields.
     *
     * <p>The auditor is determined as follows:</p>
     * <ul>
     *   <li>If user is authenticated: returns user's email from CustomUserDetails</li>
     *   <li>If not authenticated or anonymous: returns "SYSTEM"</li>
     * </ul>
     *
     * @return AuditorAware bean that supplies the current user's email
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
                return Optional.of("SYSTEM");
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof CustomUserDetails userDetails) {
                return Optional.of(userDetails.getUsername()); // Returns email
            }

            // Fallback to principal's toString() (username)
            return Optional.of(principal.toString());
        };
    }
}
