package com.pitstop.shared.security.tenant;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Thread-safe context holder for current tenant (oficina).
 *
 * <p>Uses ThreadLocal to ensure isolation between concurrent requests.
 * Each HTTP request thread has its own tenant context.</p>
 *
 * <p><b>Usage Flow:</b></p>
 * <ol>
 *   <li>TenantFilter extracts oficinaId from JWT token</li>
 *   <li>TenantFilter calls {@link #setTenantId(UUID)} before controller</li>
 *   <li>Repositories/Services call {@link #getTenantId()} for queries</li>
 *   <li>TenantFilter calls {@link #clear()} in finally block</li>
 * </ol>
 *
 * <p><b>IMPORTANT:</b> Always clear() in finally block to prevent memory leaks!</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Slf4j
public final class TenantContext {

    /**
     * ThreadLocal storage for tenant ID.
     * Each thread (HTTP request) has its own copy.
     */
    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();

    /**
     * Private constructor - utility class.
     */
    private TenantContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Sets the current tenant ID for this thread.
     *
     * <p>Called by TenantFilter after extracting oficinaId from JWT.</p>
     *
     * @param tenantId Oficina ID (must not be null)
     * @throws IllegalArgumentException if tenantId is null
     */
    public static void setTenantId(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }

        currentTenant.set(tenantId);
        log.debug("Tenant context set for thread {}: {}", Thread.currentThread().getName(), tenantId);
    }

    /**
     * Gets the current tenant ID for this thread.
     *
     * <p>Called by repositories and services to filter queries by oficina.</p>
     *
     * @return Current oficina ID
     * @throws TenantNotSetException if no tenant is set for current thread
     */
    public static UUID getTenantId() {
        UUID tenantId = currentTenant.get();

        if (tenantId == null) {
            log.error("Tenant context not set for thread: {}", Thread.currentThread().getName());
            throw new TenantNotSetException(
                "Tenant context not set for current request. " +
                "Ensure TenantFilter is properly configured."
            );
        }

        return tenantId;
    }

    /**
     * Gets the current tenant ID if set, or returns null.
     *
     * <p>Use this for optional tenant operations (e.g., logging, metrics).</p>
     *
     * @return Current oficina ID or null if not set
     */
    public static UUID getTenantIdOrNull() {
        return currentTenant.get();
    }

    /**
     * Checks if tenant context is set for current thread.
     *
     * @return true if tenant is set, false otherwise
     */
    public static boolean isSet() {
        return currentTenant.get() != null;
    }

    /**
     * Clears the tenant context for current thread.
     *
     * <p><b>CRITICAL:</b> ALWAYS call this in finally block of TenantFilter!</p>
     *
     * <p>Failure to clear can cause:</p>
     * <ul>
     *   <li>Memory leaks (ThreadLocal not released)</li>
     *   <li>Cross-request contamination (wrong tenant in pooled threads)</li>
     *   <li>Security vulnerabilities (user A sees user B's data)</li>
     * </ul>
     */
    public static void clear() {
        UUID tenantId = currentTenant.get();
        if (tenantId != null) {
            log.debug("Clearing tenant context for thread {}: {}",
                Thread.currentThread().getName(), tenantId);
        }
        currentTenant.remove();
    }

    /**
     * Sets tenant ID for current thread (for testing/background jobs).
     *
     * <p><b>USE WITH CAUTION!</b> Only for:</p>
     * <ul>
     *   <li>Unit tests that need tenant context</li>
     *   <li>Background jobs with known tenant</li>
     *   <li>Scheduled tasks with specific oficina</li>
     * </ul>
     *
     * <p>Example (test):</p>
     * <pre>
     * {@code
     * @BeforeEach
     * void setup() {
     *     TenantContext.setTenantIdUnsafe(UUID.fromString("..."));
     * }
     *
     * @AfterEach
     * void teardown() {
     *     TenantContext.clear();
     * }
     * }
     * </pre>
     *
     * @param tenantId Oficina ID
     */
    public static void setTenantIdUnsafe(UUID tenantId) {
        log.warn("Setting tenant context UNSAFELY (test/background job): {}", tenantId);
        setTenantId(tenantId);
    }
}
