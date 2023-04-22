package com.sc.hcv.client.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class VaultAuthenticator {

    private String vaultUrl;
    private RestTemplate restTemplate;
    private int maxRetries;
    private long backoffInterval;

    public VaultAuthenticator(String vaultUrl, int maxRetries, long backoffInterval) {
        this.vaultUrl = vaultUrl;
        this.maxRetries = maxRetries;
        this.backoffInterval = backoffInterval;
        this.restTemplate = new RestTemplate();
    }

    public String authenticate(String roleId, String secretId) throws Exception {
        int numRetries = 0;
        while (numRetries < maxRetries) {
            try {
                Map<String, String> requestMap = new HashMap<>();
                requestMap.put("role_id", roleId);
                requestMap.put("secret_id", secretId);

                ResponseEntity<String> response = restTemplate.postForEntity(vaultUrl + "/v1/auth/approle/login", requestMap, String.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    return "Vault authentication successful";
                } else {
                    throw new Exception("Vault authentication failed");
                }
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    throw new Exception("Invalid Vault credentials");
                } else {
                    numRetries++;
                    Thread.sleep(backoffInterval * numRetries);
                }
            } catch (Exception ex) {
                numRetries++;
                Thread.sleep(backoffInterval * numRetries);
            }
        }
        throw new Exception("Max retries exceeded for Vault authentication");
    }
}
