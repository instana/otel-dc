/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb;

import com.instana.dc.IDc;
import com.instana.dc.config.Config;
import com.instana.dc.config.ConfigPipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.LOGGING_PROP;

public class DataCollector {
    static {
        System.setProperty("java.util.logging.config.file", LOGGING_PROP);
    }

    private static final Logger logger = Logger.getLogger(DataCollector.class.getName());

    private final Config dcConfig;

    private final List<IDc> dcs;

    private DataCollector() throws Exception {
        dcConfig = ConfigPipeline.getConfig();
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

}
