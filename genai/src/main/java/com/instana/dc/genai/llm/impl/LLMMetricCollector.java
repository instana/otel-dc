package com.instana.dc.genai.llm.impl;

import com.instana.dc.RawMetric;
import com.instana.dc.genai.base.AbstractMetricCollector;
import com.instana.dc.genai.llm.metrics.LLMOtelMetric;
import com.instana.dc.genai.vectordb.metrics.VectordbOtelMetric;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static com.instana.dc.genai.llm.utils.LLMDcUtil.*;

public class LLMMetricCollector extends AbstractMetricCollector {
    private static final Logger logger = Logger.getLogger(LLMMetricCollector.class.getName());

    private final Map<String, ModelAggregation> modelAggrMap;
    private final Map<String, Map<String, ModelAggregation>> serviceModelAggrMap;
    private final Map<String, Double> llmTokenPrices;
    private final Map<String, RawMetric> rawMetricsMap;

    public LLMMetricCollector(Boolean otelAgentlessMode, Integer otelPollInterval, int listenPort, Map<String, RawMetric> rawMetricsMap) {
        super(otelAgentlessMode, otelPollInterval, listenPort);
        this.rawMetricsMap = rawMetricsMap;
        this.modelAggrMap = new ConcurrentHashMap<>();
        this.serviceModelAggrMap = new ConcurrentHashMap<>();
        this.llmTokenPrices = new ConcurrentHashMap<>();
        loadTokenPricesFromConfig();
    }

    private void loadTokenPricesFromConfig() {
        try (BufferedReader reader = new BufferedReader(new FileReader(LLM_PRICES_PROPERTIES))) {
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
                    setTokenPrices(value, key);
                }
            }
        } catch (Exception e) {
            logger.severe("Cannot load price properties file: " + e.getMessage());
        }
    }

    private void setTokenPrices(String value, String key) {
        try {
            Double dValue = Double.parseDouble(value);
            llmTokenPrices.put(key, dValue);
        } catch (Exception e) {
            llmTokenPrices.put(key, 0.0);
            logger.warning(value + " cannot be parsed to Double: " + e.getMessage());
        }
    }

    @Override
    protected void processLLMMetric(LLMOtelMetric metric) {
        updateModelAggregation(metric);
        updateServiceModelAggregation(metric);
    }

    @Override
    protected void processMilvusMetric(VectordbOtelMetric metric) {
        // LLM collector doesn't process Milvus metrics
    }



    private void updateModelAggregation(LLMOtelMetric metric) {
        synchronized (modelAggrMap) {
            ModelAggregation aggr = this.modelAggrMap.computeIfAbsent(metric.getModelId(), k -> new ModelAggregation(metric.getModelId(), metric.getAiSystem()));
            aggr.addDeltaInputTokens(metric.getDeltaInputTokens());
            aggr.addDeltaOutputTokens(metric.getDeltaOutputTokens());
            aggr.addDeltaDuration(metric.getDeltaDuration());
            aggr.addDeltaRequestCount(metric.getDeltaRequestCount());
        }
    }

    private void updateServiceModelAggregation(LLMOtelMetric metric) {
        synchronized (serviceModelAggrMap) {
            Map<String, ModelAggregation> modelAggregationMap = this.serviceModelAggrMap.computeIfAbsent(metric.getServiceName(), k -> new ConcurrentHashMap<>());
            synchronized (modelAggregationMap) {
                ModelAggregation aggr = modelAggregationMap.computeIfAbsent(metric.getModelId(), k -> new ModelAggregation(metric.getModelId(), metric.getAiSystem()));
                aggr.addDeltaInputTokens(metric.getDeltaInputTokens());
                aggr.addDeltaOutputTokens(metric.getDeltaOutputTokens());
                aggr.addDeltaDuration(metric.getDeltaDuration());
                aggr.addDeltaRequestCount(metric.getDeltaRequestCount());
            }
        }
    }

    @Override
    protected void processMetrics(int divisor) {
        logger.info("-----------------------------------------");
        processModelMetrics(divisor);
        logger.info("-----------------------------------------");
        processServiceMetrics(divisor);
        logger.info("-----------------------------------------");
    }

    private void processModelMetrics(int divisor) {
        synchronized (modelAggrMap) {
            this.modelAggrMap.forEach((modelId, aggr) -> {
                String aiSystem = aggr.getAiSystem();

                long deltaInputTokens = aggr.getDeltaInputTokens();
                long deltaOutputTokens = aggr.getDeltaOutputTokens();
                long deltaDuration = aggr.getDeltaDuration();
                long deltaRequestCount = aggr.getDeltaRequestCount();
                long maxDurationSoFar = aggr.getMaxDurationSoFar();

                aggr.resetDeltaValues();

                double intervalReqCount = (double) deltaRequestCount / divisor;
                double intervalInputTokens = (double) deltaInputTokens / divisor;
                double intervalOutputTokens = (double) deltaOutputTokens / divisor;
                double intervalTotalTokens = intervalInputTokens + intervalOutputTokens;

                double intervalInputCost = intervalInputTokens / 1000 * getTokenPrice(aiSystem, modelId, "input");
                double intervalOutputCost = intervalOutputTokens / 1000 * getTokenPrice(aiSystem, modelId, "output");
                double intervalTotalCost = intervalInputCost + intervalOutputCost;

                if (!isForceBackwardCompatible()) {
                    intervalTotalCost *= 10000;
                    intervalInputCost *= 10000;
                    intervalOutputCost *= 10000;
                }

                long avgDurationPerReq = deltaRequestCount == 0 ? 0 : deltaDuration / deltaRequestCount;
                if (avgDurationPerReq > maxDurationSoFar) {
                    maxDurationSoFar = avgDurationPerReq;
                    aggr.setMaxDurationSoFar(maxDurationSoFar);
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

                // Update raw metrics
                updateModelRawMetrics(modelId, aiSystem, avgDurationPerReq, maxDurationSoFar,
                        intervalTotalCost, intervalInputCost, intervalOutputCost,
                        intervalTotalTokens, intervalInputTokens, intervalOutputTokens,
                        intervalReqCount);
            });
        }
    }

    private void updateModelRawMetrics(String modelId, String aiSystem, long avgDuration, long maxDuration,
                                       double totalCost, double inputCost, double outputCost,
                                       double totalTokens, double inputTokens, double outputTokens,
                                       double reqCount) {
        Map<String, Object> attributes = new HashMap<>();
        String replacedId = modelId.replace(".", "/");
        String modelIdExt = aiSystem + ":" + replacedId;
        attributes.put("model_id", modelIdExt);
        attributes.put("ai_system", aiSystem);

        rawMetricsMap.get(LLM_STATUS_NAME).setValue(1);
        rawMetricsMap.get(LLM_DURATION_NAME).getDataPoint(modelIdExt).setValue(avgDuration, attributes);
        rawMetricsMap.get(LLM_DURATION_MAX_NAME).getDataPoint(modelIdExt).setValue(maxDuration, attributes);
        rawMetricsMap.get(LLM_COST_NAME).getDataPoint(modelIdExt).setValue(totalCost, attributes);
        rawMetricsMap.get(LLM_INPUT_COST_NAME).getDataPoint(modelIdExt).setValue(inputCost, attributes);
        rawMetricsMap.get(LLM_OUTPUT_COST_NAME).getDataPoint(modelIdExt).setValue(outputCost, attributes);
        rawMetricsMap.get(LLM_TOKEN_NAME).getDataPoint(modelIdExt).setValue(totalTokens, attributes);
        rawMetricsMap.get(LLM_INPUT_TOKEN_NAME).getDataPoint(modelIdExt).setValue(inputTokens, attributes);
        rawMetricsMap.get(LLM_OUTPUT_TOKEN_NAME).getDataPoint(modelIdExt).setValue(outputTokens, attributes);
        rawMetricsMap.get(LLM_REQ_COUNT_NAME).getDataPoint(modelIdExt).setValue(reqCount, attributes);
    }

    private void processServiceMetrics(int divisor) {
        synchronized (serviceModelAggrMap) {
            this.serviceModelAggrMap.forEach((serviceName, modelAggregationMap) -> {

                double serviceIntervalReqCount = 0.0;
                double serviceIntervalInputTokens = 0.0;
                double serviceIntervalOutputTokens = 0.0;
                double serviceIntervalInputCost = 0.0;
                double serviceIntervalOutputCost = 0.0;

                for (Map.Entry<String, ModelAggregation> entry : modelAggregationMap.entrySet()) {
                    ModelAggregation aggr = entry.getValue();
                    String modelId = aggr.getModelId();
                    String aiSystem = aggr.getAiSystem();

                    serviceIntervalReqCount += (double) aggr.getDeltaRequestCount() / divisor;

                    double intervalInputTokens = (double) aggr.getDeltaInputTokens() / divisor;
                    serviceIntervalInputTokens += intervalInputTokens;
                    serviceIntervalInputCost += intervalInputTokens / 1000 * getTokenPrice(aiSystem, modelId, "input");

                    double intervalOutputTokens = (double) aggr.getDeltaOutputTokens() / divisor;
                    serviceIntervalOutputTokens += intervalOutputTokens;
                    serviceIntervalOutputCost += intervalOutputTokens / 1000 * getTokenPrice(aiSystem, modelId, "output");

                    aggr.resetDeltaValues();
                }

                double serviceIntervalTotalTokens = serviceIntervalInputTokens + serviceIntervalOutputTokens;
                double serviceIntervalTotalCost = serviceIntervalInputCost + serviceIntervalOutputCost;

                if (!isForceBackwardCompatible()) {
                    serviceIntervalTotalCost *= 10000;
                    serviceIntervalInputCost *= 10000;
                    serviceIntervalOutputCost *= 10000;
                }

                System.out.printf("Metrics for service %s", serviceName);
                System.out.println(" - Interval Tokens  : " + serviceIntervalTotalTokens);
                System.out.println(" - Interval Input Tokens  : " + serviceIntervalInputTokens);
                System.out.println(" - Interval Output Tokens  : " + serviceIntervalOutputTokens);
                System.out.println(" - Interval Cost    : " + serviceIntervalTotalCost);
                System.out.println(" - Interval Input Cost    : " + serviceIntervalInputCost);
                System.out.println(" - Interval Output Cost    : " + serviceIntervalOutputCost);
                System.out.println(" - Interval Request : " + serviceIntervalReqCount);

                // Update raw metrics
                updateServiceRawMetrics(serviceName, serviceIntervalTotalCost, serviceIntervalInputCost,
                        serviceIntervalOutputCost, serviceIntervalTotalTokens,
                        serviceIntervalInputTokens, serviceIntervalOutputTokens,
                        serviceIntervalReqCount);
            });
        }
    }

    private void updateServiceRawMetrics(String serviceName, double totalCost, double inputCost,
                                         double outputCost, double totalTokens, double inputTokens,
                                         double outputTokens, double reqCount) {
        Map<String, Object> attributes = Map.of("service_name", serviceName);
        rawMetricsMap.get(LLM_SERVICE_COST_NAME).getDataPoint(serviceName).setValue(totalCost, attributes);
        rawMetricsMap.get(LLM_SERVICE_INPUT_COST_NAME).getDataPoint(serviceName).setValue(inputCost, attributes);
        rawMetricsMap.get(LLM_SERVICE_OUTPUT_COST_NAME).getDataPoint(serviceName).setValue(outputCost, attributes);
        rawMetricsMap.get(LLM_SERVICE_TOKEN_NAME).getDataPoint(serviceName).setValue(totalTokens, attributes);
        rawMetricsMap.get(LLM_SERVICE_INPUT_TOKEN_NAME).getDataPoint(serviceName).setValue(inputTokens, attributes);
        rawMetricsMap.get(LLM_SERVICE_OUTPUT_TOKEN_NAME).getDataPoint(serviceName).setValue(outputTokens, attributes);
        rawMetricsMap.get(LLM_SERVICE_REQ_COUNT_NAME).getDataPoint(serviceName).setValue(reqCount, attributes);
    }

    private static boolean isForceBackwardCompatible() {
        return "true".equalsIgnoreCase(System.getenv("FORCE_BACKWARD_COMPATIBLE"));
    }

    private Double getTokenPrice(String aiSystem, String modelId, String io) {
        return llmTokenPrices.getOrDefault(String.join(".", aiSystem, modelId, io).toLowerCase(),
                llmTokenPrices.getOrDefault((aiSystem + ".*." + io).toLowerCase(), 0.0));
    }

    @Override
    protected void collectMetrics() {
        try {
            this.resetAggregations();
            List<LLMOtelMetric> metrics = metricsCollectorService.getLLMDeltaMetrics();

            if (!metrics.isEmpty()) {
                metrics.forEach(this::processLLMMetric);
                this.processMetrics(otelPollInterval);
                metricsCollectorService.resetLLMMetrics();
            }
        } catch (Exception e) {
            logger.severe("Error collecting metrics: " + e.getMessage());
        }
    }

    private void resetAggregations() {
        synchronized (modelAggrMap) {
            this.modelAggrMap.values().forEach(ModelAggregation::resetDeltaValues);
        }
        synchronized (serviceModelAggrMap) {
            this.serviceModelAggrMap.values().forEach(modelAggregationMap ->
                modelAggregationMap.values().forEach(ModelAggregation::resetDeltaValues)
            );
        }
    }

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
}
