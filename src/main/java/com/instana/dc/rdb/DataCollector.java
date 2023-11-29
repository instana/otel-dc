/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.instana.dc.rdb.impl.DbDcRegistry;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.CONFIG_YAML;
import static com.instana.dc.DcUtil.LOGGING_PROP;

public class DataCollector {
    static {
        System.setProperty("java.util.logging.config.file", LOGGING_PROP);
    }

    private static final Logger logger = Logger.getLogger(DataCollector.class.getName());

    private final DcConfig dcConfig;

    private final List<IDbDc> dcs;

    private DataCollector() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        dcConfig = objectMapper.readValue(new File(CONFIG_YAML), DcConfig.class);
        int n = dcConfig.getInstances().size();
        dcs = new ArrayList<>(n);
        for (Map<String, String> props : dcConfig.getInstances()) {
            dcs.add(newDc(props));
        }
        if (!dcs.isEmpty()) {
            dcs.get(0).initOnce();
        }
        //Informix
        Map<String, Object> informixConfig = dcConfig.getInformix();
        Map<String, Object> systemProps = (Map<String, Object>) informixConfig.get("system.info");
        List<Map<String, String>> instances = (List<Map<String, String>>) informixConfig.get("instances");
        for (Map<String, String> instanceProps : instances) {
            dcs.add(newDc(systemProps, instanceProps));
        }
    }

    private IDbDc newDc(Map<String, String> props) throws Exception {
        return new DbDcRegistry().findDatabaseDc(dcConfig.getDbSystem()).getConstructor(Map.class, String.class, String.class).newInstance(props, dcConfig.getDbSystem(), dcConfig.getDbDriver());
    }

    private IDbDc newDc(Map<String, Object> systemProps, Map<String, String> instanceProps) throws Exception {
        return new DbDcRegistry().findDatabaseDc((String) systemProps.get("db.system")).getConstructor(Map.class, Map.class).newInstance(systemProps, instanceProps);
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
        for (IDbDc dc : dcs) {
            dc.initDC();
            logger.info("DC No." + ++i + " is initialized");
        }
    }

    private void startCollect() {
        int i = 1;
        for (IDbDc dc : dcs) {
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
        private final List<Map<String, String>> instances = new ArrayList<>();

        private final Map<String, Object> informix = new HashMap<>();

        public String getDbSystem() {
            return dbSystem;
        }

        public String getDbDriver() {
            return dbDriver;
        }

        public List<Map<String, String>> getInstances() {
            return instances;
        }

        public Map<String, Object> getInformix() {
            return informix;
        }

        public void setDbSystem(String dbSystem) {
            this.dbSystem = dbSystem;
        }

        public void setDbDriver(String dbDriver) {
            this.dbDriver = dbDriver;
        }
    }

}
