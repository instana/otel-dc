package com.instana.vault.services.vault;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.instana.vault.VaultServiceConfig;
import com.instana.vault.services.vault.auth.AuthFactory;
import com.instana.vault.services.vault.auth.VaultAuthenticationStrategy;

import java.io.File;
import java.util.logging.Logger;

public class VaultClient {

    private static final Logger logger = Logger.getLogger(VaultClient.class.getName());

    public static Vault createVaultClient(VaultServiceConfig vaultServiceConfig) {
        try {
            final VaultConfig vaultConfig = new VaultConfig()
                    .address(vaultServiceConfig.getConnectionURL());
            if (vaultServiceConfig.getConnectionURL().startsWith("https")) {
                SslConfig sslConfig = new SslConfig().pemFile(new File(vaultServiceConfig.getPathToPEMFile())).build(); // must be build to verify
                vaultConfig.sslConfig(sslConfig);
                logger.info("set ssl for Vault");
            }

            VaultAuthenticationStrategy vaultAuthenticationStrategy = AuthFactory.getVaultAuthStrategyFromConfig(vaultServiceConfig);
            if (!vaultAuthenticationStrategy.token().get().isEmpty()) {
                vaultConfig.token(vaultAuthenticationStrategy.token().get());
                return new Vault(vaultConfig.build(), vaultServiceConfig.getKvVersion());
            } else if (!vaultAuthenticationStrategy.github().get().isEmpty()) {
                Vault vault = new Vault(vaultConfig);
                String authToken = vault.auth().loginByGithub(vaultAuthenticationStrategy.github().get()).getAuthClientToken();
                vaultConfig.token(authToken);
                return new Vault(vaultConfig.build(), vaultServiceConfig.getKvVersion());
            } else if (!(vaultAuthenticationStrategy.roleID().get().isEmpty() && vaultAuthenticationStrategy.secretID().get().isEmpty())) {
                Vault vault = new Vault(vaultConfig);
                String authToken = vault.auth().loginByAppRole(vaultAuthenticationStrategy.roleID().get(), vaultAuthenticationStrategy.secretID().get()).getAuthClientToken();
                vaultConfig.token(authToken);
                return new Vault(vaultConfig.build(), vaultServiceConfig.getKvVersion());
            }
        } catch (VaultException e) {
            logger.info("An exception occurred" + e.getMessage());
        }
        return null;
    }


}