package com.pitstop.shared.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing refresh tokens in Redis.
 *
 * <p>Refresh tokens are stored in Redis (not in database) for the following reasons:
 * <ul>
 *   <li><b>Performance</b>: Redis is faster than PostgreSQL for key-value lookups</li>
 *   <li><b>Automatic expiration</b>: Redis TTL automatically removes expired tokens</li>
 *   <li><b>Revocation</b>: Logout simply deletes the token from Redis</li>
 *   <li><b>Stateless JWT + Revocation hybrid</b>: Access tokens are stateless, refresh tokens are stateful</li>
 * </ul>
 *
 * <p><b>Key format:</b> {@code refresh_token:{userId}}
 *
 * <p><b>Value format:</b> JSON string containing:
 * <ul>
 *   <li>{@code token}: the refresh token string</li>
 *   <li>{@code userId}: UUID of the user</li>
 *   <li>{@code createdAt}: timestamp when token was created</li>
 *   <li>{@code expiresAt}: timestamp when token expires</li>
 * </ul>
 *
 * <p><b>Token rotation:</b>
 * When a refresh token is used to generate a new access token, a NEW refresh token is generated
 * and stored in Redis (replacing the old one). This prevents token reuse attacks.
 *
 * <p><b>Single-tenant note:</b>
 * Currently stores tokens with key {@code refresh_token:{userId}}.
 * When migrating to SaaS multi-tenant, consider using {@code refresh_token:{tenantId}:{userId}}
 * to isolate tokens by tenant (optional - userId already provides uniqueness).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${application.jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    /**
     * Stores a refresh token in Redis.
     *
     * <p>If a token already exists for this user, it will be replaced (token rotation).
     *
     * @param userId the user's ID
     * @param token the refresh token string
     */
    public void storeRefreshToken(UUID userId, String token) {
        String key = buildKey(userId);

        RefreshTokenData data = new RefreshTokenData(
                token,
                userId,
                LocalDateTime.now(),
                LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000)
        );

        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, refreshTokenExpiration, TimeUnit.MILLISECONDS);
            log.debug("Refresh token stored in Redis for user: {}", userId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize refresh token data for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Erro ao armazenar refresh token", e);
        }
    }

    /**
     * Retrieves a refresh token from Redis.
     *
     * @param userId the user's ID
     * @return the refresh token string, or empty if not found or expired
     */
    public Optional<String> getRefreshToken(UUID userId) {
        String key = buildKey(userId);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            log.debug("No refresh token found in Redis for user: {}", userId);
            return Optional.empty();
        }

        try {
            RefreshTokenData data = objectMapper.readValue(json, RefreshTokenData.class);
            log.debug("Refresh token retrieved from Redis for user: {}", userId);
            return Optional.of(data.token());
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize refresh token data for user {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Deletes a refresh token from Redis.
     *
     * <p>Used during logout to revoke the refresh token.
     *
     * @param userId the user's ID
     */
    public void deleteRefreshToken(UUID userId) {
        String key = buildKey(userId);
        Boolean deleted = redisTemplate.delete(key);

        if (Boolean.TRUE.equals(deleted)) {
            log.debug("Refresh token deleted from Redis for user: {}", userId);
        } else {
            log.debug("No refresh token to delete for user: {}", userId);
        }
    }

    /**
     * Validates if a refresh token is valid for a given user.
     *
     * <p>Checks if:
     * <ul>
     *   <li>Token exists in Redis for the user</li>
     *   <li>Token value matches the provided token</li>
     * </ul>
     *
     * <p>Note: Token expiration is already handled by Redis TTL.
     *
     * @param userId the user's ID
     * @param token the refresh token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean isRefreshTokenValid(UUID userId, String token) {
        Optional<String> storedToken = getRefreshToken(userId);

        boolean isValid = storedToken.isPresent() && storedToken.get().equals(token);

        if (isValid) {
            log.debug("Refresh token validation successful for user: {}", userId);
        } else {
            log.warn("Refresh token validation failed for user: {}", userId);
        }

        return isValid;
    }

    /**
     * Builds the Redis key for a user's refresh token.
     *
     * @param userId the user's ID
     * @return the Redis key
     */
    private String buildKey(UUID userId) {
        return "refresh_token:" + userId.toString();
    }

    /**
     * Internal record for storing refresh token metadata in Redis.
     *
     * <p>Stored as JSON string in Redis.
     *
     * @param token the refresh token string
     * @param userId the user's ID
     * @param createdAt when the token was created
     * @param expiresAt when the token expires
     */
    private record RefreshTokenData(
            String token,
            UUID userId,
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {}
}
