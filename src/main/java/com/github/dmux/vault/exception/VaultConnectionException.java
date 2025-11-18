package com.github.dmux.vault.exception;

/**
 * Exception thrown when network or connection errors occur while communicating with Vault.
 * This typically occurs when:
 * - Network connectivity issues prevent reaching the Vault server
 * - Connection timeout is exceeded
 * - Socket timeout is exceeded during read operations
 * - DNS resolution fails for the Vault server
 */
public class VaultConnectionException extends VaultException {

    /**
     * Constructs a new VaultConnectionException with the specified detail message.
     *
     * @param message the detail message
     */
    public VaultConnectionException(String message) {
        super(message);
    }

    /**
     * Constructs a new VaultConnectionException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public VaultConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
