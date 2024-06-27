/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.instana.dc.IDc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.*;

public class DataCollector {
    static {
        System.setProperty("java.util.logging.config.file", LOGGING_PROP);
    }

    private static final Logger logger = Logger.getLogger(DataCollector.class.getName());

    private final CustomDcConfig dcConfig;

    private final List<IDc> dcs;

    private DataCollector() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        String configFile = System.getenv(CONFIG_ENV);
        if (configFile == null) {
            configFile = "config/config-informix.yaml";
        }
        dcConfig = objectMapper.readValue(new File(configFile), CustomDcConfig.class);
        int n = dcConfig.getInstances().size();
        dcs = new ArrayList<>(n);
        for (Map<String, Object> props : dcConfig.getInstances()) {
            dcs.add(newDc(props));
        }
        if (!dcs.isEmpty()) {
            dcs.get(0).initOnce();
        }
    }

    private IDc newDc(Map<String, Object> props) throws Exception {
        return new DbDcRegistry().findDatabaseDc(dcConfig.getDbSystem()).getConstructor(Map.class, String.class, String.class)
                .newInstance(props, dcConfig.getDbSystem(), dcConfig.getDbDriver());
    }

    public static void main(String[] args) {
        try {
            DataCollector dcol = new DataCollector();
            dcol.initDcs();
            dcol.startCollect();
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

    static class DcConfig {
        @JsonProperty("db.system")
        private String dbSystem;
        @JsonProperty("db.driver")
        private String dbDriver;
        private final List<Map<String, Object>> instances = new ArrayList<>();

        public String getDbSystem() {
            return dbSystem;
        }

        public String getDbDriver() {
            return dbDriver;
        }

        public List<Map<String, Object>> getInstances() {
            return instances;
        }

        public void setDbSystem(String dbSystem) {
            this.dbSystem = dbSystem;
        }

        public void setDbDriver(String dbDriver) {
            this.dbDriver = dbDriver;
        }
    }

    static class CustomDcConfig {
        @JsonProperty("db.system")
        private String dbSystem;
        @JsonProperty("db.driver")
        private String dbDriver;
        private final List<Map<String, Object>> instances = new ArrayList<>();

        public String getDbSystem() {
            return dbSystem;
        }

        public String getDbDriver() {
            return dbDriver;
        }

        public List<Map<String, Object>> getInstances() {
            return instances;
        }

        public void setDbSystem(String dbSystem) {
            this.dbSystem = dbSystem;
        }

        public void setDbDriver(String dbDriver) {
            this.dbDriver = dbDriver;
        }
    }

}
