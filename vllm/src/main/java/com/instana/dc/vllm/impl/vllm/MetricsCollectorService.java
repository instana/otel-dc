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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.instana.dc.vllm.VLLMDcConstants.INSTANCE_ID;
import static com.instana.dc.vllm.VLLMDcConstants.MODEL_NAME;

public class MetricsCollectorService {
    private final OkHttpClient client;
    private static final long START_TIME_UNIX_NANO = System.currentTimeMillis() * 1_000_000L;

    public MetricsCollectorService() {
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

            public Measurement(MetricsCollectorService.MetricsAggregation.Measurement other) {
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
        ConcurrentMap<String, ConcurrentMap<String, MetricsCollectorService.MetricsAggregation.Measurement>> metrics;

        public ConcurrentMap<String, ConcurrentMap<String, MetricsCollectorService.MetricsAggregation.Measurement>> getMetrics() {
            return metrics;
        }

        public String getInstance() {
            return instance;
        }

        public MetricsAggregation(String instance) {
            this.instance = instance;
            this.metrics = new ConcurrentHashMap<>();
        }

        public MetricsAggregation(MetricsCollectorService.MetricsAggregation other) {
            this.instance = other.instance;
            this.metrics = deepCopy(other);
        }

        private static ConcurrentMap<String, ConcurrentMap<String, MetricsCollectorService.MetricsAggregation.Measurement>> deepCopy(MetricsCollectorService.MetricsAggregation other) {
            return other.getMetrics().entrySet().stream()
                    .collect(Collectors.toConcurrentMap(
                            Map.Entry::getKey, entry -> entry.getValue().entrySet().stream()
                                    .collect(Collectors.toConcurrentMap(Map.Entry::getKey, modelEntry -> new MetricsCollectorService.MetricsAggregation.Measurement(modelEntry.getValue())))));
        }
    }

    public void scrapeMetrics(List<String> endpoints) {
        List<CompletableFuture<Void>> futures = endpoints.stream()
                .map(endpoint -> CompletableFuture.runAsync(() -> scrapeSingleEndpoint(endpoint)))
                .collect(Collectors.toList());

        for (CompletableFuture<Void> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error in async metric scrape: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    private void scrapeSingleEndpoint(String endpoint) {
        try {
            URI uri = validateEndpoint(endpoint);
            boolean isHealthy = healthCheck(uri);
            List<Metric> metrics = new ArrayList<>();
            metrics.add(Metric.newBuilder()
                    .setName("vllm.health")
                    .setDescription("Health check status of the Prometheus endpoint")
                    .setGauge(Gauge.newBuilder()
                            .addDataPoints(NumberDataPoint.newBuilder()
                                    .setAsDouble(isHealthy ? 1 : 0)
                                    .setTimeUnixNano(System.currentTimeMillis() * 1_000_000L)
                                    .addAttributes(KeyValue.newBuilder()
                                            .setKey("instance")
                                            .setValue(AnyValue.newBuilder().setStringValue(uri.getAuthority()).build())
                                            .build())
                                    .build())
                            .build())
                    .build());
            if (isHealthy) {
                String metricsData = fetchMetrics(uri);
                metrics.addAll(parsePrometheusFile(metricsData));
            }
            ExportMetricsServiceRequest serviceRequest = exportToOTLP(metrics, uri.getAuthority());
            processMetrics(serviceRequest);
        } catch (Exception e) {
            System.err.println("[" + endpoint + "] Error scraping metrics: " + e.getMessage());
        }
    }

    private URI validateEndpoint(String endpoint) throws URISyntaxException {
        URI uri = new URI(endpoint);
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("Invalid URL: Invalid host in " + endpoint);
        }
        if (uri.getPort() == -1) {
            throw new IllegalArgumentException("URL must include a port: " + endpoint);
        }
        return uri;
    }

    private String fetchMetrics(URI uri) throws IOException {
        Request request = new Request.Builder()
                .url(uri.toString() + "/metrics")
                .header("Accept", TextFormat.CONTENT_TYPE_004)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
            throw new IOException("HTTP status: " + response.code());
        }
    }

    private boolean healthCheck(URI uri) {
        Request request = new Request.Builder()
                .url(uri.toString() + "/health")
                .header("Accept", TextFormat.CONTENT_TYPE_004)
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("response: " + response);
            System.out.println(response.isSuccessful());
            System.out.println(response.body());
            System.out.println(response.code());
            if (response.isSuccessful() && response.body() != null) {
                return true;
            }
        } catch (IOException ignored) {

        }
        return false;
    }

    private static final Pattern HELP_PATTERN = Pattern.compile("^# HELP (\\S+) (.+)$");
    private static final Pattern METRIC_PATTERN = Pattern.compile("^(\\S+)\\{(.*?)\\}\\s+(\\S+)$");


    private static List<Metric> parsePrometheusFile(String metricData) throws IOException {
        List<Metric> metrics = new ArrayList<>();
        Map<String, String> metricDescriptions = new HashMap<>();
        Map<String, TreeMap<Double, Long>> histogramBuckets = new HashMap<>();
        Map<String, Long> histogramCounts = new HashMap<>();
        Map<String, String> histogramLabels = new HashMap<>();
        Map<String, Double> histogramSums = new HashMap<>();
        long currentTimeNano = System.currentTimeMillis() * 1_000_000L;

        BufferedReader reader = new BufferedReader(new StringReader(metricData));
        String line;

        while ((line = reader.readLine()) != null) {
            Matcher helpMatcher = HELP_PATTERN.matcher(line);
            if (helpMatcher.matches()) {
                parseHelpLine(helpMatcher, metricDescriptions);
                continue;
            }

            parseMetricLine(line, metricDescriptions, metrics,
                    histogramBuckets, histogramCounts, histogramLabels,
                    histogramSums, currentTimeNano);
        }

        reader.close();

        buildHistograms(metrics, histogramBuckets, histogramCounts, histogramLabels, histogramSums, metricDescriptions, currentTimeNano);

        return metrics;
    }

    private static void parseHelpLine(Matcher matcher, Map<String, String> descriptions) {
        String metricName = matcher.group(1);
        String description = matcher.group(2).trim().replaceAll("\\\\$", "");
        descriptions.put(metricName, description);
    }

    private static void parseMetricLine(String line,
                                        Map<String, String> descriptions,
                                        List<Metric> metrics,
                                        Map<String, TreeMap<Double, Long>> histogramBuckets,
                                        Map<String, Long> histogramCounts,
                                        Map<String, String> histogramLabels,
                                        Map<String, Double> histogramSums,
                                        long currentTimeNano) {
        Matcher matcher = METRIC_PATTERN.matcher(line);
        if (!matcher.matches())
            return;

        String name = clean(matcher.group(1));
        String labels = matcher.group(2).trim();
        String valueStr = cleanValue(matcher.group(3));
        if (valueStr.isEmpty()) {
            System.err.println("Empty metric value for " + name);
            return;
        }

        try {
            double value = Double.parseDouble(valueStr);
            String description = descriptions.getOrDefault(name, "Metric description not available");

            if (name.endsWith("_bucket")) {
                handleHistogramBucket(name, labels, value, histogramBuckets);
            } else if (name.endsWith("_count")) {
                String baseName = name.replace("_count", "");
                histogramCounts.put(baseName, (long) value);
                histogramLabels.put(baseName, labels);
            } else if (name.endsWith("_sum")) {
                String baseName = name.replace("_sum", "");
                histogramSums.put(baseName, value);
            } else if (name.endsWith("_total")) {
                metrics.add(createCounterMetric(name, labels, value, description, currentTimeNano));
            } else {
                metrics.add(createGaugeMetric(name, labels, value, description, currentTimeNano));
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid metric value: " + valueStr + " for " + name);
        }
    }

    private static void handleHistogramBucket(String name, String labels, double value,
                                              Map<String, TreeMap<Double, Long>> histogramBuckets) {
        String baseName = name.replace("_bucket", "");
        Pattern labelPattern = Pattern.compile("le=\"([^\"]+)\"");
        Matcher matcher = labelPattern.matcher(labels);
        String boundaryStr = null;
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                boundaryStr = matcher.group(1);
            }
        }
        if (boundaryStr == null || boundaryStr.equals("+Inf"))
            return;
        double boundary = Double.parseDouble(boundaryStr);
        histogramBuckets.computeIfAbsent(baseName, k -> new TreeMap<>()).put(boundary, (long) value);
    }

    private static void buildHistograms(List<Metric> metrics,
                                        Map<String, TreeMap<Double, Long>> histogramBuckets,
                                        Map<String, Long> histogramCounts,
                                        Map<String, String> histogramLabels,
                                        Map<String, Double> histogramSums,
                                        Map<String, String> metricDescriptions,
                                        long currentTimeNano) {
        for (Map.Entry<String, TreeMap<Double, Long>> histogramEntry : histogramBuckets.entrySet()) {
            String histName = histogramEntry.getKey();
            TreeMap<Double, Long> cumulativeBuckets = histogramEntry.getValue();
            String labels = histogramLabels.getOrDefault(histName, "");

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
    }

    private static Metric createGaugeMetric(String name, String labels, double value, String description, long timestamp) {
        return Metric.newBuilder()
                .setName(name)
                .setDescription(description.replaceAll("\\\\$", ""))
                .setGauge(Gauge.newBuilder()
                        .addDataPoints(NumberDataPoint.newBuilder()
                                .setAsDouble(value)
                                .setTimeUnixNano(timestamp)
                                .addAllAttributes(parseAttributes(labels))
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
                                .setStartTimeUnixNano(START_TIME_UNIX_NANO)
                                .setTimeUnixNano(timestamp)
                                .addAllAttributes(parseAttributes(labels))
                                .build())
                        .build())
                .build();
    }

    private static Metric createHistogramMetric(String name, String labels, List<Double> explicitBounds,
                                                List<Long> bucketCounts, long count, double sum, String description,
                                                long timestamp) {
        HistogramDataPoint.Builder dataPoint = HistogramDataPoint.newBuilder()
                .setStartTimeUnixNano(START_TIME_UNIX_NANO)
                .setTimeUnixNano(timestamp)
                .setCount(count)
                .setSum(sum)
                .addAllAttributes(parseAttributes(labels))
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

    private static List<KeyValue> parseAttributes(String labels) {
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

    private static ExportMetricsServiceRequest exportToOTLP(List<Metric> metrics, String serviceInstance) {
        ResourceMetrics resourceMetrics = ResourceMetrics.newBuilder()
                .setResource(Resource.newBuilder()
                        .addAttributes(KeyValue.newBuilder()
                                .setKey("service.name")
                                .setValue(AnyValue.newBuilder().setStringValue("prometheus-metrics").build())
                                .build())
                        .addAttributes(KeyValue.newBuilder()
                                .setKey("service.instance.id")
                                .setValue(AnyValue.newBuilder().setStringValue(serviceInstance).build())
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

    private static final ConcurrentMap<String, MetricsAggregation> exportMetrics = new ConcurrentHashMap<>();

    public List<MetricsCollectorService.MetricsAggregation> getDeltaMetricsList() {
        return exportMetrics.values().stream()
                .filter(Objects::nonNull)
                .map(MetricsCollectorService.MetricsAggregation::new)
                .collect(ImmutableList.toImmutableList());
    }

    private static void processMetrics(ExportMetricsServiceRequest request) {
        List<ResourceMetrics> allResourceMetrics = request.getResourceMetricsList();
        for (ResourceMetrics resourceMetric : allResourceMetrics) {
            String instance = getInstanceId(resourceMetric);
            MetricsCollectorService.MetricsAggregation metricsAggregation = exportMetrics.computeIfAbsent(instance, key -> new MetricsCollectorService.MetricsAggregation(instance));
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

    private static void processGauge(Metric metric, MetricsCollectorService.MetricsAggregation metricsAggregation) {
        Map<String, MetricsCollectorService.MetricsAggregation.Measurement> measurement = metricsAggregation.getMetrics()
                .computeIfAbsent(metric.getName(), key -> new ConcurrentHashMap<>());
        for (NumberDataPoint dataPoint : metric.getGauge().getDataPointsList()) {
            dataPoint.getAttributesList().stream().filter(attribute -> Arrays.asList(MODEL_NAME, "instance").contains(attribute.getKey()))
                    .map(KeyValue::getValue).map(AnyValue::getStringValue).findAny().ifPresent(model -> {
                        MetricsCollectorService.MetricsAggregation.Measurement modelMeasurement = measurement.computeIfAbsent(model, key -> new MetricsCollectorService.MetricsAggregation.Measurement());
                        recordGauge(dataPoint, modelMeasurement);
                    });
        }
    }

    private static void recordGauge(NumberDataPoint dataPoint, MetricsCollectorService.MetricsAggregation.Measurement modelMeasurement) {
        modelMeasurement.setValue(dataPoint.getAsDouble());
    }

    private static void processSum(Metric metric, MetricsCollectorService.MetricsAggregation metricsAggregation) {
        Map<String, MetricsCollectorService.MetricsAggregation.Measurement> measurement = metricsAggregation.getMetrics()
                .computeIfAbsent(metric.getName(), key -> new ConcurrentHashMap<>());
        for (NumberDataPoint dataPoint : metric.getSum().getDataPointsList()) {
            dataPoint.getAttributesList().stream().filter(attribute -> attribute.getKey().equals(MODEL_NAME))
                    .map(KeyValue::getValue).map(AnyValue::getStringValue).findAny().ifPresent(model -> {
                        MetricsCollectorService.MetricsAggregation.Measurement modelMeasurement = measurement.computeIfAbsent(model, key -> new MetricsCollectorService.MetricsAggregation.Measurement());
                        recordSum(dataPoint, modelMeasurement);
                    });
        }
    }

    private static void recordSum(NumberDataPoint dataPoint, MetricsCollectorService.MetricsAggregation.Measurement modelMeasurement) {
        if (dataPoint.getStartTimeUnixNano() == modelMeasurement.getStartTime()) {
            modelMeasurement.setValue(dataPoint.getAsDouble() - modelMeasurement.getCumulativeValue());
            modelMeasurement.setCumulativeValue(dataPoint.getAsDouble());
        } else {
            modelMeasurement.setValue(dataPoint.getAsDouble());
            modelMeasurement.setCumulativeValue(dataPoint.getAsDouble() + modelMeasurement.getCumulativeValue());
            modelMeasurement.setStartTime(dataPoint.getStartTimeUnixNano());
        }
    }

    private static void processHistogram(Metric metric, MetricsCollectorService.MetricsAggregation metricsAggregation) {
        Map<String, MetricsCollectorService.MetricsAggregation.Measurement> measurement = metricsAggregation.getMetrics()
                .computeIfAbsent(metric.getName(), key -> new ConcurrentHashMap<>());
        for (HistogramDataPoint dataPoint : metric.getHistogram().getDataPointsList()) {
            dataPoint.getAttributesList().stream().filter(attribute -> attribute.getKey().equals(MODEL_NAME))
                    .map(KeyValue::getValue).map(AnyValue::getStringValue).findAny().ifPresent(model -> {
                        MetricsCollectorService.MetricsAggregation.Measurement modelMeasurement = measurement.computeIfAbsent(model, key -> new MetricsCollectorService.MetricsAggregation.Measurement());
                        recordHistogram(dataPoint, modelMeasurement);
                    });
        }
    }

    private static void recordHistogram(HistogramDataPoint dataPoint, MetricsCollectorService.MetricsAggregation.Measurement modelMeasurement) {
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

    private static String clean(String raw) {
        return raw.trim().replaceAll("\\\\$", "");
    }

    private static String cleanValue(String raw) {
        return raw.trim().replaceAll("[^0-9.eE-]", "");
    }
}
