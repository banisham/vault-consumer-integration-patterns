package com.sc.hcv;

import com.sc.hcv.auth.VaultAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VaultMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultMain.class);

    public static void main(String[] args) throws Exception {

        VaultMain vaultMain = new VaultMain();
        Properties properties = vaultMain.readVaultAttributes();


        String vaultUrl = properties.getProperty("vault.url");
        String authMethod = properties.getProperty("vault.auth.method");
        String authPath = properties.getProperty("vault.auth.path");
        String roleId = properties.getProperty("vault.auth.roleId");
        String secretId = properties.getProperty("vault.auth.secretId");
        int maxRetries = Integer.parseInt(properties.getProperty("vault.max-retries"));
        int backoffInterval = Integer.parseInt(properties.getProperty("vault.backoff-interval"));
        int maxBackoffInterval = Integer.parseInt(properties.getProperty("vault.max-backoff-interval"));
        String adPath = properties.getProperty("vault.ad.path");


        VaultAuthenticator authenticator;
        if (authMethod.equals("approle")) {
            authenticator = new VaultAuthenticator(vaultUrl, maxRetries, backoffInterval, maxBackoffInterval);
        } else {
            System.out.println("Invalid authentication method specified in properties file");
            return;
        }

        String result = authenticator.authenticateAppRole(roleId, secretId, authPath);

        System.out.println(result);

  }

    public Properties readVaultAttributes(){
        Properties props = new Properties();
        try{
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("vault.properties");
            if (inputStream == null) {
                throw new FileNotFoundException("vault.properties file not found in the classpath");
            }
            props.load(inputStream);


        } catch (IOException ex) {
            LOGGER.error("Failed to load properties file: " + ex.getMessage());
        }
        return props;
    }

}
