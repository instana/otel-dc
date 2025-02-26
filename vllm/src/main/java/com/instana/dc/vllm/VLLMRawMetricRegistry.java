/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.vllm;

import static com.instana.dc.InstrumentType.GAUGE;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_GENERATION_TOKENS_DESC;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_GENERATION_TOKENS_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_GENERATION_TOKENS_UNIT;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_GPU_CACHE_HIT_RATE_DESC;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_GPU_CACHE_HIT_RATE_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_GPU_CACHE_HIT_RATE_UNIT;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_GPU_CACHE_USAGE_PERC_DESC;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_GPU_CACHE_USAGE_PERC_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_GPU_CACHE_USAGE_PERC_UNIT;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_PENDING_REQ_COUNT_DESC;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_PENDING_REQ_COUNT_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_PENDING_REQ_COUNT_UNIT;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_PROMPT_TOKENS_DESC;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_PROMPT_TOKENS_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_PROMPT_TOKENS_UNIT;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_REQUEST_LATENCY_DESC;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_REQUEST_LATENCY_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_REQUEST_LATENCY_UNIT;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_REQUEST_TTFT_DESC;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_REQUEST_TTFT_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_REQUEST_TTFT_UNIT;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_REQ_COUNT_DESC;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_REQ_COUNT_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_REQ_COUNT_UNIT;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_STATUS_DESC;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_STATUS_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_STATUS_UNIT;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_TOTAL_TOKENS_DESC;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_TOTAL_TOKENS_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_TOTAL_TOKENS_UNIT;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.instana.dc.RawMetric;

public class VLLMRawMetricRegistry {
    private final Map<String, RawMetric> map = new ConcurrentHashMap<>() {{
        put(VLLM_STATUS_NAME, new RawMetric(GAUGE, VLLM_STATUS_NAME, VLLM_STATUS_DESC, VLLM_STATUS_UNIT, true, null));
        put(VLLM_REQ_COUNT_NAME, new RawMetric(GAUGE, VLLM_REQ_COUNT_NAME, VLLM_REQ_COUNT_DESC, VLLM_REQ_COUNT_UNIT, false, "servive_name"));
        put(VLLM_PENDING_REQ_COUNT_NAME, new RawMetric(GAUGE, VLLM_PENDING_REQ_COUNT_NAME, VLLM_PENDING_REQ_COUNT_DESC, VLLM_PENDING_REQ_COUNT_UNIT, false, "servive_name"));
        put(VLLM_GPU_CACHE_USAGE_PERC_NAME, new RawMetric(GAUGE, VLLM_GPU_CACHE_USAGE_PERC_NAME, VLLM_GPU_CACHE_USAGE_PERC_DESC, VLLM_GPU_CACHE_USAGE_PERC_UNIT, false, "servive_name"));
        put(VLLM_GPU_CACHE_HIT_RATE_NAME, new RawMetric(GAUGE, VLLM_GPU_CACHE_HIT_RATE_NAME, VLLM_GPU_CACHE_HIT_RATE_DESC, VLLM_GPU_CACHE_HIT_RATE_UNIT, false, "servive_name"));
        put(VLLM_REQUEST_LATENCY_NAME, new RawMetric(GAUGE, VLLM_REQUEST_LATENCY_NAME, VLLM_REQUEST_LATENCY_DESC, VLLM_REQUEST_LATENCY_UNIT, false, "servive_name"));
        put(VLLM_REQUEST_TTFT_NAME, new RawMetric(GAUGE, VLLM_REQUEST_TTFT_NAME, VLLM_REQUEST_TTFT_DESC, VLLM_REQUEST_TTFT_UNIT, false, "servive_name"));
        put(VLLM_PROMPT_TOKENS_NAME, new RawMetric(GAUGE, VLLM_PROMPT_TOKENS_NAME, VLLM_PROMPT_TOKENS_DESC, VLLM_PROMPT_TOKENS_UNIT, false, "servive_name"));
        put(VLLM_GENERATION_TOKENS_NAME, new RawMetric(GAUGE, VLLM_GENERATION_TOKENS_NAME, VLLM_GENERATION_TOKENS_DESC, VLLM_GENERATION_TOKENS_UNIT, false, "servive_name"));
        put(VLLM_TOTAL_TOKENS_NAME, new RawMetric(GAUGE, VLLM_TOTAL_TOKENS_NAME, VLLM_TOTAL_TOKENS_DESC, VLLM_TOTAL_TOKENS_UNIT, false, "servive_name"));
    }};

    public Map<String, RawMetric> getMap() {
        return map;
    }
}
