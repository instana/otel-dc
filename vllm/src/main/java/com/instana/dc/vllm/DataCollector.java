/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.vllm;

import static com.instana.dc.DcUtil.CONFIG_ENV;
import static com.instana.dc.DcUtil.CONFIG_YAML;
import static com.instana.dc.DcUtil.LOGGING_PROP;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.instana.dc.IDc;

public class DataCollector {
    static {
        System.setProperty("java.util.logging.config.file", LOGGING_PROP);
    }

    private static final Logger logger = Logger.getLogger(DataCollector.class.getName());

    private final List<IDc> dcs;

    private DataCollector() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        String configFile = Optional.ofNullable(System.getenv(CONFIG_ENV)).orElse(CONFIG_YAML);
        CustomDcConfig cdcConfig = objectMapper.readValue(new File(configFile), CustomDcConfig.class);
        dcs = new ArrayList<>(cdcConfig.getInstances().size());
        for (Map<String, Object> props : cdcConfig.getInstances()) {
            dcs.add(newDc(props));
        }
    }

    private IDc newDc(Map<String, Object> props) throws Exception {
        return new VLLMDcRegistry().findVLLMDc("vLLM").getConstructor(Map.class)
                .newInstance(props);
    }

    public static void main(String[] args) {
        try {
            DataCollector dataCollector = new DataCollector();
            dataCollector.initDcs();
            dataCollector.startCollect();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void initDcs() throws Exception {
        int i = 0;
        for (IDc dc : dcs) {
            dc.initDC();
            logger.info("DC No." + ++i + " is initialized");
        }
    }

    private void startCollect() {
        int i = 1;
        for (IDc dc : dcs) {
            logger.info("DC No." + i + " is collecting data...");
            dc.start();
            i++;
        }
    }

    static public class CustomDcConfig {
        @JsonProperty("llm.application")
        private String application;

        private final List<Map<String, Object>> instances = new ArrayList<>();

        public void setApplication(String application) {
            this.application = application;
        }

        public String getApplication() {
            return application;
        }

        public List<Map<String, Object>> getInstances() {
            return instances;
        }

    }
}
