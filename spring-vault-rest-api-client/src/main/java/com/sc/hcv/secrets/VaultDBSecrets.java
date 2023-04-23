package com.sc.hcv.secrets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class VaultDBSecrets {

    private static final Logger LOGGER = LoggerFactory.getLogger(com.sc.hcv.secrets.VaultDBSecrets.class);

    private final String vaultUrl;
    private RestTemplate restTemplate;
    private final int maxRetries;
    private final long backoffInterval;
    private final long maxBackoffInterval;


    public VaultDBSecrets(String vaultUrl, int maxRetries, long backoffInterval, long maxBackOffInterval) {
        this.vaultUrl = vaultUrl;
        this.maxRetries = maxRetries;
        this.backoffInterval = backoffInterval;
        this.maxBackoffInterval = maxBackOffInterval;
        this.restTemplate = new RestTemplate();
    }


    /**
     * Retrieves a database secret from Vault using the Vault token and the database path.
     * @param vaultToken the Vault token to use for authentication
     * @param databasePath the path to the database secret engine in Vault
     * @return a Map containing the database secret data
     * @throws Exception if there is an error retrieving the database secret from Vault or if the maximum number of retries is exceeded
     */

    public Map<String, String> readDatabaseSecret(String vaultToken, String databasePath) throws Exception {
        int numRetries = 0;
        while (numRetries < maxRetries) {
            try {
                Map<String, String> requestMap = new HashMap<>();
                requestMap.put("token", vaultToken);

                ResponseEntity<Map> response = restTemplate.getForEntity(vaultUrl + "/v1/" + databasePath, Map.class, requestMap);

                if (response.getStatusCode() == HttpStatus.OK) {
                    Map<String, String> data = response.getBody();
                    LOGGER.info("Reading database secret retrieval from : {} successful", databasePath);
                    return data;
                } else {
                    LOGGER.error("Vault database secret retrieval failed for path: {}", databasePath);
                    throw new Exception("Vault database secret retrieval failed");
                }
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    LOGGER.error("Invalid Vault token");
                    throw new Exception("Invalid Vault token");
                } else {
                    numRetries++;
                    LOGGER.warn("Retrying database secret retrieval after {} milliseconds", backoffInterval * numRetries);
                    waitUntilConditionMet(backoffInterval * numRetries, maxBackoffInterval);
                }
            } catch (Exception ex) {
                numRetries++;
                LOGGER.warn("Retrying database secret retrieval after {} milliseconds", backoffInterval * numRetries);
                waitUntilConditionMet(backoffInterval * numRetries, maxBackoffInterval);
            }
        }
        LOGGER.error("Max retries exceeded for Vault database secret retrieval for path: {}", databasePath);
        throw new Exception("Max retries exceeded for Vault database secret retrieval");
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
