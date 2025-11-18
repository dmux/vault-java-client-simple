#!/bin/sh
set -e

echo "Waiting for Vault to be ready..."
sleep 2

echo "Enabling AWS auth method..."
vault auth enable aws || echo "AWS auth already enabled"

echo "Configuring AWS auth..."
vault write auth/aws/config/client \
    iam_server_id_header_value=vault.example.com

echo "Creating test policy..."
vault policy write test-policy - <<EOF
path "secret/data/*" {
  capabilities = ["read", "list"]
}

path "secret/metadata/*" {
  capabilities = ["read", "list"]
}
EOF

echo "Creating AWS IAM role..."
vault write auth/aws/role/test-role \
    auth_type=iam \
    bound_iam_principal_arn="arn:aws:iam::*:role/*" \
    policies=test-policy \
    ttl=1h

echo "Enabling KV v2 secrets engine..."
vault secrets enable -version=2 -path=secret kv || echo "KV secrets engine already enabled"

echo "Writing test secrets..."
vault kv put secret/test/config \
    username=testuser \
    password=testpass \
    database_url="jdbc:postgresql://localhost:5432/testdb"

vault kv put secret/test/api \
    api_key=test-api-key-12345 \
    api_secret=test-api-secret-67890

vault kv put secret/myapp/config \
    app_name="My Application" \
    environment=development \
    debug=true

echo "Creating multiple versions of a secret for version testing..."
vault kv put secret/test/versioned value=version1
vault kv put secret/test/versioned value=version2
vault kv put secret/test/versioned value=version3

echo "Vault initialization complete!"
echo "Root token: root"
echo "Vault URL: http://localhost:8200"
echo "Test role: test-role"
