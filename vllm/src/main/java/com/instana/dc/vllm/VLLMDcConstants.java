/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.vllm;

public class VLLMDcConstants {

    /* Configurations for the Data Collector:
     */
    public static final String SERVICE_LISTEN_PORT = "otel.service.port";
    public static final String OTEL_AGENTLESS_MODE = "otel.agentless.mode";
    public static final String INSTANCE_ID = "service.instance.id";

    public static final String MODEL_NAME = "model_name";
    public static final String SERVICE_NAME = "service_name";

    public static final String VLLM_METRICS_URLS = "otel.vllm.metrics.url";

    /* Configurations for Metrics:
     */
    public enum VllmMetric {

        VLLM_STATUS("vllm.status", "The status of the vLLM data collector", "{status}"),
        VLLM_RUNNING_REQUESTS("vllm.request.running.count", "Number of requests currently running on GPU", "{count}"),
        VLLM_WAITING_REQUESTS("vllm.request.waiting.count", "Number of requests waiting to be processed", "{count}"),
        VLLM_GPU_CACHE_USAGE_PERC("vllm.gpu.cache.usage.perc", "GPU KV-cache usage", "{perc}"),
        VLLM_GPU_CACHE_HIT_RATE("vllm.gpu.cache.hit.rate", "GPU prefix cache block hit rate", "{rate}"),
        VLLM_REQUEST_LATENCY("vllm.request.latency", "End to end request latency", "{seconds}"),
        VLLM_REQUEST_TTFT("vllm.request.ttft", "Time to first token", "{seconds}"),
        VLLM_PROMPT_TOKENS("vllm.tokens.prompt.count", "Number of prefill tokens processed", "{count}"),
        VLLM_GENERATION_TOKENS("vllm.tokens.generation.count", "Number of generation tokens processed", "{count}"),
        VLLM_TOTAL_TOKENS("vllm.tokens.total.count", "Total number of tokens processed", "{count}");

        private final String name;
        private final String description;
        private final String unit;

        VllmMetric(String name, String description, String unit) {
            this.name = name;
            this.description = description;
            this.unit = unit;
        }

        // Getter methods
        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getUnit() {
            return unit;
        }
    }

}