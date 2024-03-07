package com.instana.dc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.instana.dc.config.services.vault.VaultConfigStep;
import com.instana.dc.rdb.DataCollector;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.CONFIG_ENV;
import static com.instana.dc.DcUtil.CONFIG_YAML;

public class ConfigPipeline {
    private static final Logger logger = Logger.getLogger(DataCollector.class.getName());

    public static Config getConfig() throws IOException {
       ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
       objectMapper.findAndRegisterModules();
       String configFile = System.getenv(CONFIG_ENV);
       if (configFile == null) {
           configFile = CONFIG_YAML;
       }
       Config dcConfig = objectMapper.readValue(new File(configFile), Config.class);
       if(dcConfig.isVaultServiceConfigPresent()){
           logger.info("vault config exists");
           dcConfig = VaultConfigStep.executeStep(dcConfig);
       }
       else{
           logger.info("vault config didn't exists");
       }
        return dcConfig;

    }
}
