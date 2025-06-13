package com.instana.dc.genai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.instana.dc.IDc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataCollector {
    private static final String CONFIG_ENV = "DC_CONFIG";
    private static final String CONFIG_YAML = "config/config.yaml";
    private static final String LOGGING_PROP = "config/logging.properties";
    private static final Logger logger = Logger.getLogger(DataCollector.class.getName());

    static {
        System.setProperty("java.util.logging.config.file", LOGGING_PROP);
    }

    private final CustomDcConfig cdcConfig;
    private final List<IDc> dcs;

    private DataCollector() throws IOException {
        this.cdcConfig = loadConfig();
        this.dcs = initializeCollectors();
        initializeFirstCollector();
    }

    private CustomDcConfig loadConfig() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        String configFile = System.getenv(CONFIG_ENV);
        if (configFile == null) {
            configFile = CONFIG_YAML;
        }
        return objectMapper.readValue(new File(configFile), CustomDcConfig.class);
    }

    private List<IDc> initializeCollectors() {
        List<IDc> collectors = new ArrayList<>();
        for (Map<String, Object> props : cdcConfig.getInstances()) {
            try {
                collectors.add(createLLMCollector(props));
                collectors.add(createVectorDBCollector(props));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to initialize collector: {0}", e.getMessage());
            }
        }
        return collectors;
    }

    private void initializeFirstCollector() {
        if (!dcs.isEmpty()) {
            try {
                dcs.get(0).initOnce();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to initialize first collector: {0}", e.getMessage());
            }
        }
    }

    private IDc createLLMCollector(Map<String, Object> props) throws Exception {
        return new GenAIDcRegistry()
                .findGenAIDc("LLM")
                .getConstructor(Map.class, CustomDcConfig.class)
                .newInstance(props, cdcConfig);
    }

    private IDc createVectorDBCollector(Map<String, Object> props) throws Exception {
        return new GenAIDcRegistry()
                .findGenAIDc("VECTORDB")
                .getConstructor(Map.class, CustomDcConfig.class)
                .newInstance(props, cdcConfig);
    }

    public static void main(String[] args) {
        try {
            DataCollector collector = new DataCollector();
            collector.initAndStartCollectors();
            logger.log(Level.INFO, "GenAI Data Collector initialized with {0} collectors", collector.dcs.size());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start data collector: {0}", e.getMessage());
        }
    }

    public void initAndStartCollectors() throws Exception {
        for (IDc dc : dcs) {
            dc.initDC();
            dc.start();
        }
    }

    public static class CustomDcConfig {
        @JsonProperty("genai.application")
        private String application;
        private final List<Map<String, Object>> instances = new ArrayList<>();

        public List<Map<String, Object>> getInstances() {
            return instances;
        }
    }
} 