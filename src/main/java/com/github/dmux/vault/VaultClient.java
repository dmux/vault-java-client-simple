package com.github.dmux.vault;

import com.github.dmux.vault.exception.VaultAuthenticationException;
import com.github.dmux.vault.exception.VaultException;

import java.util.Map;

/**
 * Main facade for interacting with HashiCorp Vault.
 * Provides methods for authentication and secret retrieval using KV v2 secrets engine.
 * 
 * Example usage:
 * <pre>
 * VaultConfig config = new VaultConfig.Builder()
 *     .vaultUrl("http://localhost:8200")
 *     .awsRegion("us-east-1")
 *     .awsRole("my-app-role")
 *     .build();
 * 
 * VaultClient client = new VaultClient(config);
 * client.authenticate();
 * Map&lt;String, Object&gt; secret = client.readSecret("secret/myapp/config");
 * </pre>
 */
public class VaultClient {
    private final VaultConfig config;
    private final VaultHttpClient httpClient;
    private final AwsAuthenticator authenticator;
    private String token;

    /**
     * Constructs a VaultClient with the given configuration.
     * Initializes the HTTP client and AWS authenticator.
     *
     * @param config Vault configuration
     */
    public VaultClient(VaultConfig config) {
        this.config = config;
        this.httpClient = new VaultHttpClient(config);
        this.authenticator = new AwsAuthenticator(config, httpClient);
        this.token = null;
    }

    /**
     * Authenticates with Vault using AWS IAM credentials.
     * The obtained token is stored and used for subsequent operations.
     *
     * @throws VaultException if authentication fails
     */
    public void authenticate() throws VaultException {
        this.token = authenticator.authenticate();
    }

    /**
     * Checks if the client is currently authenticated.
     *
     * @return true if authenticated (token is not null), false otherwise
     */
    public boolean isAuthenticated() {
        return token != null;
    }

    /**
     * Reads a secret from the specified path using KV v2 secrets engine.
     * Returns the secret data and metadata.
     *
     * @param path Secret path (e.g., "secret/myapp/config")
     * @return Map containing the secret data under the "data" key and metadata under "metadata" key
     * @throws VaultAuthenticationException if not authenticated
     * @throws VaultException if the request fails
     */
    public Map<String, Object> readSecret(String path) throws VaultException {
        validateAuthentication();
        
        String kvV2Path = buildKvV2Path(path);
        VaultResponse response = httpClient.get(kvV2Path, token);
        
        return extractSecretData(response);
    }

    /**
     * Reads a specific version of a secret from the specified path using KV v2 secrets engine.
     *
     * @param path Secret path (e.g., "secret/myapp/config")
     * @param version Version number to retrieve
     * @return Map containing the secret data under the "data" key and metadata under "metadata" key
     * @throws VaultAuthenticationException if not authenticated
     * @throws VaultException if the request fails
     */
    public Map<String, Object> readSecretVersion(String path, int version) throws VaultException {
        validateAuthentication();
        
        String kvV2Path = buildKvV2Path(path) + "?version=" + version;
        VaultResponse response = httpClient.get(kvV2Path, token);
        
        return extractSecretData(response);
    }

    /**
     * Validates that the client is authenticated before performing operations.
     *
     * @throws VaultAuthenticationException if not authenticated
     */
    private void validateAuthentication() throws VaultAuthenticationException {
        if (!isAuthenticated()) {
            throw new VaultAuthenticationException(
                "Not authenticated. Call authenticate() before performing operations.");
        }
    }

    /**
     * Builds the KV v2 path by inserting "/data/" between the mount point and secret path.
     * 
     * Examples:
     * - "secret/myapp/config" -> "/v1/secret/data/myapp/config"
     * - "/secret/myapp/config" -> "/v1/secret/data/myapp/config"
     *
     * @param path Secret path
     * @return KV v2 formatted path
     * @throws IllegalArgumentException if the path is invalid
     */
    private String buildKvV2Path(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Secret path cannot be null or empty");
        }

        // Remove leading slash if present
        path = path.startsWith("/") ? path.substring(1) : path;

        // Split into mount and secret path
        String[] parts = path.split("/", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException(
                "Invalid secret path: must contain at least mount and secret name (e.g., 'secret/myapp/config')");
        }

        String mount = parts[0];
        String secretPath = parts[1];

        return "/v1/" + mount + "/data/" + secretPath;
    }

    /**
     * Extracts secret data and metadata from the Vault response.
     * KV v2 returns data in the format: {"data": {"data": {...}, "metadata": {...}}}
     *
     * @param response Vault HTTP response
     * @return Map containing "data" (secret values) and "metadata" (version info, timestamps, etc.)
     * @throws VaultException if the response format is invalid
     */
    private Map<String, Object> extractSecretData(VaultResponse response) throws VaultException {
        Map<String, Object> responseData = response.parseJson();
        
        if (!responseData.containsKey("data")) {
            throw new VaultException("Invalid secret response: missing 'data' field");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> dataWrapper = (Map<String, Object>) responseData.get("data");
        
        if (!dataWrapper.containsKey("data")) {
            throw new VaultException("Invalid secret response: missing nested 'data' field");
        }
        
        // Extract the actual secret data
        @SuppressWarnings("unchecked")
        Map<String, Object> secretData = (Map<String, Object>) dataWrapper.get("data");
        
        // Extract metadata if available
        Object metadata = dataWrapper.get("metadata");
        
        // Return a map with both data and metadata
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("data", secretData);
        if (metadata != null) {
            result.put("metadata", metadata);
        }
        
        return result;
    }
}
