package com.github.dmux.vault;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for VaultConfig and its Builder.
 * Tests configuration validation, default values, and builder pattern.
 */
public class VaultConfigTest {

    @Test
    public void testBuilderWithAllParameters() {
        // Arrange & Act
        VaultConfig config = new VaultConfig.Builder()
                .vaultUrl("https://vault.example.com:8200")
                .awsRegion("us-west-2")
                .awsRole("my-app-role")
                .sslVerify(false)
                .connectionTimeout(3000)
                .readTimeout(8000)
                .build();

        // Assert
        assertEquals("https://vault.example.com:8200", config.getVaultUrl());
        assertEquals("us-west-2", config.getAwsRegion());
        assertEquals("my-app-role", config.getAwsRole());
        assertFalse(config.isSslVerify());
        assertEquals(3000, config.getConnectionTimeout());
        assertEquals(8000, config.getReadTimeout());
    }

    @Test
    public void testBuilderWithDefaultValues() {
        // Arrange & Act
        VaultConfig config = new VaultConfig.Builder()
                .vaultUrl("http://localhost:8200")
                .awsRegion("us-east-1")
                .awsRole("test-role")
                .build();

        // Assert
        assertEquals("http://localhost:8200", config.getVaultUrl());
        assertEquals("us-east-1", config.getAwsRegion());
        assertEquals("test-role", config.getAwsRole());
        assertTrue(config.isSslVerify()); // Default value
        assertEquals(5000, config.getConnectionTimeout()); // Default value
        assertEquals(10000, config.getReadTimeout()); // Default value
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderWithNullUrl() {
        // Act
        new VaultConfig.Builder()
                .vaultUrl(null)
                .awsRegion("us-east-1")
                .awsRole("test-role")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderWithEmptyUrl() {
        // Act
        new VaultConfig.Builder()
                .vaultUrl("")
                .awsRegion("us-east-1")
                .awsRole("test-role")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderWithWhitespaceUrl() {
        // Act
        new VaultConfig.Builder()
                .vaultUrl("   ")
                .awsRegion("us-east-1")
                .awsRole("test-role")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderWithInvalidUrlNoProtocol() {
        // Act
        new VaultConfig.Builder()
                .vaultUrl("vault.example.com:8200")
                .awsRegion("us-east-1")
                .awsRole("test-role")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderWithInvalidUrlWrongProtocol() {
        // Act
        new VaultConfig.Builder()
                .vaultUrl("ftp://vault.example.com:8200")
                .awsRegion("us-east-1")
                .awsRole("test-role")
                .build();
    }

    @Test
    public void testBuilderWithHttpUrl() {
        // Act
        VaultConfig config = new VaultConfig.Builder()
                .vaultUrl("http://localhost:8200")
                .awsRegion("us-east-1")
                .awsRole("test-role")
                .build();

        // Assert
        assertEquals("http://localhost:8200", config.getVaultUrl());
    }

    @Test
    public void testBuilderWithHttpsUrl() {
        // Act
        VaultConfig config = new VaultConfig.Builder()
                .vaultUrl("https://vault.example.com")
                .awsRegion("us-east-1")
                .awsRole("test-role")
                .build();

        // Assert
        assertEquals("https://vault.example.com", config.getVaultUrl());
    }

    @Test
    public void testBuilderWithMixedCaseUrl() {
        // Act
        VaultConfig config = new VaultConfig.Builder()
                .vaultUrl("HTTPS://VAULT.EXAMPLE.COM")
                .awsRegion("us-east-1")
                .awsRole("test-role")
                .build();

        // Assert
        assertEquals("HTTPS://VAULT.EXAMPLE.COM", config.getVaultUrl());
    }

    @Test
    public void testBuilderFluentInterface() {
        // Act
        VaultConfig.Builder builder = new VaultConfig.Builder();
        VaultConfig.Builder result = builder.vaultUrl("http://localhost:8200");

        // Assert - verify fluent interface returns builder
        assertSame(builder, result);
    }

    @Test
    public void testBuilderWithCustomTimeouts() {
        // Act
        VaultConfig config = new VaultConfig.Builder()
                .vaultUrl("http://localhost:8200")
                .awsRegion("us-east-1")
                .awsRole("test-role")
                .connectionTimeout(1000)
                .readTimeout(2000)
                .build();

        // Assert
        assertEquals(1000, config.getConnectionTimeout());
        assertEquals(2000, config.getReadTimeout());
    }

    @Test
    public void testBuilderWithSslVerifyTrue() {
        // Act
        VaultConfig config = new VaultConfig.Builder()
                .vaultUrl("https://vault.example.com")
                .awsRegion("us-east-1")
                .awsRole("test-role")
                .sslVerify(true)
                .build();

        // Assert
        assertTrue(config.isSslVerify());
    }

    @Test
    public void testBuilderWithSslVerifyFalse() {
        // Act
        VaultConfig config = new VaultConfig.Builder()
                .vaultUrl("http://localhost:8200")
                .awsRegion("us-east-1")
                .awsRole("test-role")
                .sslVerify(false)
                .build();

        // Assert
        assertFalse(config.isSslVerify());
    }
}
