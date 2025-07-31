package com.instana.dc.genai.vectordb.milvus.impl;

import java.util.Map;
import java.util.logging.Logger;

import com.instana.dc.genai.DataCollector.CustomDcConfig;
import com.instana.dc.genai.base.AbstractGenAIDc;
import com.instana.dc.genai.vectordb.milvus.metrics.MilvusRawMetricRegistry;

import static com.instana.dc.DcUtil.*;
import static com.instana.dc.genai.util.GenAIDcUtil.OTEL_AGENTLESS_MODE;
import static com.instana.dc.genai.util.GenAIDcUtil.SERVICE_LISTEN_PORT;

public class MilvusDc extends AbstractGenAIDc {
    private static final Logger logger = Logger.getLogger(MilvusDc.class.getName());
    private static final int DEFAULT_PORT = 8000;
    private final MilvusMetricCollector metricCollector;

    public MilvusDc(Map<String, Object> properties, CustomDcConfig cdcConfig) {
        super(properties, cdcConfig, MilvusRawMetricRegistry.getRawMetrics());
        Boolean otelAgentlessMode = (Boolean) properties.getOrDefault(OTEL_AGENTLESS_MODE, Boolean.FALSE);
        Integer callbackInterval = (Integer) properties.getOrDefault(CALLBACK_INTERVAL, DEFAULT_CALLBACK_INTERVAL);
        Integer otelPollInterval = (Integer) properties.getOrDefault(POLLING_INTERVAL, callbackInterval);
        int listenPort = (int) properties.getOrDefault(SERVICE_LISTEN_PORT, DEFAULT_PORT);
        this.metricCollector = new MilvusMetricCollector(otelAgentlessMode, otelPollInterval, listenPort, MilvusRawMetricRegistry.getRawMetrics());
    }

    @Override
    protected String getPlatformName() {
        return "Milvus";
    }

    @Override
    protected String getPluginName() {
        return "milvus";
    }

    @Override
    protected String getServiceName(Map<String, Object> properties) {
        return getPlatformName() + ":" + properties.get(OTEL_SERVICE_NAME);
    }

    @Override
    public void collectData() {
        logger.info("Starting Milvus metrics collection");
        metricCollector.start();
    }
} 