package com.instana.dc.config.services.vault;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.instana.dc.config.VaultServiceConfig;
import com.instana.dc.config.services.vault.auth.AuthFactory;
import com.instana.dc.config.services.vault.auth.VaultAuthenticationStrategy;

import java.io.File;
import java.util.logging.Logger;

public class VaultClient{

    private static final Logger logger = Logger.getLogger(VaultClient.class.getName());
    public static Vault createVaultClient(VaultServiceConfig vaultServiceConfig){
        try{
            final VaultConfig vaultConfig = new VaultConfig()
                    .address(vaultServiceConfig.getConnectionURL());
            if(vaultServiceConfig.getConnectionURL().startsWith("https")){
                SslConfig sslConfig = new SslConfig().pemFile(new File(vaultServiceConfig.getPathToPEMFile())).build(); // must be build to verify
                vaultConfig.sslConfig(sslConfig);
                logger.info("set ssl for Vault");
            }

            VaultAuthenticationStrategy vaultAuthenticationStrategy = AuthFactory.getVaultAuthStrategyFromConfig(vaultServiceConfig);
            vaultConfig.token(vaultAuthenticationStrategy.token().get());
            return new Vault(vaultConfig.build(),vaultServiceConfig.getKvVersion());
        }catch (VaultException e){
            logger.info("An exception occured"+e.getMessage());
        }
        return null;
    }




}