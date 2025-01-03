/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.llm.impl.llm;

import static com.instana.dc.DcUtil.CALLBACK_INTERVAL;
import static com.instana.dc.DcUtil.POLLING_INTERVAL;
import static com.instana.dc.llm.LLMDcUtil.LLM_COST_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_DURATION_MAX_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_DURATION_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_INPUT_COST_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_INPUT_TOKEN_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_OUTPUT_COST_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_OUTPUT_TOKEN_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_PRICES_PROPERTIES;
import static com.instana.dc.llm.LLMDcUtil.LLM_REQ_COUNT_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_SERVICE_COST_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_SERVICE_INPUT_COST_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_SERVICE_INPUT_TOKEN_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_SERVICE_OUTPUT_COST_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_SERVICE_OUTPUT_TOKEN_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_SERVICE_REQ_COUNT_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_SERVICE_TOKEN_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_STATUS_NAME;
import static com.instana.dc.llm.LLMDcUtil.LLM_TOKEN_NAME;
import static com.instana.dc.llm.LLMDcUtil.OTEL_AGENTLESS_MODE;
import static com.instana.dc.llm.LLMDcUtil.SERVICE_LISTEN_PORT;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.instana.dc.llm.AbstractLLMDc;
import com.instana.dc.llm.DataCollector.CustomDcConfig;
import com.instana.dc.llm.impl.llm.MetricsCollectorService.OtelMetric;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.server.healthcheck.HealthCheckService;

@SuppressWarnings("null")
public class LLMDc extends AbstractLLMDc {
    private static final Logger logger = Logger.getLogger(LLMDc.class.getName());

    private final HashMap<String, ModelAggregation> modelAggrMap = new HashMap<>();
    private final HashMap<String, Map<String, ModelAggregation>> serviceModelAggrMap = new HashMap<>();
    private final MetricsCollectorService metricsCollector = new MetricsCollectorService();
    private final Boolean otelAgentlessMode;
    private final Integer otelPollInterval;
    private final HashMap<String, Double> llmTokenPrices = new HashMap<>();
    private final int listenPort;

    /**
     * The poll rate in the configuration, in seconds. In other words, the number of
     * seconds between calls to Watsonx.
     */

    private static class ModelAggregation {
        private final String modelId;
        private final String aiSystem;
        private long deltaInputTokens;
        private long deltaOutputTokens;
        private long deltaDuration;
        private long deltaRequestCount;
        private long maxDurationSoFar;
        
        public ModelAggregation(String modelId, String aiSystem) {
            this.modelId = modelId;
            this.aiSystem = aiSystem;
        }
        public String getModelId() {
            return modelId;
        }
        public String getAiSystem() {
            return aiSystem;
        }
        public long getDeltaInputTokens() {
            return deltaInputTokens;
        }
        public long getDeltaOutputTokens() {
            return deltaOutputTokens;
        }
        public long getDeltaDuration() {
            return deltaDuration;
        }
        public long getDeltaRequestCount() {
            return deltaRequestCount;
        }
        public long getMaxDurationSoFar() {
            return maxDurationSoFar;
        }
        public void addDeltaInputTokens(long inputTokens) {
            deltaInputTokens += inputTokens;
        }
        public void addDeltaOutputTokens(long outputTokens) {
            deltaOutputTokens += outputTokens;
        }
        public void addDeltaDuration(long duration) {
            deltaDuration += duration;
        }
        public void addDeltaRequestCount(long requestCount) {
            deltaRequestCount += requestCount;
        }
        public void setMaxDurationSoFar(long maxDuration) {
            maxDurationSoFar = maxDuration;
        }

        public void resetDeltaValues() {
            deltaInputTokens = 0;
            deltaOutputTokens = 0;
            deltaDuration = 0;
            deltaRequestCount = 0;
        }
    }

    public LLMDc(Map<String, Object> properties, CustomDcConfig cdcConfig) throws Exception {
        super(properties, cdcConfig);
        otelAgentlessMode = (Boolean) properties.getOrDefault(OTEL_AGENTLESS_MODE, Boolean.FALSE);
        Integer callbackInterval = (Integer) properties.getOrDefault(CALLBACK_INTERVAL, DEFAULT_LLM_CLBK_INTERVAL);
        otelPollInterval = (Integer) properties.getOrDefault(POLLING_INTERVAL, callbackInterval);
        listenPort = (int) properties.getOrDefault(SERVICE_LISTEN_PORT, 8000);

        String pricePropFile = LLM_PRICES_PROPERTIES;
        try (BufferedReader reader = new BufferedReader(new FileReader(pricePropFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int equalIndex = line.indexOf('=');
                if (equalIndex > 0) {
                    String key = line.substring(0, equalIndex).trim().toLowerCase();
                    String value = line.substring(equalIndex + 1).trim();
                    try {
                        Double dValue = Double.parseDouble(value);
                        llmTokenPrices.put(key, dValue);
                    } catch (Exception e) {
                        llmTokenPrices.put(key, 0.0);
                        logger.warning(value + " cannot be parsed to Double: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Cannot load price properties file: " + e.getMessage());
        }
    }

    @Override
    public void initOnce() throws ClassNotFoundException {

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
    public void registerMetrics() {
        super.registerMetrics();
    }

    @Override
    public void collectData() {
        logger.info("Start to collect metrics");
  
        for(Map.Entry<String,ModelAggregation> entry : modelAggrMap.entrySet()){
            ModelAggregation aggr = entry.getValue();
            aggr.resetDeltaValues();
        }

        List<OtelMetric> otelMetrics = metricsCollector.getDeltaMetricsList();
        metricsCollector.resetMetricsDetla();

        for (OtelMetric metric : otelMetrics) {
            try {
                String modelId = metric.getModelId();
                String aiSystem = metric.getAiSystem();
                String serviceName = metric.getServiceName();
                long inputTokens = metric.getDeltaInputTokens();
                long outputTokens = metric.getDeltaOutputTokens();
                long duration = metric.getDeltaDuration();
                long requestCount = metric.getDeltaRequestCount();

                ModelAggregation modelAggr = modelAggrMap.get(modelId);
                if (modelAggr == null) {
                    modelAggr = new ModelAggregation(modelId, aiSystem);
                    modelAggrMap.put(modelId, modelAggr);
                }
                modelAggr.addDeltaInputTokens(inputTokens);
                modelAggr.addDeltaOutputTokens(outputTokens);
                modelAggr.addDeltaDuration(duration);
                modelAggr.addDeltaRequestCount(requestCount);

                Map<String, ModelAggregation> serviceAggr = serviceModelAggrMap.computeIfAbsent(serviceName, k -> new HashMap<>());
                ModelAggregation model = serviceAggr.computeIfAbsent(modelId, k -> new ModelAggregation(modelId, aiSystem));
                model.addDeltaInputTokens(inputTokens);
                model.addDeltaOutputTokens(outputTokens);
                model.addDeltaDuration(duration);
                model.addDeltaRequestCount(requestCount);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        logger.info("-----------------------------------------");
        int divisor = Boolean.TRUE.equals(otelAgentlessMode) ? 1 : otelPollInterval;

        for(Map.Entry<String,ModelAggregation> entry : modelAggrMap.entrySet()){
            ModelAggregation aggr = entry.getValue();
            String modelId = aggr.getModelId();
            String aiSystem = aggr.getAiSystem();
            long deltaInputTokens = aggr.getDeltaInputTokens();
            long deltaOutputTokens = aggr.getDeltaOutputTokens();
            long deltaDuration = aggr.getDeltaDuration();
            long deltaRequestCount = aggr.getDeltaRequestCount();
            long maxDurationSoFar = aggr.getMaxDurationSoFar();
            aggr.resetDeltaValues();

            long avgDurationPerReq = deltaRequestCount == 0 ? 0 : deltaDuration/deltaRequestCount;
            if(avgDurationPerReq > maxDurationSoFar) {
                maxDurationSoFar = avgDurationPerReq;
                aggr.setMaxDurationSoFar(maxDurationSoFar);
            }

            Double priceInputTokens = getTokenPrice(aiSystem, modelId, "input");
            Double priceOutputTokens = getTokenPrice(aiSystem, modelId, "output");

            double intervalReqCount = (double)deltaRequestCount/divisor;
            double intervalInputTokens = (double)deltaInputTokens/divisor;
            double intervalOutputTokens = (double)deltaOutputTokens/divisor;
            double intervalTotalTokens = intervalInputTokens + intervalOutputTokens;

            double intervalInputCost = intervalInputTokens/1000 * priceInputTokens;
            double intervalOutputCost = intervalOutputTokens/1000 * priceOutputTokens;
            double intervalTotalCost = intervalInputCost + intervalOutputCost;

            // This costs are 10000 times the actual value to prevent very small numbers from being rounded off. 
            // And it will be adjusted to the correct value on UI.
            if (isForceBackwardCompatible()) {
                System.out.printf("FORCE_BACKWARD_COMPATIBLE is set.");        
            } else {
                intervalTotalCost = intervalTotalCost * 10000;
                intervalInputCost = intervalInputCost * 10000;
                intervalOutputCost = intervalOutputCost * 10000;
            }
            
            System.out.printf("Metrics for model %s of %s:%n", modelId, aiSystem);
            System.out.println(" - Average Duration : " + avgDurationPerReq + " ms");
            System.out.println(" - Maximum Duration : " + maxDurationSoFar + " ms");
            System.out.println(" - Interval Tokens  : " + intervalTotalTokens);
            System.out.println(" - Interval Input Tokens  : " + intervalInputTokens);
            System.out.println(" - Interval Output Tokens  : " + intervalOutputTokens);
            System.out.println(" - Interval Cost    : " + intervalTotalCost);
            System.out.println(" - Interval Input Cost    : " + intervalInputCost);
            System.out.println(" - Interval Output Cost    : " + intervalOutputCost);
            System.out.println(" - Interval Request : " + intervalReqCount);

            Map<String, Object> attributes = new HashMap<>();
            String replacedId = modelId.replace(".", "/");
            String modelIdExt = aiSystem + ":" + replacedId;
            attributes.put("model_id", modelIdExt);
            attributes.put("ai_system", aiSystem);
            getRawMetric(LLM_STATUS_NAME).setValue(1);
            getRawMetric(LLM_DURATION_NAME).getDataPoint(modelIdExt).setValue(avgDurationPerReq, attributes);
            getRawMetric(LLM_DURATION_MAX_NAME).getDataPoint(modelIdExt).setValue(maxDurationSoFar, attributes);
            getRawMetric(LLM_COST_NAME).getDataPoint(modelIdExt).setValue(intervalTotalCost, attributes);
            getRawMetric(LLM_INPUT_COST_NAME).getDataPoint(modelIdExt).setValue(intervalInputCost, attributes);
            getRawMetric(LLM_OUTPUT_COST_NAME).getDataPoint(modelIdExt).setValue(intervalOutputCost, attributes);
            getRawMetric(LLM_TOKEN_NAME).getDataPoint(modelIdExt).setValue(intervalTotalTokens, attributes);
            getRawMetric(LLM_INPUT_TOKEN_NAME).getDataPoint(modelIdExt).setValue(intervalInputTokens, attributes);
            getRawMetric(LLM_OUTPUT_TOKEN_NAME).getDataPoint(modelIdExt).setValue(intervalOutputTokens, attributes);
            getRawMetric(LLM_REQ_COUNT_NAME).getDataPoint(modelIdExt).setValue(intervalReqCount, attributes);
        }
        logger.info("-----------------------------------------");

        for (Map.Entry<String, Map<String, ModelAggregation>> serviceAggrEntry : serviceModelAggrMap.entrySet()) {
            double intervalReqCount = 0.0;
            double intervalInputTokens = 0.0, intervalOutputTokens = 0.0, intervalTotalTokens = 0.0;
            double intervalInputCost = 0.0, intervalOutputCost = 0.0, intervalTotalCost = 0.0;
            for (Map.Entry<String, ModelAggregation> entry : serviceAggrEntry.getValue().entrySet()) {
                ModelAggregation aggr = entry.getValue();
                String modelId = aggr.getModelId();
                String aiSystem = aggr.getAiSystem();

                intervalInputTokens += (double) aggr.getDeltaInputTokens() / divisor;
                intervalOutputTokens += (double) aggr.getDeltaOutputTokens() / divisor;
                intervalTotalTokens = intervalInputTokens + intervalOutputTokens;
                aggr.resetDeltaValues();

                intervalInputCost += intervalInputTokens / 1000 * getTokenPrice(aiSystem, modelId, "input");
                intervalOutputCost += intervalOutputTokens / 1000 * getTokenPrice(aiSystem, modelId, "output");
                intervalTotalCost = intervalInputCost + intervalOutputCost;
            }

            // This costs are 10000 times the actual value to prevent very small numbers from being rounded off.
            // And it will be adjusted to the correct value on UI.
            if (isForceBackwardCompatible()) {
                System.out.println("FORCE_BACKWARD_COMPATIBLE is set.");
            } else {
                intervalTotalCost = intervalTotalCost * 10000;
                intervalInputCost = intervalInputCost * 10000;
                intervalOutputCost = intervalOutputCost * 10000;
            }

            String serviceName = serviceAggrEntry.getKey();
            System.out.printf("Metrics for service %s", serviceName);
            System.out.println(" - Interval Tokens  : " + intervalTotalTokens);
            System.out.println(" - Interval Input Tokens  : " + intervalInputTokens);
            System.out.println(" - Interval Output Tokens  : " + intervalOutputTokens);
            System.out.println(" - Interval Cost    : " + intervalTotalCost);
            System.out.println(" - Interval Input Cost    : " + intervalInputCost);
            System.out.println(" - Interval Output Cost    : " + intervalOutputCost);
            System.out.println(" - Interval Request : " + intervalReqCount);

            Map<String, Object> attributes = Map.of("service_name", serviceName);
            getRawMetric(LLM_SERVICE_COST_NAME).getDataPoint(serviceName).setValue(intervalTotalCost, attributes);
            getRawMetric(LLM_SERVICE_INPUT_COST_NAME).getDataPoint(serviceName).setValue(intervalInputCost, attributes);
            getRawMetric(LLM_SERVICE_OUTPUT_COST_NAME).getDataPoint(serviceName).setValue(intervalOutputCost, attributes);
            getRawMetric(LLM_SERVICE_TOKEN_NAME).getDataPoint(serviceName).setValue(intervalTotalTokens, attributes);
            getRawMetric(LLM_SERVICE_INPUT_TOKEN_NAME).getDataPoint(serviceName).setValue(intervalInputTokens, attributes);
            getRawMetric(LLM_SERVICE_OUTPUT_TOKEN_NAME).getDataPoint(serviceName).setValue(intervalOutputTokens, attributes);
            getRawMetric(LLM_SERVICE_REQ_COUNT_NAME).getDataPoint(serviceName).setValue(intervalReqCount, attributes);
        }
        logger.info("-----------------------------------------");
    }

    private static boolean isForceBackwardCompatible() {
        return "true".equalsIgnoreCase(System.getenv("FORCE_BACKWARD_COMPATIBLE"));
    }

    private Double getTokenPrice(String aiSystem, String modelId, String io) {
        return llmTokenPrices.getOrDefault(String.join(".", aiSystem, modelId, io).toLowerCase(),
                llmTokenPrices.getOrDefault((aiSystem + ".*." + io).toLowerCase(), 0.0));
    }
}
