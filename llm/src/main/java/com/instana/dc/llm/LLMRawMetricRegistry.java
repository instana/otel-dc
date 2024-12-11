/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.llm;

import static com.instana.dc.InstrumentType.GAUGE;
import static com.instana.dc.InstrumentType.UPDOWN_COUNTER;
import static com.instana.dc.llm.LLMDcUtil.LLM_COST_DESC;
import static com.instana.dc.llm.LLMDcUtil.LLM_COST_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_COST_UNIT;
import static com.instana.dc.llm.LLMDcUtil.LLM_DURATION_DESC;
import static com.instana.dc.llm.LLMDcUtil.LLM_DURATION_MAX_DESC;
import static com.instana.dc.llm.LLMDcUtil.LLM_DURATION_MAX_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_DURATION_MAX_UNIT;
import static com.instana.dc.llm.LLMDcUtil.LLM_DURATION_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_DURATION_UNIT;
import static com.instana.dc.llm.LLMDcUtil.LLM_INPUT_COST_DESC;
import static com.instana.dc.llm.LLMDcUtil.LLM_INPUT_COST_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_INPUT_COST_UNIT;
import static com.instana.dc.llm.LLMDcUtil.LLM_INPUT_TOKEN_DESC;
import static com.instana.dc.llm.LLMDcUtil.LLM_INPUT_TOKEN_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_INPUT_TOKEN_UNIT;
import static com.instana.dc.llm.LLMDcUtil.LLM_OUTPUT_COST_DESC;
import static com.instana.dc.llm.LLMDcUtil.LLM_OUTPUT_COST_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_OUTPUT_COST_UNIT;
import static com.instana.dc.llm.LLMDcUtil.LLM_OUTPUT_TOKEN_DESC;
import static com.instana.dc.llm.LLMDcUtil.LLM_OUTPUT_TOKEN_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_OUTPUT_TOKEN_UNIT;
import static com.instana.dc.llm.LLMDcUtil.LLM_REQ_COUNT_DESC;
import static com.instana.dc.llm.LLMDcUtil.LLM_REQ_COUNT_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_REQ_COUNT_UNIT;
import static com.instana.dc.llm.LLMDcUtil.LLM_STATUS_DESC;
import static com.instana.dc.llm.LLMDcUtil.LLM_STATUS_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_STATUS_UNIT;
import static com.instana.dc.llm.LLMDcUtil.LLM_TOKEN_DESC;
import static com.instana.dc.llm.LLMDcUtil.LLM_TOKEN_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_TOKEN_UNIT;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.instana.dc.RawMetric;

public class LLMRawMetricRegistry {
    private final Map<String, RawMetric> map = new ConcurrentHashMap<String, RawMetric>() {{
        put(LLM_STATUS_NAME, new RawMetric(GAUGE, LLM_STATUS_NAME, LLM_STATUS_DESC, LLM_STATUS_UNIT, true, null));
        put(LLM_DURATION_NAME, new RawMetric(GAUGE, LLM_DURATION_NAME, LLM_DURATION_DESC, LLM_DURATION_UNIT, true, "model_id"));
        put(LLM_DURATION_MAX_NAME, new RawMetric(GAUGE, LLM_DURATION_MAX_NAME, LLM_DURATION_MAX_DESC, LLM_DURATION_MAX_UNIT, true, "model_id"));
        put(LLM_COST_NAME, new RawMetric(GAUGE, LLM_COST_NAME, LLM_COST_DESC, LLM_COST_UNIT, false, "model_id"));
        put(LLM_INPUT_COST_NAME, new RawMetric(GAUGE, LLM_INPUT_COST_NAME, LLM_INPUT_COST_DESC, LLM_INPUT_COST_UNIT, false, "model_id"));
        put(LLM_OUTPUT_COST_NAME, new RawMetric(GAUGE, LLM_OUTPUT_COST_NAME, LLM_OUTPUT_COST_DESC, LLM_OUTPUT_COST_UNIT, false, "model_id"));
        put(LLM_TOKEN_NAME, new RawMetric(GAUGE, LLM_TOKEN_NAME, LLM_TOKEN_DESC, LLM_TOKEN_UNIT, false, "model_id"));
        put(LLM_INPUT_TOKEN_NAME, new RawMetric(GAUGE, LLM_INPUT_TOKEN_NAME, LLM_INPUT_TOKEN_DESC, LLM_INPUT_TOKEN_UNIT, false, "model_id"));
        put(LLM_OUTPUT_TOKEN_NAME, new RawMetric(GAUGE, LLM_OUTPUT_TOKEN_NAME, LLM_OUTPUT_TOKEN_DESC, LLM_OUTPUT_TOKEN_UNIT, false, "model_id"));
        put(LLM_REQ_COUNT_NAME, new RawMetric(UPDOWN_COUNTER, LLM_REQ_COUNT_NAME, LLM_REQ_COUNT_DESC, LLM_REQ_COUNT_UNIT, false, "model_id"));
    }};

    public Map<String, RawMetric> getMap() {
        return map;
    }
}
