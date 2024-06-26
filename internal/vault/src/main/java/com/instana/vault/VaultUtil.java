/*
 * IBM Confidential
 * Copyright IBM Corp. 2024
 */

package com.instana.vault;

import com.bettercloud.vault.VaultException;
import com.instana.vault.services.vault.VaultConfig;

import java.util.logging.Logger;

public class VaultUtil {

    private VaultUtil(){
        //private constructor
    }

    private static final Logger logger = Logger.getLogger(VaultUtil.class.getName());

    public static <T extends VaultService> T isVaultConfigured(T config) throws VaultException {
        if (config.isVaultServiceConfigPresent()) {
            logger.info("vault config exists");
            config = VaultConfig.executeStep(config);
        } else {
            logger.info("vault config didn't exists");
        }
        return config;
    }
}
