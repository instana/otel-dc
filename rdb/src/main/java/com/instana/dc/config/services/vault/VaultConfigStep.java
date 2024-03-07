package com.instana.dc.config.services.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.instana.dc.config.Config;
import com.instana.dc.config.VaultServiceConfig;

import java.util.Map;
import java.util.logging.Logger;

public class VaultConfigStep {

    private static final Logger logger = Logger.getLogger(VaultConfigStep.class.getName());

    private static Vault vaultClient;
    public static Config executeStep(Config config){
        setVaultClient(config.getVaultServiceConfig());
        return updateVaultSecretConfig(config);
    }
    private static void setVaultClient(VaultServiceConfig config){
        vaultClient = VaultClient.createVaultClient(config);
    }

    private static String fetchSecret(Map<String,Object> vault_secrets, Config config){
        String key = (String) vault_secrets.get("vault_secret.key");
        String path = (String) vault_secrets.get("vault_secret.path");
        vaultClient = VaultClient.createVaultClient(config.getVaultServiceConfig());
        String secret = read(path,key);
        logger.info("Fetched secret from vault successfully path:"+path);
        return secret;
    }
    private static Config updateVaultSecretConfig(Config config){
        for(Map<String,Object> instance: config.getInstances()){
            for(Map.Entry<String,Object> prop: instance.entrySet()){
                if(prop.getValue() instanceof Map) {
                    Map<String, Object> vault_secrets = (Map) prop.getValue();
                    for (Map.Entry<String, Object> secret : vault_secrets.entrySet()) {
                        if (secret.getKey().contains("vault_secret")) {
                            instance.put(prop.getKey(),fetchSecret(vault_secrets,config));
                            break;
                        }
                    }
                }
            }
        }

        return config;
    }
    public static String read(final String path, final String keyName) {
        try {
            return vaultClient.logical().read(path).getData().get(keyName);
        } catch (VaultException e) {
            logger.info("Error occurred during the Vault secret retrieval, path: "+path+", key: "+keyName+", message: "+e.getMessage());
        }
        return "";
    }

}
