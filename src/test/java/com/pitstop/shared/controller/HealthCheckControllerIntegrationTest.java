package com.pitstop.shared.controller;

import com.pitstop.shared.dto.HealthCheckResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for HealthCheckController using TestContainers.
 *
 * This test validates:
 * - PostgreSQL connectivity
 * - Redis connectivity
 * - Application startup
 * - Health endpoint response
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class HealthCheckControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:16-alpine"))
        .withDatabaseName("pitstop_test")
        .withUsername("test_user")
        .withPassword("test_password");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
        DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // Disable Liquibase for this test (no migrations yet)
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @Test
    void shouldReturnHealthStatusWhenAllServicesAreUp() {
        // Given
        String url = "http://localhost:" + port + "/api/health";

        // When
        ResponseEntity<HealthCheckResponse> response = restTemplate.getForEntity(
            url, HealthCheckResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("UP");
        assertThat(response.getBody().getApplication()).isEqualTo("PitStop");
        assertThat(response.getBody().getVersion()).isEqualTo("0.0.1-SNAPSHOT");
        assertThat(response.getBody().getTimestamp()).isNotNull();

        // Validate services
        assertThat(response.getBody().getServices()).containsKeys("postgresql-neon", "redis");
        assertThat(response.getBody().getServices().get("postgresql-neon")).isEqualTo("UP");
        assertThat(response.getBody().getServices().get("redis")).isEqualTo("UP");
    }

    @Test
    void shouldStartContainersSuccessfully() {
        // Validate containers are running
        assertThat(postgres.isRunning()).isTrue();
        assertThat(redis.isRunning()).isTrue();
    }
}
