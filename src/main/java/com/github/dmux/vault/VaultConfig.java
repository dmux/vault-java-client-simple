package com.github.dmux.vault;

/**
 * Immutable configuration object for VaultClient.
 * Use the Builder to create instances with custom settings.
 */
public class VaultConfig {
    private final String vaultUrl;
    private final String awsRegion;
    private final String awsRole;
    private final boolean sslVerify;
    private final int connectionTimeout;
    private final int readTimeout;

    private VaultConfig(Builder builder) {
        this.vaultUrl = builder.vaultUrl;
        this.awsRegion = builder.awsRegion;
        this.awsRole = builder.awsRole;
        this.sslVerify = builder.sslVerify;
        this.connectionTimeout = builder.connectionTimeout;
        this.readTimeout = builder.readTimeout;
    }

    public String getVaultUrl() {
        return vaultUrl;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public String getAwsRole() {
        return awsRole;
    }

    public boolean isSslVerify() {
        return sslVerify;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Builder for creating VaultConfig instances.
     */
    public static class Builder {
        private String vaultUrl;
        private String awsRegion;
        private String awsRole;
        private boolean sslVerify = true;
        private int connectionTimeout = 5000;
        private int readTimeout = 10000;

        public Builder vaultUrl(String vaultUrl) {
            this.vaultUrl = vaultUrl;
            return this;
        }

        public Builder awsRegion(String awsRegion) {
            this.awsRegion = awsRegion;
            return this;
        }

        public Builder awsRole(String awsRole) {
            this.awsRole = awsRole;
            return this;
        }

        public Builder sslVerify(boolean sslVerify) {
            this.sslVerify = sslVerify;
            return this;
        }

        public Builder connectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public VaultConfig build() {
            validateUrl(vaultUrl);
            return new VaultConfig(this);
        }

        private void validateUrl(String url) {
            if (url == null || url.trim().isEmpty()) {
                throw new IllegalArgumentException("Vault URL cannot be null or empty");
            }
            
            String lowerUrl = url.toLowerCase();
            if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
                throw new IllegalArgumentException("Vault URL must start with http:// or https://");
            }
        }
    }
}
