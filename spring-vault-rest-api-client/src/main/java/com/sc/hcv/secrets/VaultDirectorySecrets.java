package com.sc.hcv.secrets;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class VaultDirectorySecrets {

    private static final Logger LOGGER = LoggerFactory.getLogger(com.sc.hcv.secrets.VaultDirectorySecrets.class);

    private final String vaultUrl;
    private  RestTemplate restTemplate;
    private final int maxRetries;
    private final long backoffInterval;
    private final long maxBackoffInterval;


    public VaultDirectorySecrets(String vaultUrl, int maxRetries, long backoffInterval, long maxBackOffInterval) {
        this.vaultUrl = vaultUrl;
        this.maxRetries = maxRetries;
        this.backoffInterval = backoffInterval;
        this.maxBackoffInterval = maxBackOffInterval;
        this.restTemplate = new RestTemplate();
    }


    /**
     * Retrieves a secret from the Active Directory Secret Engine in HashiCorp Vault using the provided Vault token and secret path.
     *
     * @param vaultToken   The Vault token used to authenticate with the Vault server.
     * @param adPath   The path to the secret in the Active Directory Secret Engine.
     * @param adKey    The key of the secret to retrieve.
     * @return             The value of the specified secret.
     * @throws Exception   If an error occurs while retrieving the secret, such as an invalid token or a non-existent secret.
     */
    public Map<String, String> readActiveDirectorySecret(String vaultToken, String adPath, String adKey) throws Exception {
        String secretPath = adPath + "/" + adKey;
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("token", vaultToken);

        ResponseEntity<Map> response = null;
        int numRetries = 0;
        while (numRetries < maxRetries) {
            try {
                response = restTemplate.getForEntity(vaultUrl + "/v1/" + secretPath, Map.class, requestMap);

                if (response.getStatusCode() == HttpStatus.OK) {
                    JSONObject data = new JSONObject(response.getBody());
                    if (data.has("data") && !data.isNull("data")) {
                        JSONObject secretData = data.getJSONObject("data");
                        Map<String, String> secretMap = new HashMap<>();
                        Iterator<String> keys = secretData.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            String value = secretData.getString(key);
                            secretMap.put(key, value);
                        }
                        LOGGER.info("Vault AD secret retrieval from : {} successful", secretPath);
                        return secretMap;
                    } else {
                        LOGGER.error("Vault AD secret not found for path: {}", secretPath);
                        throw new Exception("Secret data not found");
                    }
                } else {
                    LOGGER.error("Vault AD secret retrieval failed for path: {}", secretPath);
                    throw new Exception("Secret retrieval failed");

                }
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    LOGGER.error("Invalid Vault token");
                    throw new Exception("Invalid Vault token");
                } else if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                    LOGGER.error("Vault AD secret not found for path: {}", secretPath);
                    throw new Exception("Secret not found");
                } else {
                    numRetries++;
                    LOGGER.warn("Retrying AD secret retrieval after {} milliseconds", backoffInterval * numRetries);
                    waitUntilConditionMet(backoffInterval * numRetries, maxBackoffInterval);
                }
            } catch (Exception ex) {
                numRetries++;
                LOGGER.warn("Retrying AD secret retrieval after {} milliseconds", backoffInterval * numRetries);
                waitUntilConditionMet(backoffInterval * numRetries, maxBackoffInterval);
            }
        }
        LOGGER.error("Max retries exceeded for Vault AD secret retrieval for path: {}", secretPath);
        throw new Exception("Max retries exceeded for secret retrieval");
    }

    /**
     * Retrieves a secret from the LDAP Secret Engine in HashiCorp Vault using the provided Vault token and secret path.
     *
     * @param vaultToken   The Vault token used to authenticate with the Vault server.
     * @param ldapPath     The path to the secret in the LDAP  Secret Engine.
     * @return             The value of the specified secret.
     * @throws Exception   If an error occurs while retrieving the secret, such as an invalid token or a non-existent secret.
     */

    public Map<String, String> readLdapSecret(String vaultToken, String ldapPath) throws Exception {
        int numRetries = 0;
        while (numRetries < maxRetries) {
            try {
                Map<String, String> requestMap = new HashMap<>();
                requestMap.put("token", vaultToken);

                ResponseEntity<Map> response = restTemplate.getForEntity(vaultUrl + "/v1/" + ldapPath, Map.class, requestMap);

                if (response.getStatusCode() == HttpStatus.OK) {
                    Map<String, String> data = response.getBody();
                    LOGGER.info("Vault LDAP secret retrieval from : {} successful", ldapPath);

                    return data;
                } else {
                    LOGGER.error("Vault LDAP secret retrieval failed for path: {}", ldapPath);
                    throw new Exception("Vault LDAP secret retrieval failed");
                }
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    LOGGER.error("Invalid Vault token");
                    throw new Exception("Invalid Vault token");
                } else if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                    LOGGER.error("Vault LDAP secret not found for path: {}", ldapPath);
                    throw new Exception("Secret not found");
                } else {
                    numRetries++;
                    LOGGER.warn("Retrying LDAP secret retrieval after {} milliseconds", backoffInterval * numRetries);
                    waitUntilConditionMet(backoffInterval * numRetries, maxBackoffInterval);
                }
            } catch (Exception ex) {
                numRetries++;
                LOGGER.warn("Retrying LDAP secret retrieval after {} milliseconds", backoffInterval * numRetries);
                waitUntilConditionMet(backoffInterval * numRetries, maxBackoffInterval);
            }
        }
        throw new Exception("Max retries exceeded for Vault LDAP secret retrieval");
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

