# Java Vault Client

[![Build](https://github.com/dmux/vault-java-client-simple/actions/workflows/build.yml/badge.svg)](https://github.com/dmux/vault-java-client-simple/actions/workflows/build.yml)
[![GitHub release](https://img.shields.io/github/v/release/dmux/vault-java-client-simple)](https://github.com/dmux/vault-java-client-simple/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A lightweight Java 8 library for interacting with HashiCorp Vault using AWS IAM authentication. Inspired by the Python hvac client, this library provides simple yet complete functionality for authenticating via AWS IAM roles and retrieving secrets from Vault's KV v2 secrets engine.

## Features

- **AWS IAM Authentication**: Authenticate to Vault using AWS IAM roles without static credentials
- **KV v2 Secrets Engine Support**: Read secrets with versioning support
- **Minimal Dependencies**: Uses only AWS SDK 1.x and org.json for JSON parsing
- **Java 8 Compatible**: Works with Java 8 and higher
- **Comprehensive Error Handling**: Specific exceptions for different failure scenarios
- **Docker Development Environment**: Includes Docker setup for local development and testing

## Requirements

- Java 8 or higher
- Maven 3.6+
- Docker and Docker Compose (for local development and testing)
- AWS credentials configured (environment variables, instance profile, or AWS credentials file)

## Installation

### Option 1: Direct Download from GitHub Releases

Download the JAR from the [releases page](https://github.com/dmux/vault-java-client-simple/releases):

```bash
# Download latest version
wget https://github.com/dmux/vault-java-client-simple/releases/download/v1.0.0/java-vault-client-1.0.0.jar

# Use in classpath
java -cp java-vault-client-1.0.0.jar:your-app.jar com.example.Main
```

### Option 2: Local Maven Repository

Install the JAR to your local Maven repository:

```bash
mvn install:install-file \
  -Dfile=java-vault-client-1.0.0.jar \
  -DgroupId=com.github.dmux \
  -DartifactId=java-vault-client \
  -Dversion=1.0.0 \
  -Dpackaging=jar
```

Then add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.github.dmux</groupId>
    <artifactId>java-vault-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Option 3: Build from Source

```bash
git clone https://github.com/dmux/vault-java-client-simple.git
cd vault-java-client-simple
mvn clean package
# JAR will be in target/java-vault-client-1.0.0-SNAPSHOT.jar
```

## Quick Start

### Basic Authentication and Secret Retrieval

```java
import com.github.dmux.vault.VaultClient;
import com.github.dmux.vault.VaultConfig;
import com.github.dmux.vault.exception.VaultException;
import java.util.Map;

public class Example {
    public static void main(String[] args) {
        try {
            // Create client configuration
            VaultConfig config = new VaultConfig.Builder()
                .vaultUrl("http://localhost:8200")
                .awsRegion("us-east-1")
                .awsRole("my-app-role")
                .build();

            // Create client and authenticate
            VaultClient client = new VaultClient(config);
            client.authenticate();

            // Read secret from KV v2
            Map<String, Object> secret = client.readSecret("secret/myapp/config");
            String username = (String) secret.get("username");
            String password = (String) secret.get("password");

            System.out.println("Username: " + username);
            System.out.println("Password: " + password);

        } catch (VaultException e) {
            System.err.println("Vault error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### Reading a Specific Secret Version

```java
import com.github.dmux.vault.VaultClient;
import com.github.dmux.vault.VaultConfig;
import java.util.Map;

// ... configuration and authentication ...

// Read version 3 of the secret
Map<String, Object> secret = client.readSecretVersion("secret/myapp/config", 3);
String username = (String) secret.get("username");

System.out.println("Username from version 3: " + username);
```

## Configuration

The `VaultConfig.Builder` supports the following configuration parameters:

| Parameter           | Type    | Required | Default | Description                                                                                   |
| ------------------- | ------- | -------- | ------- | --------------------------------------------------------------------------------------------- |
| `vaultUrl`          | String  | Yes      | -       | URL of the Vault server (e.g., `http://localhost:8200` or `https://vault.example.com`)        |
| `awsRegion`         | String  | Yes      | -       | AWS region for IAM authentication (e.g., `us-east-1`)                                         |
| `awsRole`           | String  | Yes      | -       | Vault role name configured for AWS IAM authentication                                         |
| `sslVerify`         | boolean | No       | `true`  | Enable/disable SSL certificate verification. Set to `false` only for development environments |
| `connectionTimeout` | int     | No       | `5000`  | Connection timeout in milliseconds                                                            |
| `readTimeout`       | int     | No       | `10000` | Read timeout in milliseconds                                                                  |

### Configuration Example

```java
VaultConfig config = new VaultConfig.Builder()
    .vaultUrl("https://vault.example.com")
    .awsRegion("us-west-2")
    .awsRole("production-app-role")
    .sslVerify(true)
    .connectionTimeout(10000)
    .readTimeout(30000)
    .build();
```

### Development Configuration (Disable SSL Verification)

```java
VaultConfig config = new VaultConfig.Builder()
    .vaultUrl("http://localhost:8200")
    .awsRegion("us-east-1")
    .awsRole("dev-role")
    .sslVerify(false)  // Only for local development!
    .build();
```

## Error Handling

The library provides specific exception types for different failure scenarios. All exceptions extend `VaultException`.

### Exception Types

| Exception                      | When It's Thrown                                                            | HTTP Status |
| ------------------------------ | --------------------------------------------------------------------------- | ----------- |
| `VaultAuthenticationException` | Authentication fails due to invalid credentials or insufficient permissions | 401, 403    |
| `VaultSecretNotFoundException` | The requested secret path does not exist                                    | 404         |
| `VaultConnectionException`     | Network connectivity issues or timeouts occur                               | -           |
| `VaultServerException`         | Vault server returns an internal error                                      | 5xx         |
| `VaultException`               | Base exception for all other Vault-related errors                           | Various     |

### Comprehensive Error Handling Example

```java
import com.github.dmux.vault.VaultClient;
import com.github.dmux.vault.VaultConfig;
import com.github.dmux.vault.exception.*;
import java.util.Map;

public class ErrorHandlingExample {
    public static void main(String[] args) {
        VaultConfig config = new VaultConfig.Builder()
            .vaultUrl("http://localhost:8200")
            .awsRegion("us-east-1")
            .awsRole("my-app-role")
            .build();

        VaultClient client = new VaultClient(config);

        try {
            // Authenticate with Vault
            client.authenticate();

            // Read secret
            Map<String, Object> secret = client.readSecret("secret/myapp/config");
            String apiKey = (String) secret.get("api_key");

            // Use the secret
            System.out.println("Successfully retrieved API key");

        } catch (VaultAuthenticationException e) {
            // Handle authentication failures
            System.err.println("Failed to authenticate with Vault: " + e.getMessage());
            System.err.println("Check your AWS credentials and IAM role configuration");
            // Implement retry logic or fallback mechanism

        } catch (VaultSecretNotFoundException e) {
            // Handle missing secrets
            System.err.println("Secret not found: " + e.getMessage());
            System.err.println("Verify the secret path exists in Vault");
            // Use default values or fail gracefully

        } catch (VaultConnectionException e) {
            // Handle network issues
            System.err.println("Cannot connect to Vault: " + e.getMessage());
            System.err.println("Check network connectivity and Vault server status");
            // Implement retry with exponential backoff

        } catch (VaultServerException e) {
            // Handle Vault server errors
            System.err.println("Vault server error: " + e.getMessage());
            System.err.println("The Vault server encountered an internal error");
            // Log for monitoring and retry later

        } catch (VaultException e) {
            // Handle any other Vault errors
            System.err.println("Unexpected Vault error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

## API Reference

### VaultClient

#### Constructor

```java
public VaultClient(VaultConfig config)
```

Creates a new VaultClient instance with the specified configuration.

#### Methods

##### authenticate()

```java
public void authenticate() throws VaultException
```

Authenticates with Vault using AWS IAM credentials. Must be called before reading secrets.

**Throws:**

- `VaultAuthenticationException` - if authentication fails
- `VaultConnectionException` - if network error occurs
- `VaultException` - for other errors

##### isAuthenticated()

```java
public boolean isAuthenticated()
```

Returns `true` if the client has successfully authenticated and has a valid token.

##### readSecret(String path)

```java
public Map<String, Object> readSecret(String path) throws VaultException
```

Reads the latest version of a secret from the specified path.

**Parameters:**

- `path` - Secret path in format `mount/secret/path` (e.g., `secret/myapp/config`)

**Returns:** Map containing the secret data

**Throws:**

- `VaultAuthenticationException` - if not authenticated or token is invalid
- `VaultSecretNotFoundException` - if secret doesn't exist
- `VaultConnectionException` - if network error occurs
- `VaultException` - for other errors

##### readSecretVersion(String path, int version)

```java
public Map<String, Object> readSecretVersion(String path, int version) throws VaultException
```

Reads a specific version of a secret from the specified path.

**Parameters:**

- `path` - Secret path in format `mount/secret/path`
- `version` - Version number to retrieve

**Returns:** Map containing the secret data for the specified version

**Throws:** Same exceptions as `readSecret()`

## Development

### Setting Up the Development Environment

This project includes a Docker Compose setup for running Vault locally with AWS authentication configured.

#### Start Vault Server

```bash
# Start Vault in development mode
docker-compose up -d

# Wait a few seconds for Vault to initialize
sleep 5

# Verify Vault is running
docker-compose ps
```

The Vault server will be available at `http://localhost:8200` with root token `root`.

#### Stop Vault Server

```bash
docker-compose down
```

### Building the Project

```bash
# Clean and build the project
mvn clean install

# Build without running tests
mvn clean install -DskipTests

# Run only unit tests
mvn test

# Run integration tests (requires Docker)
mvn test -Pintegration
```

### Running Tests

#### Unit Tests

Unit tests use Mockito and don't require external dependencies:

```bash
mvn test
```

#### Integration Tests

Integration tests run against a real Vault server in Docker:

```bash
# Start Vault
docker-compose up -d

# Run integration tests
mvn test -Pintegration

# Stop Vault
docker-compose down
```

### Project Structure

```txt
java-vault-client/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── github/
│   │               └── dmux/
│   │                   └── vault/
│   │                       ├── VaultClient.java
│   │                       ├── VaultConfig.java
│   │                       ├── AwsAuthenticator.java
│   │                       ├── VaultHttpClient.java
│   │                       ├── VaultResponse.java
│   │                       └── exception/
│   │                           ├── VaultException.java
│   │                           ├── VaultAuthenticationException.java
│   │                           ├── VaultSecretNotFoundException.java
│   │                           ├── VaultConnectionException.java
│   │                           └── VaultServerException.java
│   └── test/
│       └── java/
│           └── com/
│               └── github/
│                   └── dmux/
│                       └── vault/
│                           ├── VaultClientTest.java
│                           ├── VaultConfigTest.java
│                           ├── AwsAuthenticatorTest.java
│                           ├── VaultHttpClientTest.java
│                           └── integration/
│                               └── VaultIntegrationTest.java
├── docker/
│   ├── vault-init.sh
│   └── setup-test-credentials.sh
├── docker-compose.yml
├── pom.xml
└── README.md
```

## AWS Credentials

The library uses the AWS SDK's default credential provider chain, which looks for credentials in the following order:

1. **Environment variables**: `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`
2. **AWS credentials file**: `~/.aws/credentials`
3. **IAM instance profile**: For EC2 instances with attached IAM roles
4. **ECS container credentials**: For ECS tasks with IAM roles
5. **Assume role**: If configured in AWS credentials

### Setting Up AWS Credentials for Development

```bash
# Option 1: Environment variables
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_SESSION_TOKEN=your_session_token  # Optional, for temporary credentials

# Option 2: AWS credentials file
aws configure
```

## Vault Configuration

### Enabling AWS Auth Method

```bash
# Enable AWS auth
vault auth enable aws

# Configure AWS auth
vault write auth/aws/config/client \
    iam_server_id_header_value=vault.example.com

# Create a policy
vault policy write myapp-policy - <<EOF
path "secret/data/myapp/*" {
  capabilities = ["read"]
}
EOF

# Create a role
vault write auth/aws/role/my-app-role \
    auth_type=iam \
    bound_iam_principal_arn="arn:aws:iam::123456789012:role/MyAppRole" \
    policies=myapp-policy \
    ttl=1h
```

## Security Considerations

1. **SSL Verification**: Always keep `sslVerify=true` in production environments
2. **Token Storage**: Tokens are stored in memory only and are not persisted
3. **Credential Management**: Use AWS IAM roles and instance profiles instead of static credentials
4. **Timeout Configuration**: Configure appropriate timeouts to prevent hanging connections
5. **Error Messages**: The library preserves original error messages from Vault for debugging

## Limitations

- Token renewal is not automatic; applications must handle re-authentication
- Only supports KV v2 secrets engine (KV v1 not supported)
- Only supports AWS IAM authentication method
- Read-only operations (write operations not implemented)
- Synchronous operations only (no async support)

## Troubleshooting

### Authentication Fails with "permission denied"

- Verify your AWS credentials are valid: `aws sts get-caller-identity`
- Check that the Vault role's `bound_iam_principal_arn` matches your IAM principal
- Ensure the role has the correct policies attached

### Secret Not Found (404)

- Verify the secret exists: `vault kv get secret/myapp/config`
- Check the secret path format (should be `mount/path`, not `/v1/mount/data/path`)
- Ensure your Vault policy grants read access to the secret path

### Connection Timeout

- Verify Vault server is accessible: `curl http://localhost:8200/v1/sys/health`
- Check network connectivity and firewall rules
- Increase connection timeout in configuration if needed

### SSL Certificate Verification Failed

- For production: Ensure Vault has a valid SSL certificate
- For development: Set `sslVerify(false)` in configuration (not recommended for production)

## CI/CD

This project uses GitHub Actions for automation:

- **Build & Test**: Automatic tests on every push/PR
- **Artifacts**: JARs automatically available
- **Releases**: Automatic release creation via tags

**How to create a release**:

```bash
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
```

For more details, see [Pipeline Guide](.github/PIPELINE.md)

## Contributing

Contributions are welcome! Please ensure:

1. All tests pass: `mvn test`
2. Code follows existing style conventions
3. New features include unit tests
4. Documentation is updated

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For issues and questions:

- GitHub Issues: <https://github.com/dmux/vault-java-client-simple/issues>
- GitHub Repository: <https://github.com/dmux/vault-java-client-simple>
