package com.github.dmux.vault.exception;

/**
 * Exception thrown when authentication with Vault fails.
 * This typically occurs when:
 * - Invalid credentials are provided
 * - The Vault token is expired or invalid
 * - HTTP 401 (Unauthorized) or 403 (Forbidden) responses are received
 */
public class VaultAuthenticationException extends VaultException {

    /**
     * Constructs a new VaultAuthenticationException with the specified detail message.
     *
     * @param message the detail message
     */
    public VaultAuthenticationException(String message) {
        super(message);
    }

    /**
     * Constructs a new VaultAuthenticationException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public VaultAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
