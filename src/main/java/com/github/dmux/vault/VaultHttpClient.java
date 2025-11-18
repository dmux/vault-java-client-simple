package com.github.dmux.vault;

import com.github.dmux.vault.exception.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for communicating with Vault server.
 * Handles GET and POST requests with proper error handling and SSL configuration.
 */
public class VaultHttpClient {
    private final VaultConfig config;
    private static SSLSocketFactory insecureSocketFactory;
    private static HostnameVerifier insecureHostnameVerifier;

    /**
     * Constructs a VaultHttpClient with the given configuration.
     *
     * @param config Vault configuration
     */
    public VaultHttpClient(VaultConfig config) {
        this.config = config;
    }

    /**
     * Performs a GET request to the specified path.
     *
     * @param path Vault API path (e.g., "/v1/secret/data/myapp/config")
     * @param token Vault authentication token
     * @return VaultResponse containing the response data
     * @throws VaultException if the request fails
     */
    public VaultResponse get(String path, String token) throws VaultException {
        String url = config.getVaultUrl() + path;
        HttpURLConnection conn = null;

        try {
            conn = createConnection(url, "GET");
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("X-Vault-Token", token);
            }
            conn.connect();

            return handleResponse(conn);
        } catch (SocketTimeoutException e) {
            throw new VaultConnectionException(
                "Request timeout after " + config.getReadTimeout() + "ms: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new VaultConnectionException(
                "Failed to connect to Vault: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Performs a POST request to the specified path with the given body.
     *
     * @param path Vault API path
     * @param body Request body (JSON string)
     * @param token Vault authentication token (can be null for auth requests)
     * @return VaultResponse containing the response data
     * @throws VaultException if the request fails
     */
    public VaultResponse post(String path, String body, String token) throws VaultException {
        String url = config.getVaultUrl() + path;
        HttpURLConnection conn = null;

        try {
            conn = createConnection(url, "POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("X-Vault-Token", token);
            }

            // Write request body
            if (body != null && !body.isEmpty()) {
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
            }

            return handleResponse(conn);
        } catch (SocketTimeoutException e) {
            throw new VaultConnectionException(
                "Request timeout after " + config.getReadTimeout() + "ms: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new VaultConnectionException(
                "Failed to connect to Vault: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Creates and configures an HttpURLConnection for the given URL and method.
     *
     * @param urlString URL to connect to
     * @param method HTTP method (GET, POST, etc.)
     * @return Configured HttpURLConnection
     * @throws IOException if connection creation fails
     * @throws VaultException if SSL configuration fails
     */
    private HttpURLConnection createConnection(String urlString, String method) 
            throws IOException, VaultException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod(method);
        conn.setConnectTimeout(config.getConnectionTimeout());
        conn.setReadTimeout(config.getReadTimeout());
        conn.setRequestProperty("Accept", "application/json");

        // Disable SSL verification if configured
        if (!config.isSslVerify() && conn instanceof HttpsURLConnection) {
            HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
            try {
                httpsConn.setSSLSocketFactory(getInsecureSocketFactory());
                httpsConn.setHostnameVerifier(getInsecureHostnameVerifier());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new VaultException("Failed to configure SSL: " + e.getMessage(), e);
            }
        }

        return conn;
    }

    /**
     * Handles the HTTP response and maps status codes to appropriate exceptions.
     *
     * @param conn HttpURLConnection with completed request
     * @return VaultResponse containing response data
     * @throws VaultException if the response indicates an error
     * @throws IOException if reading the response fails
     */
    private VaultResponse handleResponse(HttpURLConnection conn) 
            throws VaultException, IOException {
        int statusCode = conn.getResponseCode();
        Map<String, String> headers = extractHeaders(conn);
        String body;

        // Read response body from appropriate stream
        if (statusCode >= 200 && statusCode < 300) {
            body = readStream(conn.getInputStream());
        } else {
            body = readStream(conn.getErrorStream());
        }

        // Map status codes to exceptions
        if (statusCode == 401 || statusCode == 403) {
            String errorMessage = extractErrorMessage(body);
            throw new VaultAuthenticationException(
                "Authentication failed (HTTP " + statusCode + "): " + errorMessage);
        } else if (statusCode == 404) {
            String errorMessage = extractErrorMessage(body);
            throw new VaultSecretNotFoundException(
                "Secret not found (HTTP 404): " + errorMessage);
        } else if (statusCode >= 500) {
            String errorMessage = extractErrorMessage(body);
            throw new VaultServerException(
                "Vault server error (HTTP " + statusCode + "): " + errorMessage);
        } else if (statusCode >= 400) {
            String errorMessage = extractErrorMessage(body);
            throw new VaultException(
                "Vault request failed (HTTP " + statusCode + "): " + errorMessage);
        }

        return new VaultResponse(statusCode, body, headers);
    }

    /**
     * Extracts headers from the HTTP connection.
     *
     * @param conn HttpURLConnection
     * @return Map of header names to values
     */
    private Map<String, String> extractHeaders(HttpURLConnection conn) {
        Map<String, String> headers = new HashMap<>();
        Map<String, List<String>> headerFields = conn.getHeaderFields();
        
        if (headerFields != null) {
            for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();
                if (key != null && values != null && !values.isEmpty()) {
                    headers.put(key, values.get(0));
                }
            }
        }
        
        return headers;
    }

    /**
     * Reads the entire content from an input stream.
     *
     * @param inputStream Input stream to read from
     * @return String content of the stream
     * @throws IOException if reading fails
     */
    private String readStream(java.io.InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, "utf-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * Extracts error messages from Vault JSON error response.
     * Vault returns errors in the format: {"errors": ["error message 1", "error message 2"]}
     *
     * @param responseBody JSON response body
     * @return Extracted error message or the original body if parsing fails
     */
    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return "Unknown error";
        }

        try {
            JSONObject json = new JSONObject(responseBody);
            if (json.has("errors")) {
                JSONArray errors = json.getJSONArray("errors");
                if (errors.length() > 0) {
                    StringBuilder errorMsg = new StringBuilder();
                    for (int i = 0; i < errors.length(); i++) {
                        if (i > 0) {
                            errorMsg.append(", ");
                        }
                        errorMsg.append(errors.getString(i));
                    }
                    return errorMsg.toString();
                }
            }
        } catch (Exception e) {
            // If JSON parsing fails, return the raw body
            return responseBody;
        }

        return responseBody;
    }

    /**
     * Gets or creates an insecure SSL socket factory that trusts all certificates.
     * WARNING: This should only be used in development environments.
     *
     * @return SSLSocketFactory that trusts all certificates
     * @throws NoSuchAlgorithmException if SSL algorithm is not available
     * @throws KeyManagementException if SSL context initialization fails
     */
    private static synchronized SSLSocketFactory getInsecureSocketFactory() 
            throws NoSuchAlgorithmException, KeyManagementException {
        if (insecureSocketFactory == null) {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            insecureSocketFactory = sslContext.getSocketFactory();
        }
        return insecureSocketFactory;
    }

    /**
     * Gets or creates an insecure hostname verifier that accepts all hostnames.
     * WARNING: This should only be used in development environments.
     *
     * @return HostnameVerifier that accepts all hostnames
     */
    private static synchronized HostnameVerifier getInsecureHostnameVerifier() {
        if (insecureHostnameVerifier == null) {
            insecureHostnameVerifier = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
        }
        return insecureHostnameVerifier;
    }
}
