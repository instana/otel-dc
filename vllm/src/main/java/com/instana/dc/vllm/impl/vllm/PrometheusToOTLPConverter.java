package com.instana.dc.vllm.impl.vllm;

import com.google.common.collect.ImmutableList;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.*;
import io.opentelemetry.proto.resource.v1.Resource;
import io.prometheus.client.exporter.common.TextFormat;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.instana.dc.vllm.VLLMDcConstants.INSTANCE_ID;
import static com.instana.dc.vllm.VLLMDcConstants.MODEL_NAME;

public class PrometheusToOTLPConverter {
    private final OkHttpClient client;
    private static final long startTimeUnixNano = System.currentTimeMillis() * 1_000_000L;

    public PrometheusToOTLPConverter() {
        this.client = new OkHttpClient();
    }


    public static class MetricsAggregation {

        public static class Measurement {
            private double value;
            private double cumulativeValue;
            private double sum;
            private double cumulativeSum;
            private double count;
            private double cumulativeCount;
            private long startTime;

            public Measurement(PrometheusToOTLPConverter.MetricsAggregation.Measurement other) {
                this.value = other.value;
                this.cumulativeValue = other.cumulativeValue;
                this.sum = other.sum;
                this.cumulativeSum = other.cumulativeSum;
                this.count = other.count;
                this.cumulativeCount = other.cumulativeCount;
                this.startTime = other.startTime;
            }

            public Measurement() {
            }

            public long getStartTime() {
                return startTime;
            }

            public void setStartTime(long startTime) {
                this.startTime = startTime;
            }

            public double getCumulativeValue() {
                return cumulativeValue;
            }

            public void setCumulativeValue(double cumulativeValue) {
                this.cumulativeValue = cumulativeValue;
            }

            public double getValue() {
                return value;
            }

            public void setValue(double value) {
                this.value = value;
            }

            public double getSum() {
                return sum;
            }

            public void setSum(double sum) {
                this.sum = sum;
            }

            public double getCumulativeSum() {
                return cumulativeSum;
            }

            public void setCumulativeSum(double cumulativeSum) {
                this.cumulativeSum = cumulativeSum;
            }

            public double getCount() {
                return count;
            }

            public void setCount(double count) {
                this.count = count;
            }

            public double getCumulativeCount() {
                return cumulativeCount;
            }

            public void setCumulativeCount(double cumulativeCount) {
                this.cumulativeCount = cumulativeCount;
            }

        }

        private final String instance;
        Map<String, Map<String, PrometheusToOTLPConverter.MetricsAggregation.Measurement>> metrics;

        public Map<String, Map<String, PrometheusToOTLPConverter.MetricsAggregation.Measurement>> getMetrics() {
            return metrics;
        }

        public String getInstance() {
            return instance;
        }

        public MetricsAggregation(String instance) {
            this.instance = instance;
            this.metrics = new HashMap<>();
        }

        public MetricsAggregation(PrometheusToOTLPConverter.MetricsAggregation other) {
            this.instance = other.instance;
            this.metrics = deepCopy(other);
        }

        private static Map<String, Map<String, PrometheusToOTLPConverter.MetricsAggregation.Measurement>> deepCopy(PrometheusToOTLPConverter.MetricsAggregation other) {
            return other.getMetrics().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, entry -> entry.getValue().entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, modelEntry -> new PrometheusToOTLPConverter.MetricsAggregation.Measurement(modelEntry.getValue())))));
        }

    }

    public void scrapeMetrics(String prometheusEndpoint) {
            Request request = new Request.Builder()
                    .url(prometheusEndpoint)
                    .header("Accept", TextFormat.CONTENT_TYPE_004)
                    .build();

            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String metricsData = response.body().string();
                    List<Metric> metrics = parsePrometheusFile(metricsData);
                    System.out.println("Total metrics parsed: " + metrics.size());
                    ExportMetricsServiceRequest serviceRequest = exportToOTLP(metrics);
                    processMetrics(serviceRequest);
                }
            } catch (IOException e) {
                System.err.println("Error scraping metrics: " + e.getMessage());
            }
    }

    private static List<Metric> parsePrometheusFile(String metricData) throws IOException {
        List<Metric> metrics = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new StringReader(metricData));
        String line;

        Pattern helpPattern = Pattern.compile("^# HELP (\\S+) (.+)$");
        Pattern metricPattern = Pattern.compile("^(\\S+)\\{(.*?)\\}\\s+(\\S+)$");
        Map<String, String> metricDescriptions = new HashMap<>();
        Map<String, TreeMap<Double, Long>> histogramBuckets = new HashMap<>();
        Map<String, Long> histogramCounts = new HashMap<>();
        Map<String, String> histogramLabels = new HashMap<>();
        Map<String, Double> histogramSums = new HashMap<>();
        long currentTimeNano = System.currentTimeMillis() * 1_000_000L;
        while ((line = reader.readLine()) != null) {
            Matcher helpMatcher = helpPattern.matcher(line);
            if (helpMatcher.matches()) {
                metricDescriptions.put(helpMatcher.group(1), helpMatcher.group(2).trim().replaceAll("\\\\$", ""));
                continue;
            }

            Matcher matcher = metricPattern.matcher(line);
            if (matcher.matches()) {
                String metricName = matcher.group(1).trim().replaceAll("\\\\$", "");
                String labels = matcher.group(2).trim();
                String valueStr = matcher.group(3).trim().replaceAll("[^0-9.eE-]", "");
                String description = metricDescriptions.getOrDefault(metricName, "Metric description not available");
                if (valueStr.isEmpty()) {
                    System.err.println("Empty metric value detected for " + metricName);
                    continue;
                }

                try {
                    double value = Double.parseDouble(valueStr);
                    if (metricName.endsWith("_bucket")) {
                        String baseName = metricName.replace("_bucket", "");
                        Pattern labelPattern = Pattern.compile("le=\"([^\"]+)\"|model_name=\"([^\"]+)\"");
                        Matcher labelMatcher = labelPattern.matcher(labels);
                        String boundaryStr = null;

                        while (labelMatcher.find()) {
                            if (labelMatcher.group(1) != null) {
                                boundaryStr = labelMatcher.group(1);
                            }
                        }
                        assert boundaryStr != null;
                        if (!boundaryStr.equals("+Inf")) {
                            double boundary = Double.parseDouble(boundaryStr);
                            histogramBuckets.computeIfAbsent(baseName, k -> new TreeMap<>()).put(boundary, (long) value);
                        }
                    } else if (metricName.endsWith("_count")) {
                        String baseName = metricName.replace("_count", "");
                        histogramCounts.put(baseName, (long) value);
                        histogramLabels.put(baseName, labels);
                    } else if (metricName.endsWith("_sum")) {
                        String baseName = metricName.replace("_sum", "");
                        histogramSums.put(baseName, value);
                    }else if (metricName.endsWith("_total")) {
                        metrics.add(createCounterMetric(metricName, labels, value, description,currentTimeNano));
                    } else {
                        metrics.add(createGaugeMetric(metricName, labels, value, description,currentTimeNano));
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Skipping invalid metric value: " + valueStr + " for metric: " + metricName + " with labels: " + labels);
                }
            }
        }
        reader.close();

        for (String histName : histogramBuckets.keySet()) {
            String labels = histogramLabels.getOrDefault(histName, "");
            TreeMap<Double, Long> cumulativeBuckets = histogramBuckets.get(histName);
            List<Double> explicitBounds = new ArrayList<>(cumulativeBuckets.keySet());
            List<Long> bucketCounts = new ArrayList<>();
            long prevCount = 0;
            for (double bound : explicitBounds) {
                long currentCount = cumulativeBuckets.get(bound);
                bucketCounts.add(currentCount - prevCount);
                prevCount = currentCount;
            }
            metrics.add(createHistogramMetric(histName, labels, explicitBounds, bucketCounts,
                    histogramCounts.getOrDefault(histName, 0L), histogramSums.getOrDefault(histName, 0.0),
                    metricDescriptions.getOrDefault(histName, "Histogram metric"),
                    currentTimeNano));
        }
        return metrics;
    }

    private static Metric createGaugeMetric(String name, String labels, double value, String description, long timestamp) {
        return Metric.newBuilder()
                .setName(name)
                .setDescription(description.replaceAll("\\\\$", ""))
                .setGauge(Gauge.newBuilder()
                        .addDataPoints(NumberDataPoint.newBuilder()
                                .setAsDouble(value)
                                .setTimeUnixNano(timestamp)
                                .addAllAttributes(parseAttributes(labels, "gauge"))
                                .build())
                        .build())
                .build();
    }

private static Metric createCounterMetric(String name, String labels, double value, String description, long timestamp) {
    return Metric.newBuilder()
            .setName(name)
            .setDescription(description.replaceAll("\\\\$", ""))
            .setSum(Sum.newBuilder()
                    .setAggregationTemporality(AggregationTemporality.AGGREGATION_TEMPORALITY_CUMULATIVE)
                    .setIsMonotonic(true)
                    .addDataPoints(NumberDataPoint.newBuilder()
                            .setAsDouble(value)
                            .setStartTimeUnixNano(startTimeUnixNano)
                            .setTimeUnixNano(timestamp)
                            .addAllAttributes(parseAttributes(labels, "counter"))
                            .build())
                    .build())
            .build();
}
    private static Metric createHistogramMetric(String name, String labels, List<Double> explicitBounds, List<Long> bucketCounts, long count, double sum, String description, long timestamp) {
        HistogramDataPoint.Builder dataPoint = HistogramDataPoint.newBuilder()
                .setStartTimeUnixNano(startTimeUnixNano)
                .setTimeUnixNano(timestamp)
                .setCount(count)
                .setSum(sum)
                .addAllAttributes(parseAttributes(labels, "histogram"))
                .addAllBucketCounts(bucketCounts)
                .addAllExplicitBounds(explicitBounds);

        return Metric.newBuilder()
                .setName(name)
                .setDescription(description.replaceAll("\\\\$", ""))
                .setHistogram(Histogram.newBuilder()
                        .setAggregationTemporality(AggregationTemporality.AGGREGATION_TEMPORALITY_CUMULATIVE)
                        .addDataPoints(dataPoint.build())
                        .build())
                .build();
    }

    private static List<KeyValue> parseAttributes(String labels, String metricType) {
        List<KeyValue> attributes = new ArrayList<>();
        String[] labelPairs = labels.split(",");

        for (String pair : labelPairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].replaceAll("\\\\$", "").replace("\"", "").trim();
                attributes.add(KeyValue.newBuilder()
                        .setKey(key)
                        .setValue(AnyValue.newBuilder().setStringValue(value).build())
                        .build());
            }
        }
        return attributes;
    }

private static ExportMetricsServiceRequest exportToOTLP(List<Metric> metrics) throws IOException {
    ResourceMetrics resourceMetrics = ResourceMetrics.newBuilder()
            .setResource(Resource.newBuilder()
                    .addAttributes(KeyValue.newBuilder()
                            .setKey("service.name")
                            .setValue(AnyValue.newBuilder().setStringValue("prometheus-metrics").build())
                            .build())
                    .addAttributes(KeyValue.newBuilder()
                            .setKey("service.instance.id")
                            .setValue(AnyValue.newBuilder().setStringValue("9.30.109.130:8000").build())
                            .build())
                    .build())
            .addScopeMetrics(ScopeMetrics.newBuilder()
                    .setScope(InstrumentationScope.newBuilder()
                            .setName("github.com/open-telemetry/opentelemetry-collector-contrib/receiver/prometheusreceiver")
                            .setVersion("0.121.0")
                            .build())
                    .addAllMetrics(metrics)
                    .build())
            .build();

    return ExportMetricsServiceRequest.newBuilder()
            .addResourceMetrics(resourceMetrics)
            .build();

}

    private static final HashMap<String, PrometheusToOTLPConverter.MetricsAggregation>  exportMetrics = new HashMap<>();

    public List<PrometheusToOTLPConverter.MetricsAggregation> getDeltaMetricsList() {
            return exportMetrics.values().stream()
                    .filter(Objects::nonNull)
                    .map(PrometheusToOTLPConverter.MetricsAggregation::new)
                    .collect(ImmutableList.toImmutableList());
    }
    private static void processMetrics(ExportMetricsServiceRequest request) {

        List<ResourceMetrics> allResourceMetrics = request.getResourceMetricsList();
        for (ResourceMetrics resourceMetric : allResourceMetrics) {
            String instance = getInstanceId(resourceMetric);
            PrometheusToOTLPConverter.MetricsAggregation metricsAggregation = exportMetrics.computeIfAbsent(instance, key -> new PrometheusToOTLPConverter.MetricsAggregation(instance));
            for (ScopeMetrics scopeMetrics : resourceMetric.getScopeMetricsList()) {
                for (Metric metric : scopeMetrics.getMetricsList()) {
                    System.out.println("-----------------");
                    System.out.println("Recv Metric --- Scope Name: " + metric.getName());
                    System.out.println("Recv Metric --- Scope Desc: " + metric.getDescription());
                    switch (metric.getDataCase()) {
                        case GAUGE: processGauge(metric, metricsAggregation);
                            break;
                        case SUM: processSum(metric, metricsAggregation);
                            break;
                        case HISTOGRAM: processHistogram(metric, metricsAggregation);
                            break;
                        default:
                            System.out.println("Skip Metric DataCase: " + metric.getDataCase());
                    }
                }
                System.out.println();
            }
        }
    }

    private static void processGauge(Metric metric, PrometheusToOTLPConverter.MetricsAggregation metricsAggregation) {
        Map<String, PrometheusToOTLPConverter.MetricsAggregation.Measurement> measurement = metricsAggregation.getMetrics()
                .computeIfAbsent(metric.getName(), key -> new HashMap<>());
        for (NumberDataPoint dataPoint : metric.getGauge().getDataPointsList()) {
            dataPoint.getAttributesList().stream().filter(attribute -> attribute.getKey().equals(MODEL_NAME))
                    .map(KeyValue::getValue).map(AnyValue::getStringValue).findAny().ifPresent(model -> {
                        PrometheusToOTLPConverter.MetricsAggregation.Measurement modelMeasurement = measurement.computeIfAbsent(model, key -> new PrometheusToOTLPConverter.MetricsAggregation.Measurement());
                        recordGauge(dataPoint, modelMeasurement);
                    });
        }
    }

    private static void recordGauge(NumberDataPoint dataPoint, PrometheusToOTLPConverter.MetricsAggregation.Measurement modelMeasurement) {
        modelMeasurement.setValue(dataPoint.getAsDouble());
    }

    private static void processSum(Metric metric, PrometheusToOTLPConverter.MetricsAggregation metricsAggregation) {
        Map<String, PrometheusToOTLPConverter.MetricsAggregation.Measurement> measurement = metricsAggregation.getMetrics()
                .computeIfAbsent(metric.getName(), key -> new HashMap<>());
        for (NumberDataPoint dataPoint : metric.getSum().getDataPointsList()) {
            dataPoint.getAttributesList().stream().filter(attribute -> attribute.getKey().equals(MODEL_NAME))
                    .map(KeyValue::getValue).map(AnyValue::getStringValue).findAny().ifPresent(model -> {
                        PrometheusToOTLPConverter.MetricsAggregation.Measurement modelMeasurement = measurement.computeIfAbsent(model, key -> new PrometheusToOTLPConverter.MetricsAggregation.Measurement());
                        recordSum(dataPoint, modelMeasurement);
                    });
        }
    }

    private static void recordSum(NumberDataPoint dataPoint, PrometheusToOTLPConverter.MetricsAggregation.Measurement modelMeasurement) {
        if (dataPoint.getStartTimeUnixNano() == modelMeasurement.getStartTime()) {
            modelMeasurement.setValue(dataPoint.getAsDouble() - modelMeasurement.getCumulativeValue());
            modelMeasurement.setCumulativeValue(dataPoint.getAsDouble());
        } else {
            modelMeasurement.setValue(dataPoint.getAsDouble());
            modelMeasurement.setCumulativeValue(dataPoint.getAsDouble() + modelMeasurement.getCumulativeValue());
            modelMeasurement.setStartTime(dataPoint.getStartTimeUnixNano());
        }
    }

    private static void processHistogram(Metric metric, PrometheusToOTLPConverter.MetricsAggregation metricsAggregation) {
        Map<String, PrometheusToOTLPConverter.MetricsAggregation.Measurement> measurement = metricsAggregation.getMetrics()
                .computeIfAbsent(metric.getName(), key -> new HashMap<>());
        for (HistogramDataPoint dataPoint : metric.getHistogram().getDataPointsList()) {
            dataPoint.getAttributesList().stream().filter(attribute -> attribute.getKey().equals(MODEL_NAME))
                    .map(KeyValue::getValue).map(AnyValue::getStringValue).findAny().ifPresent(model -> {
                        PrometheusToOTLPConverter.MetricsAggregation.Measurement modelMeasurement = measurement.computeIfAbsent(model, key -> new PrometheusToOTLPConverter.MetricsAggregation.Measurement());
                        recordHistogram(dataPoint, modelMeasurement);
                    });
        }
    }

    private static void recordHistogram(HistogramDataPoint dataPoint, PrometheusToOTLPConverter.MetricsAggregation.Measurement modelMeasurement) {
        if (dataPoint.getStartTimeUnixNano() == modelMeasurement.getStartTime()) {
            modelMeasurement.setCount(dataPoint.getCount() - modelMeasurement.getCumulativeCount());
            modelMeasurement.setCumulativeCount(dataPoint.getCount());
            modelMeasurement.setSum(dataPoint.getSum() - modelMeasurement.getCumulativeSum());
            modelMeasurement.setCumulativeSum(dataPoint.getSum());
        } else {
            modelMeasurement.setCount(dataPoint.getCount());
            modelMeasurement.setCumulativeCount(dataPoint.getCount() + modelMeasurement.getCumulativeCount());
            modelMeasurement.setSum(dataPoint.getSum());
            modelMeasurement.setCumulativeSum(dataPoint.getSum() + modelMeasurement.getCumulativeSum());
            modelMeasurement.setStartTime(dataPoint.getStartTimeUnixNano());
        }
    }

    private static String getInstanceId(ResourceMetrics resourceMetrics) {
        return resourceMetrics.getResource().getAttributesList().stream()
                .filter(keyValue -> INSTANCE_ID.equals(keyValue.getKey()))
                .findAny()
                .map(KeyValue::getValue)
                .map(AnyValue::getStringValue)
                .orElse("");
    }

}
