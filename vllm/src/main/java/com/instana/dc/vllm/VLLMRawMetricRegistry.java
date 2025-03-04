/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.vllm;

import static com.instana.dc.InstrumentType.GAUGE;
import static com.instana.dc.vllm.VLLMDcConstants.SERVICE_NAME;
import static com.instana.dc.vllm.VLLMDcConstants.VllmMetric.VLLM_GENERATION_TOKENS;
import static com.instana.dc.vllm.VLLMDcConstants.VllmMetric.VLLM_GPU_CACHE_HIT_RATE;
import static com.instana.dc.vllm.VLLMDcConstants.VllmMetric.VLLM_GPU_CACHE_USAGE_PERC;
import static com.instana.dc.vllm.VLLMDcConstants.VllmMetric.VLLM_PROMPT_TOKENS;
import static com.instana.dc.vllm.VLLMDcConstants.VllmMetric.VLLM_REQUEST_LATENCY;
import static com.instana.dc.vllm.VLLMDcConstants.VllmMetric.VLLM_REQUEST_TTFT;
import static com.instana.dc.vllm.VLLMDcConstants.VllmMetric.VLLM_RUNNING_REQUESTS;
import static com.instana.dc.vllm.VLLMDcConstants.VllmMetric.VLLM_STATUS;
import static com.instana.dc.vllm.VLLMDcConstants.VllmMetric.VLLM_TOTAL_TOKENS;
import static com.instana.dc.vllm.VLLMDcConstants.VllmMetric.VLLM_WAITING_REQUESTS;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.instana.dc.RawMetric;

public class VLLMRawMetricRegistry {
    private final Map<String, RawMetric> map = new ConcurrentHashMap<>() {{
        put(VLLM_STATUS.getName(), new RawMetric(GAUGE, VLLM_STATUS.getName(), VLLM_STATUS.getDescription(), VLLM_STATUS.getUnit(), true, null));
        put(VLLM_RUNNING_REQUESTS.getName(), new RawMetric(GAUGE, VLLM_RUNNING_REQUESTS.getName(), VLLM_RUNNING_REQUESTS.getDescription(), VLLM_RUNNING_REQUESTS.getUnit(), false, SERVICE_NAME));
        put(VLLM_WAITING_REQUESTS.getName(), new RawMetric(GAUGE, VLLM_WAITING_REQUESTS.getName(), VLLM_WAITING_REQUESTS.getDescription(), VLLM_WAITING_REQUESTS.getUnit(), false, SERVICE_NAME));
        put(VLLM_GPU_CACHE_USAGE_PERC.getName(), new RawMetric(GAUGE, VLLM_GPU_CACHE_USAGE_PERC.getName(), VLLM_GPU_CACHE_USAGE_PERC.getDescription(), VLLM_GPU_CACHE_USAGE_PERC.getUnit(), false, SERVICE_NAME));
        put(VLLM_GPU_CACHE_HIT_RATE.getName(), new RawMetric(GAUGE, VLLM_GPU_CACHE_HIT_RATE.getName(), VLLM_GPU_CACHE_HIT_RATE.getDescription(), VLLM_GPU_CACHE_HIT_RATE.getUnit(), false, SERVICE_NAME));
        put(VLLM_REQUEST_LATENCY.getName(), new RawMetric(GAUGE, VLLM_REQUEST_LATENCY.getName(), VLLM_REQUEST_LATENCY.getDescription(), VLLM_REQUEST_LATENCY.getUnit(), false, SERVICE_NAME));
        put(VLLM_REQUEST_TTFT.getName(), new RawMetric(GAUGE, VLLM_REQUEST_TTFT.getName(), VLLM_REQUEST_TTFT.getDescription(), VLLM_REQUEST_TTFT.getUnit(), false, SERVICE_NAME));
        put(VLLM_PROMPT_TOKENS.getName(), new RawMetric(GAUGE, VLLM_PROMPT_TOKENS.getName(), VLLM_PROMPT_TOKENS.getDescription(), VLLM_PROMPT_TOKENS.getUnit(), false, SERVICE_NAME));
        put(VLLM_GENERATION_TOKENS.getName(), new RawMetric(GAUGE, VLLM_GENERATION_TOKENS.getName(), VLLM_GENERATION_TOKENS.getDescription(), VLLM_GENERATION_TOKENS.getUnit(), false, SERVICE_NAME));
        put(VLLM_TOTAL_TOKENS.getName(), new RawMetric(GAUGE, VLLM_TOTAL_TOKENS.getName(), VLLM_TOTAL_TOKENS.getDescription(), VLLM_TOTAL_TOKENS.getUnit(), false, SERVICE_NAME));
    }};

    public Map<String, RawMetric> getMap() {
        return map;
    }
}
