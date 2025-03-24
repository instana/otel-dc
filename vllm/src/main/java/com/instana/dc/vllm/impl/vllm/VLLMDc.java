/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.vllm.impl.vllm;

import static com.instana.dc.DcUtil.CALLBACK_INTERVAL;
import static com.instana.dc.DcUtil.POLLING_INTERVAL;
import static com.instana.dc.vllm.VLLMDcConstants.*;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToDoubleFunction;
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

    private final Boolean otelAgentlessMode;
    private final Integer otelPollInterval;
    private final String vllmMetricsUrl;
    PrometheusToOTLPConverter scraper = new PrometheusToOTLPConverter();


    /**
     * The poll rate in the configuration, in seconds. In other words, the number of
     * seconds between calls to LLM.
     */
    public VLLMDc(Map<String, Object> properties) {
        super(properties);
        otelAgentlessMode = (Boolean) properties.getOrDefault(OTEL_AGENTLESS_MODE, Boolean.FALSE);
        Integer callbackInterval = (Integer) properties.getOrDefault(CALLBACK_INTERVAL, DEFAULT_LLM_CLBK_INTERVAL);
        otelPollInterval = (Integer) properties.getOrDefault(POLLING_INTERVAL, callbackInterval);
        vllmMetricsUrl = properties.get(VLLM_METRICS_URL)+"/metrics";
    }


    @Override
    public void collectData() {

        logger.info("Start to collect metrics");

        scraper.scrapeMetrics(vllmMetricsUrl);
        List<PrometheusToOTLPConverter.MetricsAggregation> metricsAggregations = scraper.getDeltaMetricsList();

        int divisor = Boolean.TRUE.equals(otelAgentlessMode) ? 1 : otelPollInterval;

        for (PrometheusToOTLPConverter.MetricsAggregation metric : metricsAggregations) {
            Map<String, Object> attributes = Map.of(SERVICE_NAME, metric.getInstance());

            getRawMetric(VLLM_RUNNING_REQUESTS.getName()).getDataPoint(metric.getInstance()).setValue(aggregate(metric, "vllm:num_requests_running", PrometheusToOTLPConverter.MetricsAggregation.Measurement::getValue), attributes);
            getRawMetric(VLLM_WAITING_REQUESTS.getName()).getDataPoint(metric.getInstance()).setValue(aggregate(metric, "vllm:num_requests_waiting", PrometheusToOTLPConverter.MetricsAggregation.Measurement::getValue), attributes);
            getRawMetric(VLLM_GPU_CACHE_USAGE_PERC.getName()).getDataPoint(metric.getInstance()).setValue(aggregate(metric, "vllm:gpu_cache_usage_perc", PrometheusToOTLPConverter.MetricsAggregation.Measurement::getValue), attributes);
            getRawMetric(VLLM_GPU_CACHE_HIT_RATE.getName()).getDataPoint(metric.getInstance()).setValue(aggregate(metric, "vllm:gpu_prefix_cache_hit_rate", PrometheusToOTLPConverter.MetricsAggregation.Measurement::getValue), attributes);

            double promptTokens = aggregate(metric, "vllm:prompt_tokens_total", PrometheusToOTLPConverter.MetricsAggregation.Measurement::getValue);
            double generationTokens = aggregate(metric, "vllm:generation_tokens_total", PrometheusToOTLPConverter.MetricsAggregation.Measurement::getValue);
            getRawMetric(VLLM_PROMPT_TOKENS.getName()).getDataPoint(metric.getInstance()).setValue(promptTokens / divisor, attributes);
            getRawMetric(VLLM_GENERATION_TOKENS.getName()).getDataPoint(metric.getInstance()).setValue(generationTokens / divisor, attributes);
            getRawMetric(VLLM_TOTAL_TOKENS.getName()).getDataPoint(metric.getInstance()).setValue((promptTokens + generationTokens) / divisor, attributes);

            double count = aggregate(metric, "vllm:e2e_request_latency_seconds", PrometheusToOTLPConverter.MetricsAggregation.Measurement::getCount);
            if (count > 0) {
                getRawMetric(VLLM_REQUEST_LATENCY.getName()).getDataPoint(metric.getInstance()).setValue(aggregate(metric, "vllm:e2e_request_latency_seconds", PrometheusToOTLPConverter.MetricsAggregation.Measurement::getSum) / count, attributes);
            }
            count = aggregate(metric, "vllm:time_to_first_token_seconds", PrometheusToOTLPConverter.MetricsAggregation.Measurement::getCount);
            if (count > 0) {
                getRawMetric(VLLM_REQUEST_TTFT.getName()).getDataPoint(metric.getInstance()).setValue(aggregate(metric, "vllm:time_to_first_token_seconds", PrometheusToOTLPConverter.MetricsAggregation.Measurement::getSum) / count, attributes);
            }
            getRawMetric(VLLM_STATUS.getName()).setValue(1);
        }

        logger.info("-----------------------------------------");
    }

    private static double aggregate(PrometheusToOTLPConverter.MetricsAggregation metricsAggregation, String metricName,
                                    ToDoubleFunction<PrometheusToOTLPConverter.MetricsAggregation.Measurement> getter) {
        return Optional.ofNullable(metricsAggregation.getMetrics())
                .map(metrics -> metrics.get(metricName))
                .map(metric -> metric.values().stream()
                        .mapToDouble(getter).sum())
                .orElse(0.0);
    }

}
