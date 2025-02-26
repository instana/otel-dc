/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.vllm;

public class VLLMDcUtil {

    /* Configurations for the Data Collector:
     */
    public static final String SERVICE_LISTEN_PORT = "otel.service.port";
    public static final String OTEL_AGENTLESS_MODE = "otel.agentless.mode";

    /* Configurations for Metrics:
     */
    public static final String VLLM_STATUS_NAME = "vllm.status";
    public static final String VLLM_STATUS_DESC = "The status of the LLM dc";
    public static final String VLLM_STATUS_UNIT = "{status}";

    public static final String VLLM_REQ_COUNT_NAME = "vllm.request.count";
    public static final String VLLM_REQ_COUNT_DESC = "The total count of LLM calls by interval";
    public static final String VLLM_REQ_COUNT_UNIT = "{count}";

    public static final String VLLM_PENDING_REQ_COUNT_NAME = "vllm.request.pending";
    public static final String VLLM_PENDING_REQ_COUNT_DESC = "The total count of pending LLM calls by interval";
    public static final String VLLM_PENDING_REQ_COUNT_UNIT = "{count}";

    public static final String VLLM_GPU_CACHE_USAGE_PERC_NAME = "vllm.gpu.cache.usage.perc";
    public static final String VLLM_GPU_CACHE_USAGE_PERC_DESC = "The GPU cache usage perc";
    public static final String VLLM_GPU_CACHE_USAGE_PERC_UNIT = "{perc}";

    public static final String VLLM_GPU_CACHE_HIT_RATE_NAME = "vllm.gpu.cache.hit.rate";
    public static final String VLLM_GPU_CACHE_HIT_RATE_DESC = "The GPU cache hit rate";
    public static final String VLLM_GPU_CACHE_HIT_RATE_UNIT = "{rate}";

    public static final String VLLM_REQUEST_LATENCY_NAME = "vllm.request.latency";
    public static final String VLLM_REQUEST_LATENCY_DESC = "The LLM request latency";
    public static final String VLLM_REQUEST_LATENCY_UNIT = "{seconds}";

    public static final String VLLM_REQUEST_TTFT_NAME = "vllm.request.ttft";
    public static final String VLLM_REQUEST_TTFT_DESC = "The LLM TTFT";
    public static final String VLLM_REQUEST_TTFT_UNIT = "{seconds}";

    public static final String VLLM_PROMPT_TOKENS_NAME = "vllm.prompt.tokens";
    public static final String VLLM_PROMPT_TOKENS_DESC = "The LLM prompt tokens";
    public static final String VLLM_PROMPT_TOKENS_UNIT = "{count}";

    public static final String VLLM_GENERATION_TOKENS_NAME = "vllm.generation.tokens";
    public static final String VLLM_GENERATION_TOKENS_DESC = "The LLM generation tokens";
    public static final String VLLM_GENERATION_TOKENS_UNIT = "{count}";

    public static final String VLLM_TOTAL_TOKENS_NAME = "vllm.total.tokens";
    public static final String VLLM_TOTAL_TOKENS_DESC = "The LLM total tokens";
    public static final String VLLM_TOTAL_TOKENS_UNIT = "{count}";

}