package com.instana.dc.genai.vectordb;

import com.instana.dc.genai.vectordb.metrics.VectordbOtelMetric;
import io.opentelemetry.proto.metrics.v1.HistogramDataPoint;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;

public class VectordbMetricProcessor {
    private static final String DB_SYSTEM_KEY = "db.system";
    private static final Logger logger = Logger.getLogger(VectordbMetricProcessor.class.getName());

    // Generic mapping from OTel names to DB-specific names
    private static final Map<String, String> GENERIC_TO_DB_METRIC_MAP = new HashMap<>();
    static {
        GENERIC_TO_DB_METRIC_MAP.put("db.client.query.duration", "db.{db_system}.query.duration");
        GENERIC_TO_DB_METRIC_MAP.put("db.client.search.distance", "db.{db_system}.search.distance");
        GENERIC_TO_DB_METRIC_MAP.put("db.client.usage.insert_units", "db.{db_system}.usage.insert_units");
        GENERIC_TO_DB_METRIC_MAP.put("db.client.usage.upsert_units", "db.{db_system}.usage.upsert_units");
        GENERIC_TO_DB_METRIC_MAP.put("db.client.usage.delete_units", "db.{db_system}.usage.delete_units");
    }

    private VectordbMetricProcessor() { }

    /**
     * Maps generic OpenTelemetry metric names to DB-specific names
     * @param genericMetricName The generic metric name (e.g., "vectordb.query.duration")
     * @param dbSystem The database system (e.g., "milvus", "pinecone", etc.)
     * @return The DB-specific metric name (e.g., "db.milvus.query.duration")
     */
    private static String mapToDbSpecificMetricName(String genericMetricName, String dbSystem) {
        String template = GENERIC_TO_DB_METRIC_MAP.get(genericMetricName);
        if (template != null) {
            return template.replace("{db_system}", dbSystem);
        }
        return genericMetricName; // Return original if no mapping found
    }

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

            System.out.println("Recv Metric --- DB System: " + databaseSystem);

            String key = String.format("%s:%s", serviceName, databaseSystem);
            VectordbOtelMetric vectordbOtelMetric = getOrCreateMetric(vectordbMetrics, key, serviceName, databaseSystem);

            long startTime = dataPoint.getStartTimeUnixNano();
            double sum = dataPoint.getSum();
            String metricName = metric.getName();
            
            String mappedMetricName = mapToDbSpecificMetricName(metricName, databaseSystem);
            
            if (mappedMetricName.contains("duration")) {
                processDurationMetric(vectordbOtelMetric, startTime, sum);
            } else if (mappedMetricName.contains("distance")) {
                processDistanceMetric(vectordbOtelMetric, startTime, sum, mappedMetricName);
            }
        }
    }

    private static void processVectordbCounterMetric(Metric metric, Map<String, VectordbOtelMetric> vectordbMetrics, String serviceName) {
        for (NumberDataPoint dataPoint : metric.getSum().getDataPointsList()) {
            String databaseSystem = extractDatabaseSystem(dataPoint);
            if (databaseSystem == null) return;

            System.out.println("Recv Metric --- DB System: " + databaseSystem);
            System.out.println("Recv Metric --- Counter Value: " + dataPoint.getAsInt());
            System.out.println("Recv Metric --- Start Time : " + dataPoint.getStartTimeUnixNano());
            System.out.println("Recv Metric --- End Time : " + dataPoint.getTimeUnixNano());

            String key = String.format("%s_%s", serviceName, databaseSystem);
            VectordbOtelMetric vectordbOtelMetric = getOrCreateMetric(vectordbMetrics, key, serviceName, databaseSystem);

            long startTime = dataPoint.getStartTimeUnixNano();
            long dataSum = dataPoint.getAsInt();
            String metricName = metric.getName();
            
            String mappedMetricName = mapToDbSpecificMetricName(metricName, databaseSystem);
            
            long lastStartTime = vectordbOtelMetric.getMetricStartTime(mappedMetricName);
            double lastCount = vectordbOtelMetric.getMetricSum(mappedMetricName);
    
            if (startTime != lastStartTime) {
                vectordbOtelMetric.setMetricStartTime(mappedMetricName, startTime);
                vectordbOtelMetric.addMetricDelta(mappedMetricName, dataSum);
            } else {
                vectordbOtelMetric.addMetricDelta(mappedMetricName, dataSum - lastCount);
            }
            vectordbOtelMetric.setMetricSum(mappedMetricName, dataSum);
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

    private static VectordbOtelMetric getOrCreateMetric(Map<String, VectordbOtelMetric> vectordbMetrics, String key, String serviceName, String databaseSystem) {
        return vectordbMetrics.computeIfAbsent(key, k -> {
            VectordbOtelMetric metric = new VectordbOtelMetric();
            metric.setServiceName(serviceName);
            metric.setDbSystem(databaseSystem);
            return metric;
        });
    }

    private static void processDurationMetric(VectordbOtelMetric metric, long startTime, double sum) {
        System.out.println("Recv Metric --- Duration Sum: " + sum);
        System.out.println("Recv Metric --- Start Time : " + startTime);
        System.out.println("Recv Metric --- End Time : " + System.currentTimeMillis() * 1_000_000);
        long lastStartTime = metric.getLastDurationStartTime();
        double lastDurationSum = metric.getLastDurationSum();

        if (startTime != lastStartTime) {
            metric.setLastDurationStartTime(startTime);
            metric.addDeltaDuration(sum);
            metric.incrementDurationCount();
        } else {
            metric.addDeltaDuration(sum - lastDurationSum);
            metric.incrementDurationCount();
        }
        metric.setLastDurationSum(sum);
    }

    private static void processDistanceMetric(VectordbOtelMetric metric, long startTime, double sum, String metricName) {
        // Use the mapped metricName directly
        System.out.println("Recv Metric --- Distance Sum: " + sum);
        System.out.println("Recv Metric --- Start Time : " + startTime);
        System.out.println("Recv Metric --- End Time : " + System.currentTimeMillis() * 1_000_000);
        long lastStartTime = metric.getMetricStartTime(metricName);
        double lastSearchDistanceSum = metric.getMetricSum(metricName);

        if (startTime != lastStartTime) {
            metric.setMetricStartTime(metricName, startTime);
            metric.addMetricDelta(metricName, sum);
            metric.incrementMetricCount(metricName);
        } else {
            metric.addMetricDelta(metricName, (sum - lastSearchDistanceSum));
            metric.incrementMetricCount(metricName);
        }
        metric.setMetricSum(metricName, sum);
    }
}
