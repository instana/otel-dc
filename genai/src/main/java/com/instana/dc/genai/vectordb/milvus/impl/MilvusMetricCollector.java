package com.instana.dc.genai.vectordb.milvus.impl;

import com.instana.dc.RawMetric;
import com.instana.dc.genai.base.AbstractMetricCollector;
import com.instana.dc.genai.llm.metrics.LLMOtelMetric;
import com.instana.dc.genai.vectordb.milvus.utils.MilvusDcUtil;
import com.instana.dc.genai.vectordb.metrics.MetricValue;
import com.instana.dc.genai.vectordb.metrics.VectordbOtelMetric;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;

import static com.instana.dc.genai.vectordb.milvus.utils.MilvusDcUtil.MILVUS_STATUS_NAME;

public class MilvusMetricCollector extends AbstractMetricCollector {
    private final Map<String, MilvusAggregation> serviceAggrMap;
    private final Map<String, RawMetric> rawMetricsMap;
    private static final Logger logger = Logger.getLogger(MilvusMetricCollector.class.getName());

    public MilvusMetricCollector(Boolean otelAgentlessMode, Integer otelPollInterval, int listenPort, Map<String, RawMetric> rawMetricsMap) {
        super(otelAgentlessMode, otelPollInterval, listenPort);
        this.rawMetricsMap = rawMetricsMap;
        this.serviceAggrMap = new ConcurrentHashMap<>();
    }

    @Override
    protected void processLLMMetric(LLMOtelMetric metric) {
        // Milvus collector doesn't process LLM metrics
    }

    @Override
    protected void processMilvusMetric(VectordbOtelMetric metric) {
        updateServiceAggregation(metric);
    }

    private void updateServiceAggregation(VectordbOtelMetric metric) {
        synchronized (serviceAggrMap) {
            MilvusAggregation aggr = this.serviceAggrMap.computeIfAbsent(metric.getServiceName(), k -> new MilvusAggregation(metric.getDbSystem()));
            aggr.addDeltaDuration(metric.getDeltaDuration());
            aggr.addDurationCount(metric.getDurationCount());
            for (Map.Entry<String, MetricValue> entry : metric.getMetrics().entrySet()) {
                String metricName = entry.getKey();
                MetricValue metricValue = entry.getValue();
                aggr.addMetricDelta(metricName, metricValue.getDelta(), metricValue.getCount());
            }
        }
    }

    @Override
    protected void processMetrics(int divisor) {
        logger.info("-----------------------------------------");
        synchronized (serviceAggrMap) {
            this.serviceAggrMap.forEach((serviceName, aggr) -> processAggregationMetrics(aggr, serviceName, divisor));
        }
        logger.info("-----------------------------------------");
    }

    private void processAggregationMetrics(MilvusAggregation aggr, String serviceName, int divisor) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("service_name", serviceName);
        attributes.put("db_system", aggr.getDbSystem());
        System.out.println("Metrics of " + aggr.getDbSystem() + " for service " + serviceName + ":");
        
        double duration = (double) aggr.getDeltaDuration() / aggr.getDurationCount();
        String durationMetricName = MilvusDcUtil.MILVUS_DB_QUERY_DURATION_NAME;
        System.out.println(" - Duration : " + duration + " ms (" + durationMetricName + ")");
        getRawMetric(MILVUS_STATUS_NAME).setValue(1);
        
        Map<String, Double> metricsToProcess = collectMetricsToProcess(aggr, divisor, rawMetricsMap);

        updateMetric(durationMetricName, duration, attributes, serviceName);
        for (Map.Entry<String, Double> entry : metricsToProcess.entrySet()) {
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
            this.resetAggregations();
            List<VectordbOtelMetric> metrics = metricsCollectorService.getMilvusDeltaMetrics();
            if (!metrics.isEmpty()) {
                metrics.forEach(this::processMilvusMetric);
                this.processMetrics(otelPollInterval);
                metricsCollectorService.resetMilvusMetrics();
            }
        } catch (Exception e) {
            logger.severe("Error collecting metrics: " + e.getMessage());
        }
    }

    private void resetAggregations() {
        synchronized (serviceAggrMap) {
            this.serviceAggrMap.values().forEach(MilvusAggregation::resetDeltaValues);
        }
    }

    private Map<String, Double> collectMetricsToProcess(
            MilvusAggregation aggr, int divisor, Map<String, RawMetric> rawMetricsMap
    ) {
        Map<String, Double> metricsToProcess = new HashMap<>();
        for (Map.Entry<String, MetricAggregation> entry : aggr.getMetricAggregations().entrySet()) {
            String metricName = entry.getKey();
            MetricAggregation metricAggr = entry.getValue();
            
            // Calculate average for metrics that have counts, otherwise use delta
            double value;
            if (metricAggr.getCount() > 0) {
                value = metricAggr.getDelta() / metricAggr.getCount(); // Average
            } else {
                value = metricAggr.getDelta() / divisor;
            }
            if (rawMetricsMap.containsKey(metricName)) {
                System.out.printf(" - %s : %f%n", metricName, value);
                metricsToProcess.put(metricName, value);
            }
        }
        return metricsToProcess;
    }

    private static class MetricAggregation {
        private double delta;
        private long count;

        public MetricAggregation() {
            this.delta = 0;
            this.count = 0;
        }

        public double getDelta() {
            return delta;
        }

        public long getCount() {
            return count;
        }

        public void addDelta(double delta) {
            this.delta += delta;
        }

        public void addCount(long count) {
            this.count += count;
        }

        public void reset() {
            this.delta = 0;
            this.count = 0;
        }
    }

    private static class MilvusAggregation {
        private final String dbSystem;
        private long deltaDuration;
        private long durationCount;
        private final Map<String, MetricAggregation> metricAggregations;

        public MilvusAggregation(String dbSystem) {
            this.dbSystem = dbSystem;
            this.metricAggregations = new HashMap<>();
        }

        public void resetDeltaValues() {
            this.deltaDuration = 0;
            this.durationCount = 0;
            metricAggregations.values().forEach(MetricAggregation::reset);
        }

        public String getDbSystem() {
            return dbSystem;
        }

        public long getDeltaDuration() {
            return deltaDuration;
        }

        public long getDurationCount() {
            return durationCount;
        }

        public Map<String, MetricAggregation> getMetricAggregations() {
            return metricAggregations;
        }

        public void addDeltaDuration(long duration) {
            deltaDuration += duration;
        }

        public void addDurationCount(long count) {
            durationCount += count;
        }

        public void addMetricDelta(String name, double delta, long count) {
            MetricAggregation agg = metricAggregations.computeIfAbsent(name, k -> new MetricAggregation());
            agg.addDelta(delta);
            agg.addCount(count);
        }
    }
} 