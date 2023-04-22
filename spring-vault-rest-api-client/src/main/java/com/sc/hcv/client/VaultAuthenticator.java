package com.sc.hcv.client;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class VaultAuthenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultAuthenticator.class);

    private final String vaultUrl;
    private final String authPath;
    private  RestTemplate restTemplate;
    private final int maxRetries;
    private final long backoffInterval;
    private final long maxBackoffInterval;


    public VaultAuthenticator(String vaultUrl, String authPath, int maxRetries, long backoffInterval, long maxBackOffInterval) {
        this.vaultUrl = vaultUrl;
        this.authPath = authPath;
        this.maxRetries = maxRetries;
        this.backoffInterval = backoffInterval;
        this.maxBackoffInterval = maxBackOffInterval;
        this.restTemplate = new RestTemplate();
    }

   /**
    * Authenticates to Vault using the AppRole authentication method.
    * @param roleId the Role ID for the AppRole
    * @param secretId the Secret ID for the AppRole
    * @return a string message indicating successful authentication
    * @throws Exception if the authentication fails after maxRetries attempts
    */

    public String authenticate(String roleId, String secretId) throws Exception {
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



    /**
     * Checks if the given Vault token is valid by performing a token lookup on the Vault server.
     * If the token is valid, returns true. If the token is invalid or lookup fails, returns false.
     * @param vaultToken the Vault token to check for validity
     * @return true if the token is valid, false otherwise
     */

    private boolean isValidToken(String vaultToken) throws Exception {
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

}
