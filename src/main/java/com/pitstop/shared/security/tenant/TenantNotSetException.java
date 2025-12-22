package com.pitstop.shared.security.tenant;

/**
 * Exception thrown when tenant context is not set for current request.
 *
 * <p>This typically happens when:</p>
 * <ul>
 *   <li>Accessing TenantContext before filter sets it</li>
 *   <li>Public endpoints trying to access tenant data</li>
 *   <li>Background jobs without tenant context</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public class TenantNotSetException extends RuntimeException {

    public TenantNotSetException(String message) {
        super(message);
    }

    public TenantNotSetException(String message, Throwable cause) {
        super(message, cause);
    }
}
