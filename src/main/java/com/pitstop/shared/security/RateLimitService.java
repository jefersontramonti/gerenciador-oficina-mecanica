package com.pitstop.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple in-memory rate limiting service.
 *
 * <p>Provides protection against brute-force attacks on public endpoints
 * like quote approval, password reset, etc.</p>
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>Sliding window rate limiting</li>
 *   <li>Automatic cleanup of expired entries</li>
 *   <li>Thread-safe implementation</li>
 *   <li>Configurable limits per endpoint type</li>
 * </ul>
 *
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>In-memory only (not distributed)</li>
 *   <li>Lost on application restart</li>
 *   <li>For distributed systems, migrate to Redis-based implementation</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class RateLimitService {

    /**
     * Stores request counts per key (IP + endpoint).
     * Key format: "endpoint:identifier" (e.g., "orcamento:192.168.1.1")
     */
    private final Map<String, RateLimitEntry> requestCounts = new ConcurrentHashMap<>();

    /**
     * Default rate limits (requests per time window).
     */
    private static final int DEFAULT_MAX_REQUESTS = 10;
    private static final long DEFAULT_WINDOW_SECONDS = 3600; // 1 hour

    /**
     * Specific limits for different endpoint types.
     */
    private static final int ORCAMENTO_MAX_REQUESTS = 20;      // Quote approval: 20 requests/hour
    private static final int PASSWORD_RESET_MAX_REQUESTS = 5;  // Password reset: 5 requests/hour
    private static final int WEBHOOK_MAX_REQUESTS = 1000;      // Webhooks: 1000 requests/hour

    /**
     * Cleanup scheduler.
     */
    private final ScheduledExecutorService cleanupScheduler;

    public RateLimitService() {
        // Schedule cleanup every 10 minutes
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
        this.cleanupScheduler.scheduleAtFixedRate(
            this::cleanupExpiredEntries,
            10, 10, TimeUnit.MINUTES
        );
        log.info("RateLimitService initialized with automatic cleanup every 10 minutes");
    }

    /**
     * Checks if a request is allowed for quote approval endpoints.
     *
     * @param identifier Unique identifier (usually IP address)
     * @return true if request is allowed, false if rate limited
     */
    public boolean isOrcamentoRequestAllowed(String identifier) {
        return isAllowed("orcamento", identifier, ORCAMENTO_MAX_REQUESTS, DEFAULT_WINDOW_SECONDS);
    }

    /**
     * Checks if a request is allowed for password reset endpoints.
     *
     * @param identifier Unique identifier (usually IP address or email)
     * @return true if request is allowed, false if rate limited
     */
    public boolean isPasswordResetAllowed(String identifier) {
        return isAllowed("password-reset", identifier, PASSWORD_RESET_MAX_REQUESTS, DEFAULT_WINDOW_SECONDS);
    }

    /**
     * Checks if a webhook request is allowed.
     *
     * @param identifier Unique identifier (usually IP address)
     * @return true if request is allowed, false if rate limited
     */
    public boolean isWebhookAllowed(String identifier) {
        return isAllowed("webhook", identifier, WEBHOOK_MAX_REQUESTS, DEFAULT_WINDOW_SECONDS);
    }

    /**
     * Generic rate limit check.
     *
     * @param endpoint Endpoint identifier
     * @param identifier Client identifier (IP, email, etc.)
     * @param maxRequests Maximum requests allowed in window
     * @param windowSeconds Time window in seconds
     * @return true if request is allowed, false if rate limited
     */
    public boolean isAllowed(String endpoint, String identifier, int maxRequests, long windowSeconds) {
        String key = endpoint + ":" + sanitizeKey(identifier);
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(windowSeconds);

        RateLimitEntry entry = requestCounts.compute(key, (k, existing) -> {
            if (existing == null || existing.windowStart.isBefore(windowStart)) {
                // New entry or window expired
                return new RateLimitEntry(now, 1);
            } else {
                // Increment count
                existing.count++;
                return existing;
            }
        });

        boolean allowed = entry.count <= maxRequests;

        if (!allowed) {
            log.warn("RATE LIMIT EXCEEDED: endpoint={}, identifier={}, count={}, max={}",
                endpoint, maskIdentifier(identifier), entry.count, maxRequests);
        } else if (entry.count > maxRequests * 0.8) {
            // Warning when approaching limit (80%)
            log.info("Rate limit warning: endpoint={}, identifier={}, count={}/{}",
                endpoint, maskIdentifier(identifier), entry.count, maxRequests);
        }

        return allowed;
    }

    /**
     * Gets remaining requests for an endpoint/identifier.
     *
     * @param endpoint Endpoint identifier
     * @param identifier Client identifier
     * @param maxRequests Maximum requests allowed
     * @return Remaining requests (0 if exceeded)
     */
    public int getRemainingRequests(String endpoint, String identifier, int maxRequests) {
        String key = endpoint + ":" + sanitizeKey(identifier);
        RateLimitEntry entry = requestCounts.get(key);

        if (entry == null) {
            return maxRequests;
        }

        return Math.max(0, maxRequests - entry.count);
    }

    /**
     * Resets rate limit for a specific key.
     * Use with caution - typically only for admin/testing purposes.
     *
     * @param endpoint Endpoint identifier
     * @param identifier Client identifier
     */
    public void resetLimit(String endpoint, String identifier) {
        String key = endpoint + ":" + sanitizeKey(identifier);
        requestCounts.remove(key);
        log.info("Rate limit reset for: {}", key);
    }

    /**
     * Cleans up expired entries to prevent memory leaks.
     */
    private void cleanupExpiredEntries() {
        Instant threshold = Instant.now().minusSeconds(DEFAULT_WINDOW_SECONDS * 2);
        int removed = 0;

        var iterator = requestCounts.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().windowStart.isBefore(threshold)) {
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            log.debug("Rate limit cleanup: removed {} expired entries", removed);
        }
    }

    /**
     * Sanitizes key to prevent injection.
     */
    private String sanitizeKey(String key) {
        if (key == null) {
            return "unknown";
        }
        // Remove any special characters that could cause issues
        return key.replaceAll("[^a-zA-Z0-9.:-]", "_").substring(0, Math.min(key.length(), 100));
    }

    /**
     * Masks identifier for logging (privacy).
     */
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 6) {
            return "***";
        }
        // Show first 3 and last 2 characters
        return identifier.substring(0, 3) + "***" + identifier.substring(identifier.length() - 2);
    }

    /**
     * Internal class to track rate limit entries.
     */
    private static class RateLimitEntry {
        Instant windowStart;
        int count;

        RateLimitEntry(Instant windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
