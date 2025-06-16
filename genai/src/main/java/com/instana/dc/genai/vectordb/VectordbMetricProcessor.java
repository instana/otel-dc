package com.instana.dc.genai.vectordb;

import com.instana.dc.genai.vectordb.metrics.VectordbOtelMetric;
import com.instana.dc.genai.vectordb.utils.VectordbDcUtil;
import io.opentelemetry.proto.metrics.v1.HistogramDataPoint;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VectordbMetricProcessor {
    private static final String DB_SYSTEM_KEY = "db.system";
    private static final String VECTORDB_SYSTEM = "vectordb";
    private static final Logger logger = Logger.getLogger(VectordbMetricProcessor.class.getName());

    private VectordbMetricProcessor() { }

    public static void processVectordbMetric(Metric metric, Map<String, VectordbOtelMetric> vectordbMetrics, String serviceName) {
        switch (metric.getDataCase()) {
            case HISTOGRAM:
                processVectordbHistogramMetric(metric, vectordbMetrics, serviceName);
                break;
            case SUM:
                processVectordbCounterMetric(metric, vectordbMetrics, serviceName);
                break;
            default:
                logger.log(Level.FINE, "Skipping VectorDB metric with DataCase: {0}", metric.getDataCase());
        }
    }

    private static void processVectordbHistogramMetric(Metric metric, Map<String, VectordbOtelMetric> vectordbMetrics, String serviceName) {
        for (HistogramDataPoint dataPoint : metric.getHistogram().getDataPointsList()) {
            String databaseSystem = extractDatabaseSystem(dataPoint);
            if (databaseSystem == null) return;

            String modelKey = String.format("%s:%s", serviceName, databaseSystem);
            VectordbOtelMetric vectordbOtelMetric = getOrCreateMetric(vectordbMetrics, modelKey, serviceName);

            processHistogramMetric(metric, dataPoint, vectordbOtelMetric);
        }
    }

    private static void processVectordbCounterMetric(Metric metric, Map<String, VectordbOtelMetric> vectordbMetrics, String serviceName) {
        for (NumberDataPoint dataPoint : metric.getSum().getDataPointsList()) {
            String databaseSystem = extractDatabaseSystem(dataPoint);
            if (databaseSystem == null) return;

            String modelKey = String.format("%s_%s", serviceName, databaseSystem);
            VectordbOtelMetric vectordbOtelMetric = getOrCreateMetric(vectordbMetrics, modelKey, serviceName);

            processCounterMetric(metric, dataPoint, vectordbOtelMetric);
        }
    }

    private static String extractDatabaseSystem(HistogramDataPoint dataPoint) {
        return dataPoint.getAttributesList().stream()
                .filter(attr -> DB_SYSTEM_KEY.equals(attr.getKey()))
                .map(attr -> attr.getValue().getStringValue())
                .findFirst()
                .orElse(null);
    }

    private static String extractDatabaseSystem(NumberDataPoint dataPoint) {
        return dataPoint.getAttributesList().stream()
                .filter(attr -> DB_SYSTEM_KEY.equals(attr.getKey()))
                .map(attr -> attr.getValue().getStringValue())
                .findFirst()
                .orElse(null);
    }

    private static VectordbOtelMetric getOrCreateMetric(Map<String, VectordbOtelMetric> vectordbMetrics, String modelKey, String serviceName) {
        return vectordbMetrics.computeIfAbsent(modelKey, k -> {
            VectordbOtelMetric metric = new VectordbOtelMetric();
            metric.setServiceName(serviceName);
            metric.setDbSystem(VECTORDB_SYSTEM);
            return metric;
        });
    }

    private static void processHistogramMetric(Metric metric, HistogramDataPoint dataPoint, VectordbOtelMetric vectordbOtelMetric) {
        long startTime = dataPoint.getStartTimeUnixNano();
        double sum = dataPoint.getSum();

        if (metric.getName().contains("duration")) {
            processDurationMetric(vectordbOtelMetric, startTime, sum);
        } else if (metric.getName().contains("distance")) {
            processDistanceMetric(vectordbOtelMetric, startTime, sum);
        }
    }

    private static void processDurationMetric(VectordbOtelMetric metric, long startTime, double sum) {
        long lastStartTime = metric.getLastDurationStartTime();
        double lastDurationSum = metric.getLastDurationSum();

        if (startTime != lastStartTime) {
            metric.setLastDurationStartTime(startTime);
            metric.addDeltaDuration(sum);
        } else {
            metric.addDeltaDuration(sum - lastDurationSum);
        }
        metric.setLastDurationSum(sum);
    }

    private static void processDistanceMetric(VectordbOtelMetric metric, long startTime, double sum) {
        String metricName = VectordbDcUtil.MILVUS_DB_SEARCH_DISTANCE_NAME;
        long lastStartTime = metric.getMetricStartTime(metricName);
        double lastSearchDistanceSum = metric.getMetricCount(metricName);

        if (startTime != lastStartTime) {
            metric.setMetricStartTime(metricName, startTime);
            metric.addMetricDelta(metricName, (long)sum);
        } else {
            metric.addMetricDelta(metricName, (long)(sum - lastSearchDistanceSum));
        }
        metric.setMetricCount(metricName, (long)sum);
    }

    private static void processCounterMetric(Metric metric, NumberDataPoint dataPoint, VectordbOtelMetric vectordbOtelMetric) {
        long startTime = dataPoint.getStartTimeUnixNano();
        long dataSum = dataPoint.getAsInt();
        String metricName = metric.getName();

        long lastStartTime = vectordbOtelMetric.getMetricStartTime(metricName);
        long lastCount = vectordbOtelMetric.getMetricCount(metricName);

        if (startTime != lastStartTime) {
            vectordbOtelMetric.setMetricStartTime(metricName, startTime);
            vectordbOtelMetric.addMetricDelta(metricName, dataSum);
        } else {
            vectordbOtelMetric.addMetricDelta(metricName, dataSum - lastCount);
        }
        vectordbOtelMetric.setMetricCount(metricName, dataSum);
    }
}
