/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.vllm.impl.vllm;

import static com.instana.dc.DcUtil.CALLBACK_INTERVAL;
import static com.instana.dc.DcUtil.POLLING_INTERVAL;
import static com.instana.dc.vllm.VLLMDcUtil.OTEL_AGENTLESS_MODE;
import static com.instana.dc.vllm.VLLMDcUtil.SERVICE_LISTEN_PORT;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_GENERATION_TOKENS_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_GPU_CACHE_HIT_RATE_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_GPU_CACHE_USAGE_PERC_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_PENDING_REQ_COUNT_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_PROMPT_TOKENS_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_REQUEST_LATENCY_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_REQUEST_TTFT_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_REQ_COUNT_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_STATUS_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_TOTAL_TOKENS_NAME;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.instana.dc.vllm.AbstractVLLMDc;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.server.healthcheck.HealthCheckService;

@SuppressWarnings("null")
public class VLLMDc extends AbstractVLLMDc {
    private static final Logger logger = Logger.getLogger(VLLMDc.class.getName());

    private final MetricsCollectorService metricsCollector = new MetricsCollectorService();
    private final int listenPort;
    private final Boolean otelAgentlessMode;
    private final Integer otelPollInterval;

    /**
     * The poll rate in the configuration, in seconds. In other words, the number of
     * seconds between calls to LLM.
     */
    public VLLMDc(Map<String, Object> properties) {
        super(properties);
        otelAgentlessMode = (Boolean) properties.getOrDefault(OTEL_AGENTLESS_MODE, Boolean.FALSE);
        Integer callbackInterval = (Integer) properties.getOrDefault(CALLBACK_INTERVAL, DEFAULT_LLM_CLBK_INTERVAL);
        otelPollInterval = (Integer) properties.getOrDefault(POLLING_INTERVAL, callbackInterval);
        listenPort = (int) properties.getOrDefault(SERVICE_LISTEN_PORT, 8000);
    }

    @Override
    public void initOnce() {

        var server = Server.builder()
                .http(listenPort)
                .service(
                        GrpcService.builder()
                                .addService(metricsCollector)
                                .build())
                .service(
                        "/",
                        (ctx, req) -> {
                            var requests = metricsCollector.getDeltaMetricsList();
                            if (requests != null) {
                                return HttpResponse.of(
                                        HttpStatus.OK, MediaType.JSON, HttpData.wrap("OK".getBytes()));
                            } else {
                                return HttpResponse.of(
                                        HttpStatus.BAD_REQUEST, MediaType.JSON,
                                        HttpData.wrap("Bad Request".getBytes()));
                            }
                        })
                .service("/health", HealthCheckService.of())
                .build();

        server.start().join();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop().join()));
    }

    @Override
    public void collectData() {
        logger.info("Start to collect metrics");

        List<MetricsCollectorService.OtelMetric> otelMetrics = metricsCollector.getDeltaMetricsList();
        int divisor = Boolean.TRUE.equals(otelAgentlessMode) ? 1 : otelPollInterval;

        for (MetricsCollectorService.OtelMetric metric : otelMetrics) {
            Map<String, Object> attributes = Map.of("service_name", metric.getServiceName());

            getRawMetric(VLLM_REQ_COUNT_NAME).getDataPoint(metric.getServiceName()).setValue(aggregate(metric, VLLM_REQ_COUNT_NAME), attributes);
            getRawMetric(VLLM_PENDING_REQ_COUNT_NAME).getDataPoint(metric.getServiceName()).setValue(aggregate(metric, VLLM_PENDING_REQ_COUNT_NAME), attributes);
            getRawMetric(VLLM_GPU_CACHE_USAGE_PERC_NAME).getDataPoint(metric.getServiceName()).setValue(aggregate(metric, VLLM_GPU_CACHE_USAGE_PERC_NAME), attributes);
            getRawMetric(VLLM_GPU_CACHE_HIT_RATE_NAME).getDataPoint(metric.getServiceName()).setValue(aggregate(metric, VLLM_GPU_CACHE_HIT_RATE_NAME), attributes);

            getRawMetric(VLLM_REQUEST_LATENCY_NAME).getDataPoint(metric.getServiceName()).setValue(aggregate(metric, "llm.total.duration") / aggregate(metric, "llm.total.requests"), attributes);
            getRawMetric(VLLM_REQUEST_TTFT_NAME).getDataPoint(metric.getServiceName()).setValue(aggregate(metric, "llm.total.ttft.duration") / aggregate(metric, "llm.total.ttft.requests"), attributes);

            double promptTokens = aggregate(metric, VLLM_PROMPT_TOKENS_NAME);
            double generationTokens = aggregate(metric, VLLM_GENERATION_TOKENS_NAME);
            getRawMetric(VLLM_PROMPT_TOKENS_NAME).getDataPoint(metric.getServiceName()).setValue(promptTokens / divisor, attributes);
            getRawMetric(VLLM_GENERATION_TOKENS_NAME).getDataPoint(metric.getServiceName()).setValue(generationTokens / divisor, attributes);
            getRawMetric(VLLM_TOTAL_TOKENS_NAME).getDataPoint(metric.getServiceName()).setValue((promptTokens + generationTokens) / divisor, attributes);

            getRawMetric(VLLM_STATUS_NAME).setValue(1);
        }

        logger.info("-----------------------------------------");
    }

    private static double aggregate(MetricsCollectorService.OtelMetric metric, String metricName) {
        return metric.getMetrics().get(metricName).values().stream().mapToDouble(MetricsCollectorService.OtelMetric.Measure::getValue).sum();
    }

}
