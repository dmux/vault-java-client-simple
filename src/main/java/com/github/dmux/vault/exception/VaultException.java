package com.github.dmux.vault.exception;

/**
 * Base checked exception for all Vault-related errors.
 * All specific Vault exceptions extend from this class.
 */
public class VaultException extends Exception {

    /**
     * Constructs a new VaultException with the specified detail message.
     *
     * @param message the detail message
     */
    public VaultException(String message) {
        super(message);
    }

    /**
     * Constructs a new VaultException with the specified detail message and cause.
     * This constructor preserves the original exception for debugging purposes.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public VaultException(String message, Throwable cause) {
        super(message, cause);
    }
}
