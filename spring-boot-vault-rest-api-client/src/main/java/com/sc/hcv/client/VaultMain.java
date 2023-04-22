package com.sc.hcv.client;

import com.sc.hcv.client.auth.VaultAuthenticator;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class VaultMain {

    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("application.properties")) {
            properties.load(fis);
        } catch (IOException ex) {
            System.out.println("Failed to load properties file: " + ex.getMessage());
            return;
        }

        String vaultUrl = properties.getProperty("vaultUrl");
        String authMethod = properties.getProperty("authMethod");
        String roleId = properties.getProperty("roleId");
        String secretId = properties.getProperty("secretId");
        int maxRetries = Integer.parseInt(properties.getProperty("maxRetries"));
        long backoffInterval = Long.parseLong(properties.getProperty("backoffInterval"));

        VaultAuthenticator authenticator;
        if (authMethod.equals("approle")) {
            authenticator = new VaultAuthenticator(vaultUrl, maxRetries, backoffInterval);
        } else {
            System.out.println("Invalid authentication method specified in properties file");
            return;
        }

        String result = authenticator.authenticate(roleId, secretId);
        System.out.println(result);
    }
}
