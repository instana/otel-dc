package com.instana.dc.vllm.impl.vllm;

import static com.instana.dc.DcUtil.OTEL_EXPORTER_OTLP_HEADERS;
import static com.instana.dc.vllm.VLLMDcConstants.INSTANCE_ID;
import static com.instana.dc.vllm.VLLMDcConstants.MODEL_NAME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.instana.dc.HeadersSupplier;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.RequestContext;
import com.linecorp.armeria.common.RequestHeaders;

import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.internal.StringUtils;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.HistogramDataPoint;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;

class MetricsCollectorService extends MetricsServiceGrpc.MetricsServiceImplBase {

    private final Object mutex = new Object();

    public static class MetricsAggregation {

        public static class Measurement {
            private double value;
            private double cumulativeValue;
            private double sum;
            private double cumulativeSum;
            private double count;
            private double cumulativeCount;
            private long startTime;

            public Measurement(Measurement other) {
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
        Map<String, Map<String, Measurement>> metrics;

        public Map<String, Map<String, Measurement>> getMetrics() {
            return metrics;
        }

        public String getInstance() {
            return instance;
        }

        public MetricsAggregation(String instance) {
            this.instance = instance;
            this.metrics = new HashMap<>();
        }

        public MetricsAggregation(MetricsAggregation other) {
            this.instance = other.instance;
            this.metrics = deepCopy(other);
        }

        private static Map<String, Map<String, Measurement>> deepCopy(MetricsAggregation other) {
            return other.getMetrics().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, entry -> entry.getValue().entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, modelEntry -> new Measurement(modelEntry.getValue())))));
        }

    }

    private final HashMap<String, MetricsAggregation> exportMetrics = new HashMap<>();

    public List<MetricsAggregation> getDeltaMetricsList() {
        synchronized (mutex) {
            return exportMetrics.values().stream()
                .filter(Objects::nonNull)
                .map(MetricsAggregation::new)
                .collect(ImmutableList.toImmutableList());
        }
    }

    @Override
    public void export(
            ExportMetricsServiceRequest request,
            StreamObserver<ExportMetricsServiceResponse> responseObserver) {
        System.out.println(request);

        if (System.getenv(OTEL_EXPORTER_OTLP_HEADERS) == null) {
            generateHeaders();
        }

        synchronized (mutex) {
            List<ResourceMetrics> allResourceMetrics = request.getResourceMetricsList();
            for (ResourceMetrics resourceMetrics : allResourceMetrics) {
                String instance = getInstanceId(resourceMetrics);
                System.out.println("Recv Metric --- vLLM instance id: " + instance);
                MetricsAggregation metricsAggregation = exportMetrics.computeIfAbsent(instance, key -> new MetricsAggregation(instance));
                for (ScopeMetrics scopeMetrics : resourceMetrics.getScopeMetricsList()) {
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
        responseObserver.onNext(ExportMetricsServiceResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private static String getInstanceId(ResourceMetrics resourceMetrics) {
        return resourceMetrics.getResource().getAttributesList().stream()
                .filter(keyValue -> INSTANCE_ID.equals(keyValue.getKey()))
                .findAny()
                .map(KeyValue::getValue)
                .map(AnyValue::getStringValue)
                .orElse("");
    }

    private static void generateHeaders() {
        HttpRequest httpRequest = RequestContext.current().request();
        RequestHeaders headers = httpRequest != null ? httpRequest.headers() : null;
        if (headers != null) {
            HeadersSupplier supplier = HeadersSupplier.INSTANCE;
            Map<String, String> newHeaders = new HashMap<>();
            String xInstanaKey = headers.get("x-instana-key");
            if (!StringUtils.isNullOrEmpty(xInstanaKey)) {
                newHeaders.put("x-instana-key", xInstanaKey);
            }
            String xInstanaHost = headers.get("x-instana-host");
            if (!StringUtils.isNullOrEmpty(xInstanaHost)) {
                newHeaders.put("x-instana-host", xInstanaHost);
            }
            if (!newHeaders.isEmpty()) {
                supplier.updateHeaders(newHeaders);
            }
        }
    }

    private void processGauge(Metric metric, MetricsAggregation metricsAggregation) {
        Map<String, MetricsAggregation.Measurement> measurement = metricsAggregation.getMetrics()
                .computeIfAbsent(metric.getName(), key -> new HashMap<>());
        for (NumberDataPoint dataPoint : metric.getGauge().getDataPointsList()) {
            dataPoint.getAttributesList().stream().filter(attribute -> attribute.getKey().equals(MODEL_NAME))
                    .map(KeyValue::getValue).map(AnyValue::getStringValue).findAny().ifPresent(model -> {
                        MetricsAggregation.Measurement modelMeasurement = measurement.computeIfAbsent(model, key -> new MetricsAggregation.Measurement());
                        recordGauge(dataPoint, modelMeasurement);
                    });
        }
    }

    private static void recordGauge(NumberDataPoint dataPoint, MetricsAggregation.Measurement modelMeasurement) {
        modelMeasurement.setValue(dataPoint.getAsDouble());
    }

    private static void processSum(Metric metric, MetricsAggregation metricsAggregation) {
        Map<String, MetricsAggregation.Measurement> measurement = metricsAggregation.getMetrics()
                .computeIfAbsent(metric.getName(), key -> new HashMap<>());
        for (NumberDataPoint dataPoint : metric.getSum().getDataPointsList()) {
            dataPoint.getAttributesList().stream().filter(attribute -> attribute.getKey().equals(MODEL_NAME))
                    .map(KeyValue::getValue).map(AnyValue::getStringValue).findAny().ifPresent(model -> {
                        MetricsAggregation.Measurement modelMeasurement = measurement.computeIfAbsent(model, key -> new MetricsAggregation.Measurement());
                        recordSum(dataPoint, modelMeasurement);
                    });
        }
    }

    private static void recordSum(NumberDataPoint dataPoint, MetricsAggregation.Measurement modelMeasurement) {
        if (dataPoint.getStartTimeUnixNano() == modelMeasurement.getStartTime()) {
            modelMeasurement.setValue(dataPoint.getAsDouble() - modelMeasurement.getCumulativeValue());
            modelMeasurement.setCumulativeValue(dataPoint.getAsDouble());
        } else {
            modelMeasurement.setValue(dataPoint.getAsDouble());
            modelMeasurement.setCumulativeValue(dataPoint.getAsDouble() + modelMeasurement.getCumulativeValue());
            modelMeasurement.setStartTime(dataPoint.getStartTimeUnixNano());
        }
    }

    private static void processHistogram(Metric metric, MetricsAggregation metricsAggregation) {
        Map<String, MetricsAggregation.Measurement> measurement = metricsAggregation.getMetrics()
                .computeIfAbsent(metric.getName(), key -> new HashMap<>());
        for (HistogramDataPoint dataPoint : metric.getHistogram().getDataPointsList()) {
            dataPoint.getAttributesList().stream().filter(attribute -> attribute.getKey().equals(MODEL_NAME))
                    .map(KeyValue::getValue).map(AnyValue::getStringValue).findAny().ifPresent(model -> {
                        MetricsAggregation.Measurement modelMeasurement = measurement.computeIfAbsent(model, key -> new MetricsAggregation.Measurement());
                        recordHistogram(dataPoint, modelMeasurement);
                    });
        }
    }

    private static void recordHistogram(HistogramDataPoint dataPoint, MetricsAggregation.Measurement modelMeasurement) {
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

}
