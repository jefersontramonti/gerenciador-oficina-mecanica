package com.pitstop.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when there is a conflict with existing data (e.g., duplicate key violation).
 *
 * <p>This exception is typically used for business rule violations where the requested operation
 * cannot be completed because it would violate a uniqueness constraint or business invariant.</p>
 *
 * <p><b>Common scenarios:</b></p>
 * <ul>
 *   <li>Attempting to register a user with an email that already exists</li>
 *   <li>Attempting to create an oficina with a CNPJ/CPF that is already registered</li>
 *   <li>Attempting to create a resource with a unique identifier that conflicts with existing data</li>
 * </ul>
 *
 * <p>When this exception is thrown, Spring automatically returns HTTP 409 Conflict status.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    /**
     * Constructs a new ConflictException with the specified detail message.
     *
     * @param message the detail message explaining the conflict
     */
    public ConflictException(String message) {
        super(message);
    }

    /**
     * Constructs a new ConflictException with the specified detail message and cause.
     *
     * @param message the detail message explaining the conflict
     * @param cause the cause of the conflict (can be null)
     */
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
