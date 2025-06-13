package com.instana.dc.genai.vectordb.impl;

import java.util.Map;
import java.util.logging.Logger;

import com.instana.dc.genai.DataCollector.CustomDcConfig;
import com.instana.dc.genai.base.AbstractGenAIDc;
import com.instana.dc.genai.vectordb.metrics.VectordbRawMetricRegistry;

import static com.instana.dc.DcUtil.*;
import static com.instana.dc.genai.util.GenAIDcUtil.OTEL_AGENTLESS_MODE;
import static com.instana.dc.genai.util.GenAIDcUtil.SERVICE_LISTEN_PORT;

public class VectordbDc extends AbstractGenAIDc {
    private static final Logger logger = Logger.getLogger(VectordbDc.class.getName());
    private static final int DEFAULT_PORT = 8000;
    private final VectordbMetricCollector metricCollector;

    public VectordbDc(Map<String, Object> properties, CustomDcConfig cdcConfig) {
        super(properties, cdcConfig);
        Boolean otelAgentlessMode = (Boolean) properties.getOrDefault(OTEL_AGENTLESS_MODE, Boolean.FALSE);
        Integer callbackInterval = (Integer) properties.getOrDefault(CALLBACK_INTERVAL, DEFAULT_CALLBACK_INTERVAL);
        Integer otelPollInterval = (Integer) properties.getOrDefault(POLLING_INTERVAL, callbackInterval);
        int listenPort = (int) properties.getOrDefault(SERVICE_LISTEN_PORT, DEFAULT_PORT);
        this.metricCollector = new VectordbMetricCollector(otelAgentlessMode, otelPollInterval, listenPort, VectordbRawMetricRegistry.getRawMetrics());
    }

    @Override
    protected String getPlatformName() {
        return "VectorDB";
    }

    @Override
    protected String getPluginName() {
        return "vectordb";
    }

    @Override
    public void collectData() {
        logger.info("Starting VectorDB metrics collection");
        metricCollector.start();
    }
} 