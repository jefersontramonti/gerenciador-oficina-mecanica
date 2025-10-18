package com.pitstop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA configuration for PitStop application.
 *
 * Enables:
 * - JPA Auditing for automatic @CreatedDate and @LastModifiedDate
 * - JPA Repositories scanning
 * - Transaction management
 *
 * Naming strategy (configured in application.yml):
 * - Physical: CamelCaseToUnderscoresNamingStrategy (e.g., createdAt → created_at)
 * - Implicit: SpringImplicitNamingStrategy
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.pitstop")
@EnableTransactionManagement
public class JpaConfig {
    // Additional JPA configurations can be added here if needed
}
