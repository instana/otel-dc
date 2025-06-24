package com.instana.dc.genai.vectordb.impl;

import com.instana.dc.RawMetric;
import com.instana.dc.genai.base.AbstractMetricCollector;
import com.instana.dc.genai.llm.metrics.LLMOtelMetric;
import com.instana.dc.genai.vectordb.metrics.MetricValue;
import com.instana.dc.genai.vectordb.metrics.VectordbOtelMetric;
import com.instana.dc.genai.vectordb.utils.VectordbDcUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.instana.dc.genai.vectordb.utils.VectordbDcUtil.VECTORDB_STATUS_NAME;

public class VectordbMetricCollector extends AbstractMetricCollector {
    private final Map<String, VectordbAggregation> serviceAggrMap;
    private final Map<String, RawMetric> rawMetricsMap;
    private static final Logger logger = Logger.getLogger(VectordbMetricCollector.class.getName());

    public VectordbMetricCollector(Boolean otelAgentlessMode, Integer otelPollInterval, int listenPort, Map<String, RawMetric> rawMetricsMap) {
        super(otelAgentlessMode, otelPollInterval, listenPort);
        this.rawMetricsMap = rawMetricsMap;
        this.serviceAggrMap = new HashMap<>();
    }

    @Override
    protected void processLLMMetric(LLMOtelMetric metric) {
        // VectorDB collector doesn't process LLM metrics
    }

    @Override
    protected void processVectordbMetric(VectordbOtelMetric metric) {
        updateServiceAggregation(metric);
    }

    private void updateServiceAggregation(VectordbOtelMetric metric) {
        VectordbAggregation aggr = serviceAggrMap.computeIfAbsent(metric.getServiceName(), k -> new VectordbAggregation(metric.getDbSystem()));
        aggr.addDeltaDuration(metric.getDeltaDuration());
        for (Map.Entry<String, MetricValue> entry : metric.getMetrics().entrySet()) {
            String metricName = entry.getKey();
            aggr.addMetricDelta(metricName, entry.getValue().getDelta());
        }
    }

    @Override
    protected void processMetrics(int divisor) {
        logger.info("-----------------------------------------");
        serviceAggrMap.forEach((serviceName, aggr) -> processAggregationMetrics(aggr, serviceName, divisor));
        logger.info("-----------------------------------------");
    }

    private void processAggregationMetrics(VectordbAggregation aggr, String serviceName, int divisor) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("service_name", serviceName);
        attributes.put("db_system", aggr.getDbSystem());
        System.out.println("Metrics of " + aggr.getDbSystem() + " for service " + serviceName + ":");
        double duration = (double) aggr.getDeltaDuration() / divisor;
        String durationMetricName = VectordbDcUtil.MILVUS_DB_QUERY_DURATION_NAME;
        if (aggr.getDeltaDuration() != 0) {
            System.out.println(" - Duration : " + duration + " ms (" + durationMetricName + ")");
        }
        getRawMetric(VECTORDB_STATUS_NAME).setValue(1);
        Map<String, Double> nonZeroMetrics = printAndCollectNonZeroMetrics(aggr, divisor, rawMetricsMap);
        if (aggr.getDeltaDuration() != 0) {
            updateMetric(durationMetricName, duration, attributes, serviceName);
        }
        for (Map.Entry<String, Double> entry : nonZeroMetrics.entrySet()) {
            updateMetric(entry.getKey(), entry.getValue(), attributes, serviceName);
        }
    }

    private void updateMetric(String metricName, double value, Map<String, Object> attributes, String serviceName) {
        getRawMetric(metricName).getDataPoint(serviceName).setValue(value, attributes);
    }

    private RawMetric getRawMetric(String name) {
        return rawMetricsMap.get(name);
    }

    @Override
    protected void collectMetrics() {
        try {
            resetAggregations();
            List<VectordbOtelMetric> metrics = metricsCollectorService.getVectordbDeltaMetrics();
            if (!metrics.isEmpty()) {
                metrics.forEach(this::processVectordbMetric);
                processMetrics(otelPollInterval);
                metricsCollectorService.resetVectordbMetrics();
            }
        } catch (Exception e) {
            logger.severe("Error collecting metrics: " + e.getMessage());
        }
    }

    private void resetAggregations() {
        serviceAggrMap.values().forEach(VectordbAggregation::resetDeltaValues);
    }

    private Map<String, Double> printAndCollectNonZeroMetrics(
            VectordbAggregation aggr, int divisor, Map<String, RawMetric> rawMetricsMap
    ) {
        Map<String, Double> nonZeroMetrics = new HashMap<>();
        for (Map.Entry<String, Double> entry : aggr.getMetricDeltas().entrySet()) {
            String metricName = entry.getKey();
            double delta = entry.getValue();
            double value = delta / divisor;
            if (rawMetricsMap.containsKey(metricName) && delta != 0) {
                System.out.printf(" - %s : %.2f%n", metricName, value);
                nonZeroMetrics.put(metricName, value);
            }
        }
        return nonZeroMetrics;
    }

    private static class VectordbAggregation {
        private final String dbSystem;
        private long deltaDuration;
        private final Map<String, Double> metricDeltas;

        public VectordbAggregation(String dbSystem) {
            this.dbSystem = dbSystem;
            this.metricDeltas = new HashMap<>();
        }

        public void resetDeltaValues() {
            this.deltaDuration = 0;
            metricDeltas.clear();
        }

        public String getDbSystem() {
            return dbSystem;
        }

        public long getDeltaDuration() {
            return deltaDuration;
        }

        public Map<String, Double> getMetricDeltas() {
            return metricDeltas;
        }

        public void addDeltaDuration(long duration) {
            deltaDuration += duration;
        }

        public void addMetricDelta(String name, double delta) {
            metricDeltas.merge(name, delta, Double::sum);
        }
    }
}
