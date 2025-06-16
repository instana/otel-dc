package com.instana.dc.genai.llm.metrics;

import static com.instana.dc.InstrumentType.GAUGE;
import static com.instana.dc.InstrumentType.UPDOWN_COUNTER;
import static com.instana.dc.genai.llm.utils.LLMDcUtil.*;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.instana.dc.RawMetric;

public final class LLMRawMetricRegistry {
    private static final Map<String, RawMetric> RAW_METRICS;
    
    static {
        Map<String, RawMetric> metrics = new ConcurrentHashMap<>();
        // Add LLM metrics using constants
        metrics.put(LLM_STATUS_NAME, new RawMetric(GAUGE, LLM_STATUS_NAME, LLM_STATUS_DESC, LLM_STATUS_UNIT, true, null));
        metrics.put(LLM_DURATION_NAME, new RawMetric(GAUGE, LLM_DURATION_NAME, LLM_DURATION_DESC, LLM_DURATION_UNIT, true, "model_id"));
        metrics.put(LLM_DURATION_MAX_NAME, new RawMetric(GAUGE, LLM_DURATION_MAX_NAME, LLM_DURATION_MAX_DESC, LLM_DURATION_MAX_UNIT, true, "model_id"));
        metrics.put(LLM_COST_NAME, new RawMetric(GAUGE, LLM_COST_NAME, LLM_COST_DESC, LLM_COST_UNIT, false, "model_id"));
        metrics.put(LLM_INPUT_COST_NAME, new RawMetric(GAUGE, LLM_INPUT_COST_NAME, LLM_INPUT_COST_DESC, LLM_INPUT_COST_UNIT, false, "model_id"));
        metrics.put(LLM_OUTPUT_COST_NAME, new RawMetric(GAUGE, LLM_OUTPUT_COST_NAME, LLM_OUTPUT_COST_DESC, LLM_OUTPUT_COST_UNIT, false, "model_id"));
        metrics.put(LLM_TOKEN_NAME, new RawMetric(GAUGE, LLM_TOKEN_NAME, LLM_TOKEN_DESC, LLM_TOKEN_UNIT, false, "model_id"));
        metrics.put(LLM_INPUT_TOKEN_NAME, new RawMetric(GAUGE, LLM_INPUT_TOKEN_NAME, LLM_INPUT_TOKEN_DESC, LLM_INPUT_TOKEN_UNIT, false, "model_id"));
        metrics.put(LLM_OUTPUT_TOKEN_NAME, new RawMetric(GAUGE, LLM_OUTPUT_TOKEN_NAME, LLM_OUTPUT_TOKEN_DESC, LLM_OUTPUT_TOKEN_UNIT, false, "model_id"));
        metrics.put(LLM_REQ_COUNT_NAME, new RawMetric(UPDOWN_COUNTER, LLM_REQ_COUNT_NAME, LLM_REQ_COUNT_DESC, LLM_REQ_COUNT_UNIT, false, "model_id"));

        metrics.put(LLM_SERVICE_COST_NAME, new RawMetric(GAUGE, LLM_SERVICE_COST_NAME, LLM_SERVICE_COST_DESC, LLM_SERVICE_COST_UNIT, false, "model_id"));
        metrics.put(LLM_SERVICE_INPUT_COST_NAME, new RawMetric(GAUGE, LLM_SERVICE_INPUT_COST_NAME, LLM_SERVICE_INPUT_COST_DESC, LLM_SERVICE_INPUT_COST_UNIT, false, "model_id"));
        metrics.put(LLM_SERVICE_OUTPUT_COST_NAME, new RawMetric(GAUGE, LLM_SERVICE_OUTPUT_COST_NAME, LLM_SERVICE_OUTPUT_COST_DESC, LLM_SERVICE_OUTPUT_COST_UNIT, false, "model_id"));
        metrics.put(LLM_SERVICE_TOKEN_NAME, new RawMetric(GAUGE, LLM_SERVICE_TOKEN_NAME, LLM_SERVICE_TOKEN_DESC, LLM_SERVICE_TOKEN_UNIT, false, "model_id"));
        metrics.put(LLM_SERVICE_INPUT_TOKEN_NAME, new RawMetric(GAUGE, LLM_SERVICE_INPUT_TOKEN_NAME, LLM_SERVICE_INPUT_TOKEN_DESC, LLM_SERVICE_INPUT_TOKEN_UNIT, false, "model_id"));
        metrics.put(LLM_SERVICE_OUTPUT_TOKEN_NAME, new RawMetric(GAUGE, LLM_SERVICE_OUTPUT_TOKEN_NAME, LLM_SERVICE_OUTPUT_TOKEN_DESC, LLM_SERVICE_OUTPUT_TOKEN_UNIT, false, "model_id"));
        metrics.put(LLM_SERVICE_REQ_COUNT_NAME, new RawMetric(UPDOWN_COUNTER, LLM_SERVICE_REQ_COUNT_NAME, LLM_SERVICE_REQ_COUNT_DESC, LLM_SERVICE_REQ_COUNT_UNIT, false, "model_id"));
        
        RAW_METRICS = Collections.unmodifiableMap(metrics);
    }

    private LLMRawMetricRegistry() {
        // Private constructor to prevent instantiation
    }

    public static Map<String, RawMetric> getRawMetrics() {
        return RAW_METRICS;
    }
}
