package com.github.dmux.vault;

import com.amazonaws.DefaultRequest;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.github.dmux.vault.exception.VaultAuthenticationException;
import com.github.dmux.vault.exception.VaultException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Handles AWS IAM authentication with HashiCorp Vault.
 * Uses AWS STS GetCallerIdentity request signed with SigV4 to authenticate.
 */
public class AwsAuthenticator {
    private final VaultConfig config;
    private final VaultHttpClient httpClient;
    private final AWSSecurityTokenService stsClient;
    private final AWSCredentialsProvider credentialsProvider;

    /**
     * Constructs an AwsAuthenticator with the given configuration and HTTP client.
     *
     * @param config Vault configuration containing AWS region and role
     * @param httpClient HTTP client for communicating with Vault
     */
    public AwsAuthenticator(VaultConfig config, VaultHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
        
        // Create STS client using default credentials provider chain
        this.credentialsProvider = new DefaultAWSCredentialsProviderChain();
        this.stsClient = AWSSecurityTokenServiceClientBuilder.standard()
                .withRegion(config.getAwsRegion())
                .withCredentials(credentialsProvider)
                .build();
    }

    /**
     * Authenticates with Vault using AWS IAM credentials.
     * Creates a signed STS GetCallerIdentity request and sends it to Vault's AWS auth endpoint.
     *
     * @return Vault authentication token
     * @throws VaultException if authentication fails
     */
    public String authenticate() throws VaultException {
        try {
            // Generate the IAM request body with signed STS request
            String requestBody = generateIamRequestBody();
            
            // Send authentication request to Vault
            VaultResponse response = httpClient.post("/v1/auth/aws/login", requestBody, null);
            
            // Parse response and extract token
            Map<String, Object> responseData = response.parseJson();
            
            if (!responseData.containsKey("auth")) {
                throw new VaultAuthenticationException(
                    "Invalid authentication response: missing 'auth' field");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> auth = (Map<String, Object>) responseData.get("auth");
            
            if (!auth.containsKey("client_token")) {
                throw new VaultAuthenticationException(
                    "Invalid authentication response: missing 'client_token' field");
            }
            
            String token = (String) auth.get("client_token");
            
            if (token == null || token.trim().isEmpty()) {
                throw new VaultAuthenticationException(
                    "Invalid authentication response: empty client_token");
            }
            
            return token;
            
        } catch (VaultAuthenticationException e) {
            throw e;
        } catch (VaultException e) {
            throw new VaultAuthenticationException(
                "AWS authentication failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new VaultAuthenticationException(
                "Unexpected error during AWS authentication: " + e.getMessage(), e);
        }
    }

    /**
     * Generates the IAM request body for Vault authentication.
     * Creates a signed STS GetCallerIdentity request and encodes it in the format expected by Vault.
     *
     * @return JSON string containing the authentication request
     * @throws VaultException if request generation fails
     */
    private String generateIamRequestBody() throws VaultException {
        try {
            // Create a GetCallerIdentity request
            GetCallerIdentityRequest callerIdentityRequest = new GetCallerIdentityRequest();
            
            // Create a DefaultRequest for signing
            DefaultRequest<GetCallerIdentityRequest> request = new DefaultRequest<>(
                callerIdentityRequest, "sts");
            
            // Set up the request
            request.setHttpMethod(HttpMethodName.POST);
            request.setEndpoint(URI.create("https://sts." + config.getAwsRegion() + ".amazonaws.com"));
            request.setResourcePath("/");
            
            // Add required parameters for GetCallerIdentity
            request.addParameter("Action", "GetCallerIdentity");
            request.addParameter("Version", "2011-06-15");
            
            // Add empty content for POST request
            byte[] content = new byte[0];
            request.setContent(new ByteArrayInputStream(content));
            request.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            request.addHeader("Content-Length", String.valueOf(content.length));
            
            // Sign the request using AWS4Signer
            AWS4Signer signer = new AWS4Signer();
            signer.setServiceName("sts");
            signer.setRegionName(config.getAwsRegion());
            signer.sign(request, credentialsProvider.getCredentials());
            
            // Build the request URL with query parameters
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(request.getEndpoint().toString());
            urlBuilder.append(request.getResourcePath());
            
            if (!request.getParameters().isEmpty()) {
                urlBuilder.append("?");
                boolean first = true;
                for (Map.Entry<String, java.util.List<String>> entry : request.getParameters().entrySet()) {
                    for (String value : entry.getValue()) {
                        if (!first) {
                            urlBuilder.append("&");
                        }
                        urlBuilder.append(entry.getKey()).append("=").append(value);
                        first = false;
                    }
                }
            }
            
            String requestUrl = urlBuilder.toString();
            
            // Build request body (empty for GetCallerIdentity)
            String requestBodyStr = "";
            
            // Build request headers as JSON string
            JSONObject headersJson = new JSONObject();
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                headersJson.put(header.getKey(), header.getValue());
            }
            String requestHeaders = headersJson.toString();
            
            // Base64 encode the components
            String encodedUrl = Base64.getEncoder().encodeToString(
                requestUrl.getBytes(StandardCharsets.UTF_8));
            String encodedBody = Base64.getEncoder().encodeToString(
                requestBodyStr.getBytes(StandardCharsets.UTF_8));
            String encodedHeaders = Base64.getEncoder().encodeToString(
                requestHeaders.getBytes(StandardCharsets.UTF_8));
            
            // Build the final JSON payload for Vault
            JSONObject payload = new JSONObject();
            payload.put("role", config.getAwsRole());
            payload.put("iam_http_request_method", "POST");
            payload.put("iam_request_url", encodedUrl);
            payload.put("iam_request_body", encodedBody);
            payload.put("iam_request_headers", encodedHeaders);
            
            return payload.toString();
            
        } catch (Exception e) {
            throw new VaultException(
                "Failed to generate IAM request body: " + e.getMessage(), e);
        }
    }
}
