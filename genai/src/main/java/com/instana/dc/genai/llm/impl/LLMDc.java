package com.instana.dc.genai.llm.impl;

import com.instana.dc.genai.DataCollector.CustomDcConfig;
import com.instana.dc.genai.base.AbstractGenAIDc;
import com.instana.dc.genai.llm.metrics.LLMRawMetricRegistry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;

import java.util.Map;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.*;
import static com.instana.dc.genai.llm.utils.LLMDcUtil.*;
import static com.instana.dc.genai.util.GenAIDcUtil.OTEL_AGENTLESS_MODE;
import static com.instana.dc.genai.util.GenAIDcUtil.SERVICE_LISTEN_PORT;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

public class LLMDc extends AbstractGenAIDc {
    private static final Logger logger = Logger.getLogger(LLMDc.class.getName());
    private static final int DEFAULT_PORT = 8000;
    private final LLMMetricCollector metricCollector;
    private final String currencyCode;

    public LLMDc(Map<String, Object> properties, CustomDcConfig cdcConfig) {
        super(properties, cdcConfig, LLMRawMetricRegistry.getRawMetrics());
        Boolean otelAgentlessMode = (Boolean) properties.getOrDefault(OTEL_AGENTLESS_MODE, Boolean.FALSE);
        Integer callbackInterval = (Integer) properties.getOrDefault(CALLBACK_INTERVAL, DEFAULT_CALLBACK_INTERVAL);
        Integer otelPollInterval = (Integer) properties.getOrDefault(POLLING_INTERVAL, callbackInterval);
        int listenPort = (int) properties.getOrDefault(SERVICE_LISTEN_PORT, DEFAULT_PORT);
        this.currencyCode = (String) properties.getOrDefault(CURRENCY, USD);
        this.metricCollector = new LLMMetricCollector(otelAgentlessMode, otelPollInterval, listenPort, LLMRawMetricRegistry.getRawMetrics());
    }

    @Override
    public Resource getResourceAttributes() {
        Resource resource = super.getResourceAttributes();
        return resource.merge(Resource.create(Attributes.of(
                stringKey("llm.platform"), getPlatformName()
        ))).merge(Resource.create(Attributes.of(
                stringKey(CURRENCY), currencySymbolOf(currencyCode)
        )));
    }

    @Override
    protected String getPlatformName() {
        return "LLM";
    }

    @Override
    protected String getPluginName() {
        return "llmonitor";
    }

    @Override
    public void collectData() {
        logger.info("Starting LLM metrics collection");
        metricCollector.start();
    }
}
