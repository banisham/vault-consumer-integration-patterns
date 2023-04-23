package com.sc.hcv.secrets;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class VaultStaticSecrets {

    private static final Logger LOGGER = LoggerFactory.getLogger(com.sc.hcv.secrets.VaultStaticSecrets.class);

    private final String vaultUrl;
    private RestTemplate restTemplate;
    private final int maxRetries;
    private final long backoffInterval;
    private final long maxBackoffInterval;


    public VaultStaticSecrets(String vaultUrl, int maxRetries, long backoffInterval, long maxBackOffInterval) {
        this.vaultUrl = vaultUrl;
        this.maxRetries = maxRetries;
        this.backoffInterval = backoffInterval;
        this.maxBackoffInterval = maxBackOffInterval;
        this.restTemplate = new RestTemplate();
    }


    /**
     * Retrieves multiple secrets from a Vault key-value (KV) path, using the provided Vault token for authentication.
     * The method returns a Map containing the secret key-value pairs.
     * @param vaultToken The Vault token used for authentication.
     * @param secretPath The path of the KV engine where the secrets are stored.
     * @param secretKeys A list of secret keys to retrieve.
     * @return A Map containing the secret key-value pairs.
     * @throws Exception If the authentication fails or the secrets cannot be retrieved after the maximum number of retries.
     */

    public Map<String, String> readKVSecrets(String vaultToken, String secretPath, String... secretKeys) throws Exception {
        int numRetries = 0;

        // Retry logic is implemented here, similar to AuthN
        while (numRetries < maxRetries) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(vaultToken);
                HttpEntity<String> entity = new HttpEntity<>(null, headers);
                ResponseEntity<String> response = restTemplate.exchange(vaultUrl + "/v1/" + secretPath, HttpMethod.GET, entity, String.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    JSONObject jsonResponse = new JSONObject(response.getBody());
                    JSONObject data = jsonResponse.getJSONObject("data");
                    Map<String, String> secrets = new HashMap<>();
                    for (String secretKey : secretKeys) {
                        secrets.put(secretKey, data.getString(secretKey));
                    }
                    LOGGER.info("Reading secrets from Key-Value: {} successful", secretPath);
                    return secrets;
                } else {
                    LOGGER.error("Error reading secrets from Key-Value: {}", secretPath);
                    throw new Exception("Error reading secrets from Key-Value");
                }
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    LOGGER.error("Invalid Vault token");
                    throw new Exception("Invalid Vault token");
                } else {
                    numRetries++;
                    LOGGER.warn("Retrying reading secrets from Key-Value after {} milliseconds", backoffInterval * numRetries);
                    waitUntilConditionMet(backoffInterval * numRetries, maxBackoffInterval);
                }
            } catch (Exception ex) {
                numRetries++;
                LOGGER.warn("Retrying reading secrets from Key-Value after {} milliseconds", backoffInterval * numRetries);
                waitUntilConditionMet(backoffInterval * numRetries, maxBackoffInterval);
            }
        }
        LOGGER.error("Max retries exceeded for reading secrets from Key-Value: {}", secretPath);
        throw new Exception("Max retries exceeded for reading secrets from Key-Value");
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

