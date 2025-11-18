package com.github.dmux.vault;

import com.github.dmux.vault.exception.*;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for VaultHttpClient.
 * Tests HTTP status code mapping to exceptions, error message extraction,
 * and timeout configuration.
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4
 */
public class VaultHttpClientTest {

    private VaultConfig config;
    private VaultHttpClient httpClient;

    @Before
    public void setUp() {
        config = new VaultConfig.Builder()
                .vaultUrl("http://localhost:8200")
                .awsRegion("us-east-1")
                .awsRole("test-role")
                .connectionTimeout(3000)
                .readTimeout(5000)
                .build();
        
        httpClient = new VaultHttpClient(config);
    }

    /**
     * Test that HTTP 401 status code is mapped to VaultAuthenticationException.
     * Requirements: 7.2
     */
    @Test
    public void testHandleResponse401ThrowsAuthenticationException() throws Exception {
        // Arrange
        String errorBody = "{\"errors\":[\"permission denied\"]}";
        MockHttpURLConnection mockConn = createMockConnection(401, errorBody);

        // Act & Assert
        try {
            invokeHandleResponse(mockConn);
            fail("Expected VaultAuthenticationException to be thrown");
        } catch (VaultAuthenticationException e) {
            assertTrue(e.getMessage().contains("Authentication failed"));
            assertTrue(e.getMessage().contains("HTTP 401"));
            assertTrue(e.getMessage().contains("permission denied"));
        }
    }

    /**
     * Test that HTTP 403 status code is mapped to VaultAuthenticationException.
     * Requirements: 7.2
     */
    @Test
    public void testHandleResponse403ThrowsAuthenticationException() throws Exception {
        // Arrange
        String errorBody = "{\"errors\":[\"access denied\"]}";
        MockHttpURLConnection mockConn = createMockConnection(403, errorBody);

        // Act & Assert
        try {
            invokeHandleResponse(mockConn);
            fail("Expected VaultAuthenticationException to be thrown");
        } catch (VaultAuthenticationException e) {
            assertTrue(e.getMessage().contains("Authentication failed"));
            assertTrue(e.getMessage().contains("HTTP 403"));
            assertTrue(e.getMessage().contains("access denied"));
        }
    }

    /**
     * Test that HTTP 404 status code is mapped to VaultSecretNotFoundException.
     * Requirements: 7.2
     */
    @Test
    public void testHandleResponse404ThrowsSecretNotFoundException() throws Exception {
        // Arrange
        String errorBody = "{\"errors\":[\"secret not found\"]}";
        MockHttpURLConnection mockConn = createMockConnection(404, errorBody);

        // Act & Assert
        try {
            invokeHandleResponse(mockConn);
            fail("Expected VaultSecretNotFoundException to be thrown");
        } catch (VaultSecretNotFoundException e) {
            assertTrue(e.getMessage().contains("Secret not found"));
            assertTrue(e.getMessage().contains("HTTP 404"));
            assertTrue(e.getMessage().contains("secret not found"));
        }
    }

    /**
     * Test that HTTP 500 status code is mapped to VaultServerException.
     * Requirements: 7.3
     */
    @Test
    public void testHandleResponse500ThrowsServerException() throws Exception {
        // Arrange
        String errorBody = "{\"errors\":[\"internal server error\"]}";
        MockHttpURLConnection mockConn = createMockConnection(500, errorBody);

        // Act & Assert
        try {
            invokeHandleResponse(mockConn);
            fail("Expected VaultServerException to be thrown");
        } catch (VaultServerException e) {
            assertTrue(e.getMessage().contains("Vault server error"));
            assertTrue(e.getMessage().contains("HTTP 500"));
            assertTrue(e.getMessage().contains("internal server error"));
        }
    }

    /**
     * Test that HTTP 502 status code is mapped to VaultServerException.
     * Requirements: 7.3
     */
    @Test
    public void testHandleResponse502ThrowsServerException() throws Exception {
        // Arrange
        String errorBody = "{\"errors\":[\"bad gateway\"]}";
        MockHttpURLConnection mockConn = createMockConnection(502, errorBody);

        // Act & Assert
        try {
            invokeHandleResponse(mockConn);
            fail("Expected VaultServerException to be thrown");
        } catch (VaultServerException e) {
            assertTrue(e.getMessage().contains("Vault server error"));
            assertTrue(e.getMessage().contains("HTTP 502"));
            assertTrue(e.getMessage().contains("bad gateway"));
        }
    }

    /**
     * Test that HTTP 503 status code is mapped to VaultServerException.
     * Requirements: 7.3
     */
    @Test
    public void testHandleResponse503ThrowsServerException() throws Exception {
        // Arrange
        String errorBody = "{\"errors\":[\"service unavailable\"]}";
        MockHttpURLConnection mockConn = createMockConnection(503, errorBody);

        // Act & Assert
        try {
            invokeHandleResponse(mockConn);
            fail("Expected VaultServerException to be thrown");
        } catch (VaultServerException e) {
            assertTrue(e.getMessage().contains("Vault server error"));
            assertTrue(e.getMessage().contains("HTTP 503"));
            assertTrue(e.getMessage().contains("service unavailable"));
        }
    }

    /**
     * Test that HTTP 400 status code is mapped to generic VaultException.
     * Requirements: 7.2
     */
    @Test
    public void testHandleResponse400ThrowsVaultException() throws Exception {
        // Arrange
        String errorBody = "{\"errors\":[\"bad request\"]}";
        MockHttpURLConnection mockConn = createMockConnection(400, errorBody);

        // Act & Assert
        try {
            invokeHandleResponse(mockConn);
            fail("Expected VaultException to be thrown");
        } catch (VaultException e) {
            // Should not be a more specific exception type
            assertEquals(VaultException.class, e.getClass());
            assertTrue(e.getMessage().contains("Vault request failed"));
            assertTrue(e.getMessage().contains("HTTP 400"));
            assertTrue(e.getMessage().contains("bad request"));
        }
    }

    /**
     * Test that HTTP 200 status code returns successful VaultResponse.
     * Requirements: 7.2
     */
    @Test
    public void testHandleResponse200ReturnsSuccessfulResponse() throws Exception {
        // Arrange
        String responseBody = "{\"data\":{\"key\":\"value\"}}";
        MockHttpURLConnection mockConn = createMockConnection(200, responseBody);

        // Act
        VaultResponse response = invokeHandleResponse(mockConn);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals(responseBody, response.getBody());
    }

    /**
     * Test extraction of single error message from JSON response.
     * Requirements: 7.2
     */
    @Test
    public void testExtractErrorMessageSingleError() throws Exception {
        // Arrange
        String errorBody = "{\"errors\":[\"permission denied\"]}";

        // Act
        String errorMessage = invokeExtractErrorMessage(errorBody);

        // Assert
        assertEquals("permission denied", errorMessage);
    }

    /**
     * Test extraction of multiple error messages from JSON response.
     * Requirements: 7.2
     */
    @Test
    public void testExtractErrorMessageMultipleErrors() throws Exception {
        // Arrange
        String errorBody = "{\"errors\":[\"error 1\",\"error 2\",\"error 3\"]}";

        // Act
        String errorMessage = invokeExtractErrorMessage(errorBody);

        // Assert
        assertEquals("error 1, error 2, error 3", errorMessage);
    }

    /**
     * Test extraction of error message when JSON is invalid.
     * Requirements: 7.2
     */
    @Test
    public void testExtractErrorMessageInvalidJson() throws Exception {
        // Arrange
        String errorBody = "not valid json";

        // Act
        String errorMessage = invokeExtractErrorMessage(errorBody);

        // Assert
        assertEquals("not valid json", errorMessage);
    }

    /**
     * Test extraction of error message when errors array is empty.
     * Requirements: 7.2
     */
    @Test
    public void testExtractErrorMessageEmptyErrorsArray() throws Exception {
        // Arrange
        String errorBody = "{\"errors\":[]}";

        // Act
        String errorMessage = invokeExtractErrorMessage(errorBody);

        // Assert
        assertEquals("{\"errors\":[]}", errorMessage);
    }

    /**
     * Test extraction of error message when body is null.
     * Requirements: 7.2
     */
    @Test
    public void testExtractErrorMessageNullBody() throws Exception {
        // Act
        String errorMessage = invokeExtractErrorMessage(null);

        // Assert
        assertEquals("Unknown error", errorMessage);
    }

    /**
     * Test extraction of error message when body is empty.
     * Requirements: 7.2
     */
    @Test
    public void testExtractErrorMessageEmptyBody() throws Exception {
        // Act
        String errorMessage = invokeExtractErrorMessage("");

        // Assert
        assertEquals("Unknown error", errorMessage);
    }

    /**
     * Test extraction of error message when body is whitespace.
     * Requirements: 7.2
     */
    @Test
    public void testExtractErrorMessageWhitespaceBody() throws Exception {
        // Act
        String errorMessage = invokeExtractErrorMessage("   ");

        // Assert
        assertEquals("Unknown error", errorMessage);
    }

    /**
     * Test that connection timeout is configured correctly.
     * Requirements: 7.4
     */
    @Test
    public void testConnectionTimeoutConfiguration() {
        // Arrange
        VaultConfig customConfig = new VaultConfig.Builder()
                .vaultUrl("http://localhost:8200")
                .awsRegion("us-east-1")
                .awsRole("test-role")
                .connectionTimeout(2000)
                .readTimeout(4000)
                .build();

        // Act
        VaultHttpClient client = new VaultHttpClient(customConfig);

        // Assert
        assertEquals(2000, customConfig.getConnectionTimeout());
        assertEquals(4000, customConfig.getReadTimeout());
    }

    /**
     * Test that default timeouts are used when not specified.
     * Requirements: 7.4
     */
    @Test
    public void testDefaultTimeoutConfiguration() {
        // Arrange
        VaultConfig defaultConfig = new VaultConfig.Builder()
                .vaultUrl("http://localhost:8200")
                .awsRegion("us-east-1")
                .awsRole("test-role")
                .build();

        // Act
        VaultHttpClient client = new VaultHttpClient(defaultConfig);

        // Assert
        assertEquals(5000, defaultConfig.getConnectionTimeout());
        assertEquals(10000, defaultConfig.getReadTimeout());
    }

    // Helper methods for reflection-based testing

    /**
     * Invokes the private handleResponse method using reflection.
     */
    private VaultResponse invokeHandleResponse(HttpURLConnection conn) throws Exception {
        Method method = VaultHttpClient.class.getDeclaredMethod("handleResponse", HttpURLConnection.class);
        method.setAccessible(true);
        try {
            return (VaultResponse) method.invoke(httpClient, conn);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof VaultException) {
                throw (VaultException) cause;
            }
            throw e;
        }
    }

    /**
     * Invokes the private extractErrorMessage method using reflection.
     */
    private String invokeExtractErrorMessage(String responseBody) throws Exception {
        Method method = VaultHttpClient.class.getDeclaredMethod("extractErrorMessage", String.class);
        method.setAccessible(true);
        return (String) method.invoke(httpClient, responseBody);
    }

    /**
     * Creates a mock HttpURLConnection with the specified status code and response body.
     */
    private MockHttpURLConnection createMockConnection(int statusCode, String responseBody) {
        return new MockHttpURLConnection(statusCode, responseBody);
    }

    /**
     * Mock implementation of HttpURLConnection for testing.
     */
    private static class MockHttpURLConnection extends HttpURLConnection {
        private final int statusCode;
        private final String responseBody;
        private final Map<String, java.util.List<String>> headers;

        public MockHttpURLConnection(int statusCode, String responseBody) {
            super(null);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
            this.headers = new HashMap<>();
            this.headers.put("Content-Type", java.util.Arrays.asList("application/json"));
        }

        @Override
        public int getResponseCode() {
            return statusCode;
        }

        @Override
        public InputStream getInputStream() {
            if (statusCode >= 200 && statusCode < 300) {
                return new ByteArrayInputStream(responseBody.getBytes());
            }
            throw new RuntimeException("Error stream should be used for error responses");
        }

        @Override
        public InputStream getErrorStream() {
            if (statusCode >= 400) {
                return new ByteArrayInputStream(responseBody.getBytes());
            }
            return null;
        }

        @Override
        public Map<String, java.util.List<String>> getHeaderFields() {
            return headers;
        }

        @Override
        public void disconnect() {
            // No-op for mock
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
            // No-op for mock
        }
    }
}
