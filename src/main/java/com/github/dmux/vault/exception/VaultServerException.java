package com.github.dmux.vault.exception;

/**
 * Exception thrown when the Vault server returns a server-side error.
 * This typically occurs when:
 * - The Vault server encounters an internal error
 * - HTTP 5xx (Server Error) responses are received
 * - The Vault server is unavailable or overloaded
 */
public class VaultServerException extends VaultException {

    /**
     * Constructs a new VaultServerException with the specified detail message.
     *
     * @param message the detail message
     */
    public VaultServerException(String message) {
        super(message);
    }

    /**
     * Constructs a new VaultServerException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public VaultServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
