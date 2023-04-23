package com.sc.hcv.auth;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

public class VaultAuthenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultAuthenticator.class);

    private final String vaultUrl;
    private RestTemplate restTemplate;
    private final int maxRetries;
    private final long backoffInterval;
    private final long maxBackoffInterval;


    public VaultAuthenticator(String vaultUrl, int maxRetries, long backoffInterval, long maxBackOffInterval) {
        this.vaultUrl = vaultUrl;
        this.maxRetries = maxRetries;
        this.backoffInterval = backoffInterval;
        this.maxBackoffInterval = maxBackOffInterval;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Authenticates to Vault using the AppRole authentication method.
     *
     * @param roleId   the Role ID for the AppRole
     * @param secretId the Secret ID for the AppRole
     * @return a string message indicating successful authentication
     * @throws Exception if the authentication fails after maxRetries attempts
     */

    public String authenticateAppRole(String roleId, String secretId, String authPath) throws Exception {
        int numRetries = 0;
        while (numRetries < maxRetries) {
            try {
                Map<String, String> requestMap = new HashMap<>();
                requestMap.put("role_id", roleId);
                requestMap.put("secret_id", secretId);

                ResponseEntity<String> response = restTemplate.postForEntity(vaultUrl + authPath, requestMap, String.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    LOGGER.info("Vault authentication successful");
                    return "Vault authentication successful";
                } else {
                    throw new Exception("Vault authentication failed");
                }
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    LOGGER.error("Invalid Vault token");
                    throw new Exception("Invalid Vault credentials");
                } else {
                    numRetries++;
                    LOGGER.warn("Retrying Vault authentication after {} milliseconds", backoffInterval * numRetries);
                    waitUntilConditionMet(backoffInterval * numRetries, maxBackoffInterval);
                }
            } catch (Exception ex) {
                numRetries++;
                LOGGER.warn("Retrying Vault authentication after {} milliseconds", backoffInterval * numRetries);
                waitUntilConditionMet(backoffInterval * numRetries, maxBackoffInterval);
            }
        }
        LOGGER.error("Max retries exceeded for Vault authentication");
        throw new Exception("Max retries exceeded for Vault authentication");
    }

    /**
     * Authenticates with Vault using TLS certificate and key authentication.
     *
     * @param keyStorePath      Path to the PKCS12 format key store containing the client certificate and key.
     * @param keyStorePassword  Password for the key store.
     * @param trustStorePath    Path to the JKS format trust store containing the trusted certificates.
     * @param trustStorePassword Password for the trust store.
     * @param authPath          Vault authentication endpoint.
     * @return                  Success message upon successful authentication.
     * @throws Exception        Throws exception upon failure to authenticate with Vault after maximum number of retries.
     */

    public String authenticateTLS(String keyStorePath, String keyStorePassword, String trustStorePath, String trustStorePassword, String authPath) throws Exception {
        int numRetries = 0;
        while (numRetries < maxRetries) {
            try {
                HttpHeaders headers = new HttpHeaders();
                RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(getHttpClient(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword)));

                HttpEntity<String> entity = new HttpEntity<String>(headers);
                ResponseEntity<String> response = restTemplate.exchange(vaultUrl + authPath, HttpMethod.GET, entity, String.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    LOGGER.info("Vault authentication successful");
                    return "Vault authentication successful";
                } else {
                    throw new Exception("Vault authentication failed");
                }
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    LOGGER.error("Invalid Vault token");
                    throw new Exception("Invalid Vault credentials");
                } else {
                    numRetries++;
                    LOGGER.warn("Retrying Vault authentication after {} milliseconds", backoffInterval * numRetries);
                    waitUntilConditionMet(backoffInterval * numRetries, maxBackoffInterval);
                }
            } catch (Exception ex) {
                numRetries++;
                LOGGER.warn("Retrying Vault authentication after {} milliseconds", backoffInterval * numRetries);
                waitUntilConditionMet(backoffInterval * numRetries, maxBackoffInterval);
            }
        }
        LOGGER.error("Max retries exceeded for Vault authentication");
        throw new Exception("Max retries exceeded for Vault authentication");
    }

    /**
     * This method creates and returns a CloseableHttpClient object for communicating with a remote server using TLS authentication.
     * @param keyStorePath the file path of the PKCS12 format key store containing the client certificate and private key
     * @param keyStorePassword the password for the key store
     * @param trustStorePath the file path of the JKS format trust store containing the trusted CA certificates
     * @param trustStorePassword the password for the trust store
     * @return a CloseableHttpClient object configured with the SSLContext and SSLConnectionSocketFactory for TLS authentication
     * @throws Exception if an error occurs while loading the key store or trust store, or creating the SSLContext
     */

    private static CloseableHttpClient getHttpClient(String keyStorePath, String keyStorePassword, String trustStorePath, String trustStorePassword) throws Exception {

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new FileInputStream(new File(keyStorePath)), keyStorePassword.toCharArray());

        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(new FileInputStream(new File(trustStorePath)), trustStorePassword.toCharArray());

        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(keyStore, keyStorePassword.toCharArray())
                .loadTrustMaterial(trustStore, null)
                .build();

        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext, new String[]{"TLSv1.2"}, null, SSLConnectionSocketFactory.getDefaultHostnameVerifier());

        return HttpClients.custom().setSSLContext(sslContext).build();
    }


    /**
     * Checks if the given Vault token is valid by performing a token lookup on the Vault server.
     * If the token is valid, returns true. If the token is invalid or lookup fails, returns false.
     *
     * @param vaultToken the Vault token to check for validity
     * @return true if the token is valid, false otherwise
     */

    public boolean isValidToken(String vaultToken) throws Exception {
        int numRetries = 0;
        while (numRetries < maxRetries) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(vaultToken);

                HttpEntity<String> entity = new HttpEntity<>(null, headers);

                ResponseEntity<String> response = restTemplate.exchange(vaultUrl + "/v1/auth/token/lookup-self", HttpMethod.GET, entity, String.class);

                return response.getStatusCode() == HttpStatus.OK;
            } catch (Exception ex) {
                numRetries++;
                Thread.sleep(backoffInterval * numRetries);
            }
        }
        throw new Exception("Max retries exceeded for checking token validity");
    }

    /**
     * This method will cause the thread to wait for a specific amount of time
     * or until the maximum wait time is reached, whichever comes first.
     *
     * @param waitTime    the time (in milliseconds) to wait between iterations
     * @param maxWaitTime the maximum time (in milliseconds) to wait for the condition to be met
     * @throws InterruptedException if the thread is interrupted while waiting
     */

    private void waitUntilConditionMet(long waitTime, long maxWaitTime) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            Thread.sleep(waitTime);
        }
    }

}
