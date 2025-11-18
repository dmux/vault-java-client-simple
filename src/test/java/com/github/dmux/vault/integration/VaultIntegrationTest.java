package com.github.dmux.vault.integration;

import com.github.dmux.vault.VaultClient;
import com.github.dmux.vault.VaultConfig;
import com.github.dmux.vault.exception.VaultAuthenticationException;
import com.github.dmux.vault.exception.VaultException;
import com.github.dmux.vault.exception.VaultSecretNotFoundException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Integration tests for VaultClient that run against a real Vault server in Docker.
 * 
 * Prerequisites:
 * 1. Start Docker environment: docker-compose up -d
 * 2. Wait for Vault initialization to complete (check logs: docker-compose logs vault-init)
 * 3. Configure AWS credentials (required for AWS IAM authentication):
 *    - Set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables, OR
 *    - Configure ~/.aws/credentials file, OR
 *    - Use IAM instance profile (if running on EC2)
 * 4. Run tests: mvn test -Dtest=VaultIntegrationTest
 *    Or run with integration profile: mvn verify -Pintegration
 * 
 * The Docker environment includes:
 * - Vault server running on http://localhost:8200
 * - AWS auth method configured with test-role
 * - Test secrets pre-populated at secret/test/config and secret/test/versioned
 * 
 * Note: These tests use real AWS IAM authentication. The AWS credentials must be valid
 * for the STS GetCallerIdentity call to succeed. The Vault test-role is configured to
 * accept any IAM principal ARN (bound_iam_principal_arn="arn:aws:iam::*:role/*").
 * 
 * Requirements validated: 8.2, 8.3, 5.5
 */
@Category(IntegrationTest.class)
public class VaultIntegrationTest {

    // Use environment variable to allow running tests from host or inside Docker
    private static final String VAULT_URL = System.getenv("VAULT_ADDR") != null 
        ? System.getenv("VAULT_ADDR") 
        : "http://localhost:8200";
    private static final String AWS_REGION = "us-east-1";
    private static final String TEST_ROLE = "test-role";
    private static final String INVALID_ROLE = "non-existent-role";

    private VaultClient client;

    @BeforeClass
    public static void setUpClass() {
        // Verify Docker environment is running
        String vaultAddr = System.getenv("VAULT_ADDR");
        if (vaultAddr == null) {
            System.out.println("WARNING: VAULT_ADDR not set. Assuming Vault is running at " + VAULT_URL);
            System.out.println("To start the Docker environment, run: docker-compose up -d");
        }
        
        // Check for AWS credentials
        String awsAccessKey = System.getenv("AWS_ACCESS_KEY_ID");
        if (awsAccessKey == null || awsAccessKey.trim().isEmpty()) {
            System.out.println("\nWARNING: AWS credentials not found!");
            System.out.println("Integration tests require AWS credentials for IAM authentication.");
            System.out.println("\nTo set up dummy credentials for testing, run:");
            System.out.println("  ./docker/setup-test-credentials.sh");
            System.out.println("\nOr set environment variables:");
            System.out.println("  export AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE");
            System.out.println("  export AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            System.out.println("\nNote: Dummy credentials work because Vault's test-role accepts any IAM principal.\n");
        }
    }

    @Before
    public void setUp() {
        // Create a fresh client for each test
        VaultConfig config = new VaultConfig.Builder()
                .vaultUrl(VAULT_URL)
                .awsRegion(AWS_REGION)
                .awsRole(TEST_ROLE)
                .sslVerify(false) // Disable SSL verification for dev environment
                .connectionTimeout(10000)
                .readTimeout(10000)
                .build();

        client = new VaultClient(config);
    }

    /**
     * Test successful authentication with AWS IAM role.
     * Validates Requirement 1.1, 1.2, 1.5
     */
    @Test
    public void testAuthenticateSuccess() throws VaultException {
        // Verify client is not authenticated initially
        assertFalse("Client should not be authenticated initially", client.isAuthenticated());

        // Authenticate with Vault
        client.authenticate();

        // Verify client is now authenticated
        assertTrue("Client should be authenticated after authenticate() call", client.isAuthenticated());
    }

    /**
     * Test reading a secret successfully from Vault.
     * Validates that the secret contains expected fields (username and password).
     * Validates Requirements 2.1, 2.2, 2.5
     */
    @Test
    public void testReadSecretSuccess() throws VaultException {
        // Authenticate first
        client.authenticate();

        // Read the test secret
        Map<String, Object> result = client.readSecret("secret/test/config");

        // Verify the result structure
        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain 'data' key", result.containsKey("data"));
        assertTrue("Result should contain 'metadata' key", result.containsKey("metadata"));

        // Extract the secret data
        @SuppressWarnings("unchecked")
        Map<String, Object> secretData = (Map<String, Object>) result.get("data");

        // Verify expected fields exist
        assertTrue("Secret should contain 'username' field", secretData.containsKey("username"));
        assertTrue("Secret should contain 'password' field", secretData.containsKey("password"));

        // Verify expected values
        assertEquals("Username should match", "testuser", secretData.get("username"));
        assertEquals("Password should match", "testpass", secretData.get("password"));

        // Verify metadata contains version information
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
        assertNotNull("Metadata should not be null", metadata);
        assertTrue("Metadata should contain 'version' field", metadata.containsKey("version"));
        assertTrue("Metadata should contain 'created_time' field", metadata.containsKey("created_time"));
    }

    /**
     * Test reading a specific version of a secret.
     * Validates Requirement 2.6
     */
    @Test
    public void testReadSecretVersion() throws VaultException {
        // Authenticate first
        client.authenticate();

        // Read version 1 of the versioned secret
        Map<String, Object> result1 = client.readSecretVersion("secret/test/versioned", 1);
        assertNotNull("Result for version 1 should not be null", result1);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data1 = (Map<String, Object>) result1.get("data");
        assertEquals("Version 1 should have correct value", "version1", data1.get("value"));

        // Verify metadata shows version 1
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata1 = (Map<String, Object>) result1.get("metadata");
        assertEquals("Metadata should show version 1", 1, metadata1.get("version"));

        // Read version 2 of the versioned secret
        Map<String, Object> result2 = client.readSecretVersion("secret/test/versioned", 2);
        assertNotNull("Result for version 2 should not be null", result2);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data2 = (Map<String, Object>) result2.get("data");
        assertEquals("Version 2 should have correct value", "version2", data2.get("value"));

        // Verify metadata shows version 2
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata2 = (Map<String, Object>) result2.get("metadata");
        assertEquals("Metadata should show version 2", 2, metadata2.get("version"));

        // Read version 3 (latest) of the versioned secret
        Map<String, Object> result3 = client.readSecretVersion("secret/test/versioned", 3);
        assertNotNull("Result for version 3 should not be null", result3);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data3 = (Map<String, Object>) result3.get("data");
        assertEquals("Version 3 should have correct value", "version3", data3.get("value"));
    }

    /**
     * Test that reading a non-existent secret throws VaultSecretNotFoundException.
     * Validates Requirement 2.3, 7.2
     */
    @Test(expected = VaultSecretNotFoundException.class)
    public void testReadSecretNotFound() throws VaultException {
        // Authenticate first
        client.authenticate();

        // Try to read a secret that doesn't exist
        client.readSecret("secret/test/does-not-exist");

        // Should throw VaultSecretNotFoundException
        fail("Should have thrown VaultSecretNotFoundException");
    }

    /**
     * Test that authentication with an invalid role throws VaultAuthenticationException.
     * Validates Requirement 1.3, 7.2
     */
    @Test(expected = VaultAuthenticationException.class)
    public void testAuthenticationFailure() throws VaultException {
        // Create client with invalid role
        VaultConfig config = new VaultConfig.Builder()
                .vaultUrl(VAULT_URL)
                .awsRegion(AWS_REGION)
                .awsRole(INVALID_ROLE)
                .sslVerify(false)
                .build();

        VaultClient invalidClient = new VaultClient(config);

        // Try to authenticate with invalid role
        invalidClient.authenticate();

        // Should throw VaultAuthenticationException
        fail("Should have thrown VaultAuthenticationException");
    }

    /**
     * Test that attempting to read a secret without authentication throws VaultAuthenticationException.
     * Validates Requirement 2.4
     */
    @Test(expected = VaultAuthenticationException.class)
    public void testReadSecretWithoutAuthentication() throws VaultException {
        // Try to read secret without authenticating first
        client.readSecret("secret/test/config");

        // Should throw VaultAuthenticationException
        fail("Should have thrown VaultAuthenticationException for unauthenticated request");
    }
}
