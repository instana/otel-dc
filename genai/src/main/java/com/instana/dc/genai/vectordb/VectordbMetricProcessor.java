package com.instana.dc.genai.vectordb;

import com.instana.dc.genai.vectordb.metrics.VectordbOtelMetric;
import io.opentelemetry.proto.metrics.v1.HistogramDataPoint;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;

import java.util.Map;

public class VectordbMetricProcessor {
    private static final String DB_SYSTEM_KEY = "db.system";
    private static final String VECTORDB_SYSTEM = "vectordb";

    private VectordbMetricProcessor() { }

    public static void processVectordbHistogramMetric(Metric metric, Map<String, VectordbOtelMetric> vectordbMetrics, String serviceName) {
        for (HistogramDataPoint dataPoint : metric.getHistogram().getDataPointsList()) {
            String databaseSystem = extractDatabaseSystem(dataPoint);
            if (databaseSystem == null) return;

            String modelKey = String.format("%s_%s", serviceName, databaseSystem);
            VectordbOtelMetric vectordbOtelMetric = getOrCreateMetric(vectordbMetrics, modelKey, serviceName);

            processHistogramMetric(metric, dataPoint, vectordbOtelMetric);
        }
    }

    public static void processVectordbCounterMetric(Metric metric, Map<String, VectordbOtelMetric> vectordbMetrics, String serviceName) {
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
        long lastStartTime = metric.getLastSearchDistanceStartTime();
        double lastSearchDistanceSum = metric.getLastSearchDistance();

        if (startTime != lastStartTime) {
            metric.setLastSearchDistanceStartTime(startTime);
            metric.addDeltaSearchDistance(sum);
        } else {
            metric.addDeltaSearchDistance(sum - lastSearchDistanceSum);
        }
        metric.setLastSearchDistance(sum);
    }

    private static void processCounterMetric(Metric metric, NumberDataPoint dataPoint, VectordbOtelMetric vectordbOtelMetric) {
        long startTime = dataPoint.getStartTimeUnixNano();
        long dataSum = dataPoint.getAsInt();

        if (metric.getName().contains("insert_units")) {
            processInsertMetric(vectordbOtelMetric, startTime, dataSum);
        } else if (metric.getName().contains("upsert_units")) {
            processUpsertMetric(vectordbOtelMetric, startTime, dataSum);
        } else if (metric.getName().contains("delete_units")) {
            processDeleteMetric(vectordbOtelMetric, startTime, dataSum);
        }
    }

    private static void processInsertMetric(VectordbOtelMetric metric, long startTime, long dataSum) {
        long lastStartTime = metric.getLastInsertCountStartTime();
        long lastInsertCount = metric.getLastInsertCount();

        if (startTime != lastStartTime) {
            metric.setLastInsertCountStartTime(startTime);
            metric.addDeltaInsertCount(dataSum);
        } else {
            metric.addDeltaInsertCount(dataSum - lastInsertCount);
        }
        metric.setLastInsertCount(dataSum);
    }

    private static void processUpsertMetric(VectordbOtelMetric metric, long startTime, long dataSum) {
        long lastStartTime = metric.getLastUpsertCountStartTime();
        long lastUpsertCount = metric.getLastUpsertCount();

        if (startTime != lastStartTime) {
            metric.setLastUpsertCountStartTime(startTime);
            metric.addDeltaUpsertCount(dataSum);
        } else {
            metric.addDeltaUpsertCount(dataSum - lastUpsertCount);
        }
        metric.setLastUpsertCount(dataSum);
    }

    private static void processDeleteMetric(VectordbOtelMetric metric, long startTime, long dataSum) {
        long lastStartTime = metric.getLastDeleteCountStartTime();
        long lastDeleteCount = metric.getLastDeleteCount();

        if (startTime != lastStartTime) {
            metric.setLastDeleteCountStartTime(startTime);
            metric.addDeltaDeleteCount(dataSum);
        } else {
            metric.addDeltaDeleteCount(dataSum - lastDeleteCount);
        }
        metric.setLastDeleteCount(dataSum);
    }
} 