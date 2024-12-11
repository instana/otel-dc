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

    private HashMap<String, ModelAggregation> modelAggrMap = new HashMap<>();
    private MetricsCollectorService metricsCollector = new MetricsCollectorService();
    private Boolean otelAgentlessMode = Boolean.FALSE;
    private Integer callbackInterval = DEFAULT_LLM_CLBK_INTERVAL;
    private Integer otelPollInterval = DEFAULT_LLM_POLL_INTERVAL;
    private HashMap<String, Double> llmTokenPrices = new HashMap<>();
    private int listenPort = 0;

    /**
     * The poll rate in the configuration, in seconds. In other words, the number of
     * seconds between calls to Watsonx.
     */

    private class ModelAggregation { 
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
        callbackInterval = (Integer) properties.getOrDefault(CALLBACK_INTERVAL, DEFAULT_LLM_CLBK_INTERVAL);
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

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        logger.info("-----------------------------------------");
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
            int divisor = otelAgentlessMode? 1:otelPollInterval;

            String inputPriceKey = aiSystem + "." + modelId + ".input";
            Double priceInputTokens = llmTokenPrices.get(inputPriceKey.toLowerCase());
            if(priceInputTokens == null) {
                String inputFlatPriceKey = aiSystem + ".*.input";
                priceInputTokens = llmTokenPrices.get(inputFlatPriceKey.toLowerCase());
                if (priceInputTokens == null) {
                    priceInputTokens = 0.0;
                }
            }
            String outputPriceKey = aiSystem + "." + modelId + ".output";
            Double priceOutputTokens = llmTokenPrices.get(outputPriceKey.toLowerCase());
            if(priceOutputTokens == null) {
                String outputFlatPriceKey = aiSystem + ".*.output";
                priceOutputTokens = llmTokenPrices.get(outputFlatPriceKey.toLowerCase());
                if (priceOutputTokens == null) {
                    priceOutputTokens = 0.0;
                }
            }

            double intervalReqCount = (double)deltaRequestCount/divisor;
            double intervalInputTokens = (double)deltaInputTokens/divisor;
            double intervalOutputTokens = (double)deltaOutputTokens/divisor;
            double intervalTotalTokens = intervalInputTokens + intervalOutputTokens;

            double intervalInputCost = intervalInputTokens/1000 * priceInputTokens;
            double intervalOutputCost = intervalOutputTokens/1000 * priceOutputTokens;
            double intervalTotalCost = intervalInputCost + intervalOutputCost;

            // This costs are 10000 times the actual value to prevent very small numbers from being rounded off. 
            // And it will be adjusted to the correct value on UI.
            String backwardCompatible = System.getenv("FORCE_BACKWARD_COMPATIBLE");
            if (backwardCompatible != null && backwardCompatible.equalsIgnoreCase("true")) {
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
    }
}
