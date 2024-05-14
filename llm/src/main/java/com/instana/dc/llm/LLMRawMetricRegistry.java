/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.llm;

import com.instana.dc.RawMetric;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.instana.dc.InstrumentType.GAUGE;
import static com.instana.dc.InstrumentType.UPDOWN_COUNTER;
import static com.instana.dc.llm.LLMDcUtil.*;

public class LLMRawMetricRegistry {
    private final Map<String, RawMetric> map = new ConcurrentHashMap<String, RawMetric>() {{
        put(LLM_STATUS_NAME, new RawMetric(GAUGE, LLM_STATUS_NAME, LLM_STATUS_DESC, LLM_STATUS_UNIT, true, null));
        put(LLM_DURATION_NAME, new RawMetric(GAUGE, LLM_DURATION_NAME, LLM_DURATION_DESC, LLM_DURATION_UNIT, true, "model_id"));
        put(LLM_DURATION_MAX_NAME, new RawMetric(GAUGE, LLM_DURATION_MAX_NAME, LLM_DURATION_MAX_DESC, LLM_DURATION_MAX_UNIT, true, "model_id"));
        put(LLM_COST_NAME, new RawMetric(GAUGE, LLM_COST_NAME, LLM_COST_DESC, LLM_COST_UNIT, false, "model_id"));
        put(LLM_TOKEN_NAME, new RawMetric(GAUGE, LLM_TOKEN_NAME, LLM_TOKEN_DESC, LLM_TOKEN_UNIT, false, "model_id"));
        put(LLM_REQ_COUNT_NAME, new RawMetric(UPDOWN_COUNTER, LLM_REQ_COUNT_NAME, LLM_REQ_COUNT_DESC, LLM_REQ_COUNT_UNIT, false, "model_id"));
    }};

    public Map<String, RawMetric> getMap() {
        return map;
    }
}
