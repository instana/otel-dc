package com.instana.vault.services.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.instana.vault.DbConfig;
import com.instana.vault.VaultConfigurable;
import com.instana.vault.VaultServiceConfig;

import java.util.Map;
import java.util.logging.Logger;

public class VaultConfigStep {

    private static final Logger logger = Logger.getLogger(VaultConfigStep.class.getName());

    private static Vault vaultClient;

    public static <T extends VaultConfigurable> T executeStep(T config) throws RuntimeException, VaultException {
        setVaultClient(config.getVaultServiceConfig());
        return updateVaultSecretConfig(config);
    }

    private static void setVaultClient(VaultServiceConfig config) {
        vaultClient = VaultClient.createVaultClient(config);
    }

    private static String fetchSecret(Map<String, Object> vault_secrets, VaultServiceConfig config) throws VaultException {
        String key = (String) vault_secrets.get("vault_secret.key");
        String path = (String) vault_secrets.get("vault_secret.path");
        vaultClient = VaultClient.createVaultClient(config);
        String secret = read(path, key);
        if (secret.isEmpty()) {
            throw new RuntimeException("Retrieved secret was empty.");
        }
        logger.info("Fetched secret from vault successfully path:" + path);
        return secret;
    }

    private static <T extends VaultConfigurable> T updateVaultSecretConfig(T config) throws RuntimeException, VaultException {
        for (Map<String, Object> instance : config.getInstances()) {
            for (Map.Entry<String, Object> prop : instance.entrySet()) {
                if (prop.getValue() instanceof Map) {
                    Map<String, Object> vault_secrets = (Map) prop.getValue();
                    for (Map.Entry<String, Object> secret : vault_secrets.entrySet()) {
                        if (secret.getKey().contains("vault_secret")) {
                            instance.put(prop.getKey(), fetchSecret(vault_secrets, config.getVaultServiceConfig()));
                            break;
                        }
                    }
                }
            }
        }

        return config;
    }

    public static String read(final String path, final String keyName) throws VaultException {
        try {
            return vaultClient.logical().read(path).getData().get(keyName);
        } catch (VaultException e) {
            logger.info("Error occurred during the Vault secret retrieval, path: " + path + ", key: " + keyName + ", message: " + e.getMessage());
            throw e;
        }
    }

}
