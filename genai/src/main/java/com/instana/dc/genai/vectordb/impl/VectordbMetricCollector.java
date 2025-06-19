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

public class VectordbMetricCollector extends AbstractMetricCollector {
    private final Map<String, Map<String, VectordbAggregation>> serviceDbSystemAggrMap;  // service -> (dbSystem -> aggregation)
    private final Map<String, VectordbAggregation> dbSystemAggrMap;         // dbSystem -> aggregation
    private final Map<String, RawMetric> rawMetricsMap;
    private static final Logger logger = Logger.getLogger(VectordbMetricCollector.class.getName());

    private static final Map<String, String> METRIC_NAME_MAPPING = Map.of(
            VectordbDcUtil.MILVUS_DB_INSERT_UNITS_NAME, VectordbDcUtil.MILVUS_DB_SERVICE_INSERT_UNITS_NAME,
            VectordbDcUtil.MILVUS_DB_UPSERT_UNITS_NAME, VectordbDcUtil.MILVUS_DB_SERVICE_UPSERT_UNITS_NAME,
            VectordbDcUtil.MILVUS_DB_DELETE_UNITS_NAME, VectordbDcUtil.MILVUS_DB_SERVICE_DELETE_UNITS_NAME,
            VectordbDcUtil.MILVUS_DB_SEARCH_DISTANCE_NAME, VectordbDcUtil.MILVUS_DB_SERVICE_SEARCH_DISTANCE_NAME
    );

    public VectordbMetricCollector(Boolean otelAgentlessMode, Integer otelPollInterval, int listenPort, Map<String, RawMetric> rawMetricsMap) {
        super(otelAgentlessMode, otelPollInterval, listenPort);
        this.rawMetricsMap = rawMetricsMap;
        this.serviceDbSystemAggrMap = new HashMap<>();
        this.dbSystemAggrMap = new HashMap<>();
    }

    @Override
    protected void processLLMMetric(LLMOtelMetric metric) {
        // VectorDB collector doesn't process LLM metrics
    }

    @Override
    protected void processVectordbMetric(VectordbOtelMetric metric) {
        updateServiceDbSystemAggregation(metric);
        updateDbSystemAggregation(metric);
    }

    private void updateServiceDbSystemAggregation(VectordbOtelMetric metric) {
        Map<String, VectordbAggregation> serviceAggrMap = serviceDbSystemAggrMap.computeIfAbsent(
                metric.getServiceName(),
                k -> new HashMap<>()
        );

        VectordbAggregation aggr = serviceAggrMap.computeIfAbsent(
                metric.getDbSystem(),
                k -> new VectordbAggregation(k, metric.getServiceName())
        );

        aggr.addDeltaDuration(metric.getDeltaDuration());

        for (Map.Entry<String, MetricValue> entry : metric.getMetrics().entrySet()) {
            String metricName = entry.getKey();
            String serviceMetricName = METRIC_NAME_MAPPING.getOrDefault(metricName, metricName);
            aggr.addMetricDelta(serviceMetricName, entry.getValue().getDelta());
        }
    }

    private void updateDbSystemAggregation(VectordbOtelMetric metric) {
        VectordbAggregation aggr = dbSystemAggrMap.computeIfAbsent(
                metric.getDbSystem(),
                k -> new VectordbAggregation(k, metric.getServiceName())
        );

        aggr.addDeltaDuration(metric.getDeltaDuration());

        for (Map.Entry<String, MetricValue> entry : metric.getMetrics().entrySet()) {
            String metricName = entry.getKey();
            if (!metricName.equals(VectordbDcUtil.MILVUS_DB_QUERY_DURATION_NAME)) {
                aggr.addMetricDelta(metricName, entry.getValue().getDelta());
            }
        }
    }

    @Override
    protected void processMetrics(int divisor) {
        logger.info("-----------------------------------------");
        // Process service+DB system level metrics
        serviceDbSystemAggrMap.forEach((serviceName, dbSystemMap) ->
                dbSystemMap.forEach((dbSystem, aggr) -> processAggregationMetrics(aggr, serviceName, dbSystem, divisor, true)));
        logger.info("-----------------------------------------");
        // Process DB system level metrics
        dbSystemAggrMap.values().forEach(aggr -> processAggregationMetrics(aggr, aggr.getServiceName(), aggr.getDbSystem(), divisor, false));
        logger.info("-----------------------------------------");
    }

    private void processAggregationMetrics(VectordbAggregation aggr, String serviceName, String dbSystem, int divisor, boolean isServiceLevel) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("db_system", dbSystem);
        attributes.put("service", serviceName);

        double duration = (double) aggr.getDeltaDuration() / divisor;
        String durationMetricName = isServiceLevel ? VectordbDcUtil.MILVUS_DB_SERVICE_QUERY_DURATION_NAME : VectordbDcUtil.MILVUS_DB_QUERY_DURATION_NAME;

        System.out.printf("Metrics for dbSystem %s of service %s:%n", dbSystem, serviceName);
        if (aggr.getDeltaDuration() != 0) {
            System.out.println(" - Duration : " + duration + " ms (" + durationMetricName + ")");
        }
        Map<String, Double> nonZeroMetrics = printAndCollectNonZeroMetrics(aggr, divisor, rawMetricsMap);

        if (aggr.getDeltaDuration() != 0) {
            updateMetric(durationMetricName, duration, attributes, dbSystem);
        }
        for (Map.Entry<String, Double> entry : nonZeroMetrics.entrySet()) {
            updateMetric(entry.getKey(), entry.getValue(), attributes, dbSystem);
        }
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
        serviceDbSystemAggrMap.values().forEach(dbSystemMap ->
                dbSystemMap.values().forEach(VectordbAggregation::resetDeltaValues)
        );
        dbSystemAggrMap.values().forEach(VectordbAggregation::resetDeltaValues);
    }

    private Map<String, Double> printAndCollectNonZeroMetrics(
    VectordbAggregation aggr, int divisor, Map<String, RawMetric> rawMetricsMap
) {
    Map<String, Double> nonZeroMetrics = new HashMap<>();
    for (Map.Entry<String, Long> entry : aggr.getMetricDeltas().entrySet()) {
        String metricName = entry.getKey();
        long delta = entry.getValue();
        double value = (double) delta / divisor;
        if (rawMetricsMap.containsKey(metricName) && delta != 0) {
            System.out.println(String.format(" - %s : %.2f", metricName, value));
            nonZeroMetrics.put(metricName, value);
        }
    }
    return nonZeroMetrics;
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
