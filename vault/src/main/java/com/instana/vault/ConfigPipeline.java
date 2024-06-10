package com.instana.vault;


import com.bettercloud.vault.VaultException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.instana.vault.services.vault.VaultConfigStep;


import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.CONFIG_ENV;
import static com.instana.dc.DcUtil.CONFIG_YAML;

public class ConfigPipeline {
    private static final Logger logger = Logger.getLogger(ConfigPipeline.class.getName());

    public static <T extends VaultConfigurable> T getConfig(Class<T> configClass) throws IOException, RuntimeException, VaultException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.findAndRegisterModules();
        String configFile = System.getenv(CONFIG_ENV);
        if (configFile == null) {
            configFile = CONFIG_YAML;
        }
        T config = objectMapper.readValue(new File(configFile), configClass);
        if (config.isVaultServiceConfigPresent()) {
            logger.info("vault config exists");
            config = VaultConfigStep.executeStep(config);
        } else {
            logger.info("vault config didn't exists");
        }
        return config;

    }
}
