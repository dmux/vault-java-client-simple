package com.github.dmux.vault.exception;

/**
 * Exception thrown when a requested secret is not found in Vault.
 * This typically occurs when:
 * - The secret path does not exist
 * - The requested version of a secret does not exist
 * - HTTP 404 (Not Found) response is received
 */
public class VaultSecretNotFoundException extends VaultException {

    /**
     * Constructs a new VaultSecretNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public VaultSecretNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new VaultSecretNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public VaultSecretNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
