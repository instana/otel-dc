package com.instana.dc.genai.vectordb.impl;

import com.instana.dc.RawMetric;
import com.instana.dc.genai.base.AbstractMetricCollector;
import com.instana.dc.genai.llm.metrics.LLMOtelMetric;
import com.instana.dc.genai.vectordb.metrics.VectordbOtelMetric;
import com.instana.dc.genai.metrics.OtelMetric;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.instana.dc.genai.vectordb.utils.VectordbDcUtil.*;

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
        aggr.addDeltaSearchDistance(metric.getDeltaSearchDistance());
        aggr.addDeltaInsertCount(metric.getDeltaInsertCount());
        aggr.addDeltaUpsertCount(metric.getDeltaUpsertCount());
        aggr.addDeltaDeleteCount(metric.getDeltaDeleteCount());
    }

    @Override
    protected void processMetrics(int divisor) {
        vectordbAggrMap.values().forEach(aggr -> {
            String dbSystem = aggr.getDbSystem();
            String serviceName = aggr.getServiceName();

            double duration = (double) aggr.getDeltaDuration() / divisor;
            double searchDistance = (double) aggr.getDeltaSearchDistance() / divisor;
            double insertCount = (double) aggr.getDeltaInsertCount() / divisor;
            double upsertCount = (double) aggr.getDeltaUpsertCount() / divisor;
            double deleteCount = (double) aggr.getDeltaDeleteCount() / divisor;

            // Update metrics
            updateMetric(MILVUS_DB_QUERY_DURATION_NAME, aggr.getDeltaDuration(), divisor);
            updateMetric(MILVUS_DB_SEARCH_DISTANCE_NAME, aggr.getDeltaSearchDistance(), divisor);
            updateMetric(MILVUS_DB_INSERT_UNITS_NAME, aggr.getDeltaInsertCount(), divisor);
            updateMetric(MILVUS_DB_UPSERT_UNITS_NAME, aggr.getDeltaUpsertCount(), divisor);
            updateMetric(MILVUS_DB_DELETE_UNITS_NAME, aggr.getDeltaDeleteCount(), divisor);

            logger.info(String.format("Metrics for DB system %s of %s:", dbSystem, serviceName));
            logger.info(String.format(" - Duration : %.2f ms", duration));
            logger.info(String.format(" - Search distance : %.2f", searchDistance));
            logger.info(String.format(" - Insert Count  : %.2f", insertCount));
            logger.info(String.format(" - Upsert Count  : %.2f", upsertCount));
            logger.info(String.format(" - Delete Count  : %.2f", deleteCount));
        });
    }

    private void updateMetric(String metricName, long value, int divisor) {
        Optional.ofNullable(getRawMetric(metricName))
            .ifPresent(metric -> metric.setValue((double) value / divisor));
    }


    private RawMetric getRawMetric(String name) {
        return rawMetricsMap.get(name);
    }

    @Override
    protected void collectMetrics() {
        try {
            resetAggregations();
            List<OtelMetric> metrics = metricsCollectorService.getDeltaMetrics();
            
            List<VectordbOtelMetric> vectordbMetrics = metrics.stream()
                .filter(VectordbOtelMetric.class::isInstance)
                .map(VectordbOtelMetric.class::cast)
                .collect(Collectors.toList());
                
            if (!vectordbMetrics.isEmpty()) {
                vectordbMetrics.forEach(this::processVectordbMetric);
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
        private long deltaInsertCount;
        private long deltaUpsertCount;
        private long deltaDeleteCount;
        private long deltaSearchDistance;

        public VectordbAggregation(String dbSystem, String serviceName) {
            this.dbSystem = dbSystem;
            this.serviceName = serviceName;
        }

        public void resetDeltaValues() {
            this.deltaDuration = 0;
            this.deltaInsertCount = 0;
            this.deltaUpsertCount = 0;
            this.deltaDeleteCount = 0;
            this.deltaSearchDistance = 0;
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

        public long getDeltaInsertCount() {
            return deltaInsertCount;
        }

        public long getDeltaUpsertCount() {
            return deltaUpsertCount;
        }

        public long getDeltaDeleteCount() {
            return deltaDeleteCount;
        }

        public long getDeltaSearchDistance() {
            return deltaSearchDistance;
        }

        public void addDeltaDuration(long duration) {
            deltaDuration += duration;
        }

        public void addDeltaSearchDistance(long searchDistance) {
            deltaSearchDistance += searchDistance;
        }

        public void addDeltaInsertCount(long insertCount) {
            deltaInsertCount += insertCount;
        }

        public void addDeltaUpsertCount(long upsertCount) {
            deltaUpsertCount += upsertCount;
        }

        public void addDeltaDeleteCount(long deleteCount) {
            deltaDeleteCount += deleteCount;
        }
    }
} 