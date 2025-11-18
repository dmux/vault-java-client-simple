package com.github.dmux.vault;

import com.github.dmux.vault.exception.VaultAuthenticationException;
import com.github.dmux.vault.exception.VaultException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VaultClient.
 * Tests path building, secret reading, version reading, and authentication validation.
 * 
 * Requirements: 2.1, 2.2, 2.5, 2.6
 */
public class VaultClientTest {

    private VaultConfig config;
    private VaultClient client;
    private VaultHttpClient mockHttpClient;
    private AwsAuthenticator mockAuthenticator;

    @Before
    public void setUp() {
        config = new VaultConfig.Builder()
                .vaultUrl("http://localhost:8200")
                .awsRegion("us-east-1")
                .awsRole("test-role")
                .build();
        
        client = new VaultClient(config);
    }

    /**
     * Test buildKvV2Path with standard secret path.
     * Requirements: 2.5
     */
    @Test
    public void testBuildKvV2PathStandardPath() throws Exception {
        // Arrange
        String input = "secret/myapp/config";
        String expected = "/v1/secret/data/myapp/config";

        // Act
        String result = invokeBuildKvV2Path(input);

        // Assert
        assertEquals(expected, result);
    }

    /**
     * Test buildKvV2Path with leading slash.
     * Requirements: 2.5
     */
    @Test
    public void testBuildKvV2PathWithLeadingSlash() throws Exception {
        // Arrange
        String input = "/secret/myapp/config";
        String expected = "/v1/secret/data/myapp/config";

        // Act
        String result = invokeBuildKvV2Path(input);

        // Assert
        assertEquals(expected, result);
    }

    /**
     * Test buildKvV2Path with nested path.
     * Requirements: 2.5
     */
    @Test
    public void testBuildKvV2PathNestedPath() throws Exception {
        // Arrange
        String input = "secret/myapp/prod/database/config";
        String expected = "/v1/secret/data/myapp/prod/database/config";

        // Act
        String result = invokeBuildKvV2Path(input);

        // Assert
        assertEquals(expected, result);
    }

    /**
     * Test buildKvV2Path with different mount point.
     * Requirements: 2.5
     */
    @Test
    public void testBuildKvV2PathDifferentMount() throws Exception {
        // Arrange
        String input = "kv/application/settings";
        String expected = "/v1/kv/data/application/settings";

        // Act
        String result = invokeBuildKvV2Path(input);

        // Assert
        assertEquals(expected, result);
    }

    /**
     * Test buildKvV2Path with minimal valid path.
     * Requirements: 2.5
     */
    @Test
    public void testBuildKvV2PathMinimalPath() throws Exception {
        // Arrange
        String input = "secret/config";
        String expected = "/v1/secret/data/config";

        // Act
        String result = invokeBuildKvV2Path(input);

        // Assert
        assertEquals(expected, result);
    }

    /**
     * Test buildKvV2Path throws exception for null path.
     * Requirements: 2.5
     */
    @Test
    public void testBuildKvV2PathNullPath() {
        // Act & Assert
        try {
            invokeBuildKvV2Path(null);
            fail("Expected IllegalArgumentException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            assertTrue(e.getCause().getMessage().contains("cannot be null or empty"));
        }
    }

    /**
     * Test buildKvV2Path throws exception for empty path.
     * Requirements: 2.5
     */
    @Test
    public void testBuildKvV2PathEmptyPath() {
        // Act & Assert
        try {
            invokeBuildKvV2Path("");
            fail("Expected IllegalArgumentException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            assertTrue(e.getCause().getMessage().contains("cannot be null or empty"));
        }
    }

    /**
     * Test buildKvV2Path throws exception for whitespace path.
     * Requirements: 2.5
     */
    @Test
    public void testBuildKvV2PathWhitespacePath() {
        // Act & Assert
        try {
            invokeBuildKvV2Path("   ");
            fail("Expected IllegalArgumentException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            assertTrue(e.getCause().getMessage().contains("cannot be null or empty"));
        }
    }

    /**
     * Test buildKvV2Path throws exception for invalid path (no secret name).
     * Requirements: 2.5
     */
    @Test
    public void testBuildKvV2PathInvalidPathNoSecretName() {
        // Act & Assert
        try {
            invokeBuildKvV2Path("secret");
            fail("Expected IllegalArgumentException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            assertTrue(e.getCause().getMessage().contains("Invalid secret path"));
        }
    }

    /**
     * Test readSecret with successful response.
     * Requirements: 2.1, 2.2
     */
    @Test
    public void testReadSecretSuccess() throws Exception {
        // Arrange
        VaultClient clientWithMocks = createClientWithMocks();
        authenticateClient(clientWithMocks);

        String secretPath = "secret/myapp/config";
        String responseBody = createSecretResponseJson("testuser", "testpass", 1);
        VaultResponse mockResponse = new VaultResponse(200, responseBody, new HashMap<>());

        when(mockHttpClient.get(eq("/v1/secret/data/myapp/config"), anyString()))
                .thenReturn(mockResponse);

        // Act
        Map<String, Object> result = clientWithMocks.readSecret(secretPath);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("data"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertEquals("testuser", data.get("username"));
        assertEquals("testpass", data.get("password"));
        
        verify(mockHttpClient).get(eq("/v1/secret/data/myapp/config"), anyString());
    }

    /**
     * Test readSecret throws exception when not authenticated.
     * Requirements: 2.1
     */
    @Test(expected = VaultAuthenticationException.class)
    public void testReadSecretNotAuthenticated() throws Exception {
        // Arrange
        VaultClient clientWithMocks = createClientWithMocks();
        // Do not authenticate

        // Act
        clientWithMocks.readSecret("secret/myapp/config");
        
        // Assert - exception expected
    }

    /**
     * Test readSecretVersion with version parameter.
     * Requirements: 2.6
     */
    @Test
    public void testReadSecretVersionSuccess() throws Exception {
        // Arrange
        VaultClient clientWithMocks = createClientWithMocks();
        authenticateClient(clientWithMocks);

        String secretPath = "secret/myapp/config";
        int version = 3;
        String responseBody = createSecretResponseJson("olduser", "oldpass", version);
        VaultResponse mockResponse = new VaultResponse(200, responseBody, new HashMap<>());

        when(mockHttpClient.get(eq("/v1/secret/data/myapp/config?version=3"), anyString()))
                .thenReturn(mockResponse);

        // Act
        Map<String, Object> result = clientWithMocks.readSecretVersion(secretPath, version);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("data"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertEquals("olduser", data.get("username"));
        assertEquals("oldpass", data.get("password"));
        
        verify(mockHttpClient).get(eq("/v1/secret/data/myapp/config?version=3"), anyString());
    }

    /**
     * Test readSecretVersion with different version numbers.
     * Requirements: 2.6
     */
    @Test
    public void testReadSecretVersionDifferentVersions() throws Exception {
        // Arrange
        VaultClient clientWithMocks = createClientWithMocks();
        authenticateClient(clientWithMocks);

        String secretPath = "secret/myapp/config";
        
        // Test version 1
        String responseBody1 = createSecretResponseJson("user1", "pass1", 1);
        VaultResponse mockResponse1 = new VaultResponse(200, responseBody1, new HashMap<>());
        when(mockHttpClient.get(eq("/v1/secret/data/myapp/config?version=1"), anyString()))
                .thenReturn(mockResponse1);

        // Test version 5
        String responseBody5 = createSecretResponseJson("user5", "pass5", 5);
        VaultResponse mockResponse5 = new VaultResponse(200, responseBody5, new HashMap<>());
        when(mockHttpClient.get(eq("/v1/secret/data/myapp/config?version=5"), anyString()))
                .thenReturn(mockResponse5);

        // Act & Assert version 1
        Map<String, Object> result1 = clientWithMocks.readSecretVersion(secretPath, 1);
        @SuppressWarnings("unchecked")
        Map<String, Object> data1 = (Map<String, Object>) result1.get("data");
        assertEquals("user1", data1.get("username"));

        // Act & Assert version 5
        Map<String, Object> result5 = clientWithMocks.readSecretVersion(secretPath, 5);
        @SuppressWarnings("unchecked")
        Map<String, Object> data5 = (Map<String, Object>) result5.get("data");
        assertEquals("user5", data5.get("username"));
        
        verify(mockHttpClient).get(eq("/v1/secret/data/myapp/config?version=1"), anyString());
        verify(mockHttpClient).get(eq("/v1/secret/data/myapp/config?version=5"), anyString());
    }

    /**
     * Test readSecretVersion throws exception when not authenticated.
     * Requirements: 2.6
     */
    @Test(expected = VaultAuthenticationException.class)
    public void testReadSecretVersionNotAuthenticated() throws Exception {
        // Arrange
        VaultClient clientWithMocks = createClientWithMocks();
        // Do not authenticate

        // Act
        clientWithMocks.readSecretVersion("secret/myapp/config", 2);
        
        // Assert - exception expected
    }

    /**
     * Test isAuthenticated returns false before authentication.
     * Requirements: 2.1
     */
    @Test
    public void testIsAuthenticatedBeforeAuth() {
        // Act
        boolean result = client.isAuthenticated();

        // Assert
        assertFalse(result);
    }

    /**
     * Test isAuthenticated returns true after authentication.
     * Requirements: 2.1
     */
    @Test
    public void testIsAuthenticatedAfterAuth() throws Exception {
        // Arrange
        VaultClient clientWithMocks = createClientWithMocks();
        authenticateClient(clientWithMocks);

        // Act
        boolean result = clientWithMocks.isAuthenticated();

        // Assert
        assertTrue(result);
    }

    /**
     * Test readSecret with metadata in response.
     * Requirements: 2.2
     */
    @Test
    public void testReadSecretWithMetadata() throws Exception {
        // Arrange
        VaultClient clientWithMocks = createClientWithMocks();
        authenticateClient(clientWithMocks);

        String secretPath = "secret/myapp/config";
        String responseBody = createSecretResponseJsonWithMetadata("testuser", "testpass", 2, "2024-01-01T00:00:00Z");
        VaultResponse mockResponse = new VaultResponse(200, responseBody, new HashMap<>());

        when(mockHttpClient.get(eq("/v1/secret/data/myapp/config"), anyString()))
                .thenReturn(mockResponse);

        // Act
        Map<String, Object> result = clientWithMocks.readSecret(secretPath);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("data"));
        assertTrue(result.containsKey("metadata"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
        assertEquals(2, metadata.get("version"));
        assertEquals("2024-01-01T00:00:00Z", metadata.get("created_time"));
    }

    // Helper methods

    /**
     * Creates a VaultClient with mocked dependencies for testing.
     */
    private VaultClient createClientWithMocks() throws Exception {
        mockHttpClient = mock(VaultHttpClient.class);
        mockAuthenticator = mock(AwsAuthenticator.class);
        
        VaultClient client = new VaultClient(config);
        
        // Use reflection to inject mocks
        java.lang.reflect.Field httpClientField = VaultClient.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(client, mockHttpClient);
        
        java.lang.reflect.Field authenticatorField = VaultClient.class.getDeclaredField("authenticator");
        authenticatorField.setAccessible(true);
        authenticatorField.set(client, mockAuthenticator);
        
        return client;
    }

    /**
     * Authenticates the client by setting a mock token.
     */
    private void authenticateClient(VaultClient client) throws Exception {
        when(mockAuthenticator.authenticate()).thenReturn("mock-token-12345");
        client.authenticate();
    }

    /**
     * Invokes the private buildKvV2Path method using reflection.
     */
    private String invokeBuildKvV2Path(String path) throws Exception {
        Method method = VaultClient.class.getDeclaredMethod("buildKvV2Path", String.class);
        method.setAccessible(true);
        return (String) method.invoke(client, path);
    }

    /**
     * Creates a JSON response body for a secret.
     */
    private String createSecretResponseJson(String username, String password, int version) {
        return String.format(
            "{\"data\":{\"data\":{\"username\":\"%s\",\"password\":\"%s\"},\"metadata\":{\"version\":%d}}}",
            username, password, version
        );
    }

    /**
     * Creates a JSON response body for a secret with full metadata.
     */
    private String createSecretResponseJsonWithMetadata(String username, String password, int version, String createdTime) {
        return String.format(
            "{\"data\":{\"data\":{\"username\":\"%s\",\"password\":\"%s\"},\"metadata\":{\"version\":%d,\"created_time\":\"%s\"}}}",
            username, password, version, createdTime
        );
    }
}
