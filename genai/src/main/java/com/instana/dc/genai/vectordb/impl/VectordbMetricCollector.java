package com.instana.dc.genai.vectordb.impl;

import com.instana.dc.RawMetric;
import com.instana.dc.genai.base.AbstractMetricCollector;
import com.instana.dc.genai.llm.metrics.LLMOtelMetric;
import com.instana.dc.genai.vectordb.metrics.VectordbOtelMetric;
import com.instana.dc.genai.vectordb.utils.VectordbDcUtil;
import com.instana.dc.genai.vectordb.metrics.MetricValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class VectordbMetricCollector extends AbstractMetricCollector {
    private final Map<String, VectordbAggregation> vectordbAggrMap;
    private final Map<String, RawMetric> rawMetricsMap;
    private static final Logger logger = Logger.getLogger(VectordbMetricCollector.class.getName());

    public VectordbMetricCollector(Boolean otelAgentlessMode, Integer otelPollInterval, int listenPort, Map<String, RawMetric> rawMetricsMap) {
        super(otelAgentlessMode, otelPollInterval, listenPort);
        this.rawMetricsMap = rawMetricsMap;
        this.vectordbAggrMap = new HashMap<>();
    }

    @Override
    protected void processLLMMetric(LLMOtelMetric metric) {
        // VectorDB collector doesn't process LLM metrics
    }

    @Override
    protected void processVectordbMetric(VectordbOtelMetric metric) {
        updateVectordbAggregation(metric);
    }

    private void updateVectordbAggregation(VectordbOtelMetric metric) {
        VectordbAggregation aggr = vectordbAggrMap.computeIfAbsent(
                metric.getDbSystem(),
                k -> new VectordbAggregation(k, metric.getServiceName())
        );

        aggr.addDeltaDuration(metric.getDeltaDuration());

        for (Map.Entry<String, MetricValue> entry : metric.getMetrics().entrySet()) {
            String metricName = entry.getKey();
            if (!metricName.equals(VectordbDcUtil.MILVUS_DB_QUERY_DURATION_NAME)) {  // Skip duration as it's handled separately
                aggr.addMetricDelta(metricName, entry.getValue().getDelta());
            }
        }
    }

    @Override
    protected void processMetrics(int divisor) {
        vectordbAggrMap.values().forEach(aggr -> {
            String dbSystem = aggr.getDbSystem();
            String serviceName = aggr.getServiceName();
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("db_system", dbSystem);

            double duration = (double) aggr.getDeltaDuration() / divisor;
            logger.info(String.format("Metrics for DB system %s of %s:", dbSystem, serviceName));
            logger.info(String.format(" - %s : %.2f ms", VectordbDcUtil.MILVUS_DB_QUERY_DURATION_NAME, duration));
            updateMetric(VectordbDcUtil.MILVUS_DB_QUERY_DURATION_NAME, duration, attributes, dbSystem);

            for (Map.Entry<String, Long> entry : aggr.getMetricDeltas().entrySet()) {
                String metricName = entry.getKey();
                long delta = entry.getValue();
                double value = (double) delta / divisor;
                
                if (rawMetricsMap.containsKey(metricName)) {
                    updateMetric(metricName, value, attributes, dbSystem);
                    logger.info(String.format(" - %s : %.2f", metricName, value));
                }
            }
        });
    }

    private void updateMetric(String metricName, double value, Map<String, Object> attributes, String dbSystem) {
        getRawMetric(metricName).getDataPoint(dbSystem).setValue(value, attributes);
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
                metricsCollectorService.resetDeltaMetrics();
            }
        } catch (Exception e) {
            logger.severe("Error collecting metrics: " + e.getMessage());
        }
    }

    private void resetAggregations() {
        vectordbAggrMap.values().forEach(VectordbAggregation::resetDeltaValues);
    }

    private static class VectordbAggregation {
        private final String serviceName;
        private final String dbSystem;
        private long deltaDuration;
        private final Map<String, Long> metricDeltas;

        public VectordbAggregation(String dbSystem, String serviceName) {
            this.dbSystem = dbSystem;
            this.serviceName = serviceName;
            this.metricDeltas = new HashMap<>();
        }

        public void resetDeltaValues() {
            this.deltaDuration = 0;
            metricDeltas.clear();
        }

        public String getDbSystem() {
            return dbSystem;
        }

        public String getServiceName() {
            return serviceName;
        }

        public long getDeltaDuration() {
            return deltaDuration;
        }

        public Map<String, Long> getMetricDeltas() {
            return metricDeltas;
        }

        public void addDeltaDuration(long duration) {
            deltaDuration += duration;
        }

        public void addMetricDelta(String name, long delta) {
            metricDeltas.merge(name, delta, Long::sum);
        }
    }
} 