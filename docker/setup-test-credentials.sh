#!/bin/bash
# Script to set up dummy AWS credentials for integration testing
# These credentials are only used for signing requests - Vault's test-role
# is configured to accept any IAM principal ARN

set -e

echo "Setting up dummy AWS credentials for integration testing..."

# Create .aws directory if it doesn't exist
mkdir -p ~/.aws

# Create credentials file with dummy values
cat > ~/.aws/credentials <<EOF
[default]
aws_access_key_id = AKIAIOSFODNN7EXAMPLE
aws_secret_access_key = wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
EOF

# Create config file
cat > ~/.aws/config <<EOF
[default]
region = us-east-1
output = json
EOF

echo "✓ AWS credentials configured at ~/.aws/credentials"
echo "✓ AWS config configured at ~/.aws/config"
echo ""
echo "Note: These are dummy credentials for testing purposes only."
echo "The Vault test-role is configured to accept any IAM principal ARN."
echo ""
echo "You can now run integration tests with:"
echo "  docker compose exec maven mvn test -Dtest=VaultIntegrationTest"
