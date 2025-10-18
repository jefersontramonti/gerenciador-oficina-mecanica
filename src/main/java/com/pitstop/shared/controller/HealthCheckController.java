package com.pitstop.shared.controller;

import com.pitstop.shared.dto.HealthCheckResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint for validating infrastructure connectivity.
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health Check", description = "Application health and infrastructure status")
public class HealthCheckController {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    @GetMapping
    @Operation(
        summary = "Check application health",
        description = "Validates connectivity to PostgreSQL and Redis, returns application status"
    )
    public ResponseEntity<HealthCheckResponse> checkHealth() {
        log.debug("Health check requested");

        Map<String, String> services = new HashMap<>();

        // Check PostgreSQL (Neon)
        try {
            dataSource.getConnection().close();
            services.put("postgresql-neon", "UP");
            log.debug("PostgreSQL (Neon) connection: OK");
        } catch (Exception e) {
            services.put("postgresql-neon", "DOWN - " + e.getMessage());
            log.error("PostgreSQL (Neon) connection failed", e);
        }

        // Check Redis
        try {
            redisConnectionFactory.getConnection().close();
            services.put("redis", "UP");
            log.debug("Redis connection: OK");
        } catch (Exception e) {
            services.put("redis", "DOWN - " + e.getMessage());
            log.error("Redis connection failed", e);
        }

        // Determine overall status
        boolean allServicesUp = services.values().stream()
            .allMatch(status -> status.equals("UP"));
        String overallStatus = allServicesUp ? "UP" : "DEGRADED";

        HealthCheckResponse response = HealthCheckResponse.builder()
            .status(overallStatus)
            .application("PitStop")
            .version("0.0.1-SNAPSHOT")
            .timestamp(LocalDateTime.now())
            .services(services)
            .build();

        log.info("Health check completed: {} - Services: {}", overallStatus, services);

        return ResponseEntity.ok(response);
    }
}
