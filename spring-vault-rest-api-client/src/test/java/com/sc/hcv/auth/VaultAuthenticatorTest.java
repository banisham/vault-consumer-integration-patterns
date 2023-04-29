package com.sc.hcv.auth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VaultAuthenticatorTest {
    private VaultAuthenticator vaultAuthenticator;
    @Mock
    private RestTemplate restTemplate;

    private final String roleId = "testRoleId";
    private final String secretId = "testSecretId";
    private final String authPath = "/testAuthPath";
    private final String vaultUrl = "http://testvaulturl.com";
    private final int maxRetries = 3;
    private final long backoffInterval = 100L;
    private final long maxBackoffInterval = 1000L;

    @Before
    public void setUp() {
        vaultAuthenticator = new VaultAuthenticator(vaultUrl, maxRetries, backoffInterval, maxBackoffInterval);
    }

    @Test
    public void testAuthenticateAppRoleSuccess() throws Exception {
        String token = "testToken";
        String responseJson = "{\"auth\": {\"client_token\": \"" + token + "\"}}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(responseJson, HttpStatus.OK);
        when(restTemplate.postForEntity(eq(vaultUrl + authPath), any(), eq(String.class))).thenReturn(responseEntity);

        String result = vaultAuthenticator.authenticateAppRole(roleId, secretId, authPath);
        assertEquals(token, result);
    }

    @Test(expected = Exception.class)
    public void testAuthenticateAppRoleFailure() throws Exception {
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"code\": \"testCode\", \"message\": \"testMessage\"}", HttpStatus.BAD_REQUEST);
        when(restTemplate.postForEntity(eq(vaultUrl + authPath), any(), eq(String.class))).thenReturn(responseEntity);

        vaultAuthenticator.authenticateAppRole(roleId, secretId, authPath);
    }

    @Test(expected = Exception.class)
    public void testAuthenticateAppRoleMaxRetries() throws Exception {
        when(restTemplate.postForEntity(eq(vaultUrl + authPath), any(), eq(String.class))).thenThrow(HttpClientErrorException.class);

        vaultAuthenticator.authenticateAppRole(roleId, secretId, authPath);
    }
}

