package com.instana.dc.vllm.impl.vllm;

import static com.instana.dc.DcUtil.OTEL_EXPORTER_OTLP_HEADERS;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_GENERATION_TOKENS_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_GPU_CACHE_HIT_RATE_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_GPU_CACHE_USAGE_PERC_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_PENDING_REQ_COUNT_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_PROMPT_TOKENS_NAME;
import static com.instana.dc.vllm.VLLMDcUtil.VLLM_REQ_COUNT_NAME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.instana.dc.HeadersSupplier;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.RequestContext;
import com.linecorp.armeria.common.RequestHeaders;

import io.grpc.stub.StreamObserver;
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
import io.opentelemetry.proto.resource.v1.Resource;

class MetricsCollectorService extends MetricsServiceGrpc.MetricsServiceImplBase {
    private static final HashMap<String, String> GAUGES = new HashMap<>();
    private static final HashMap<String, String> SUMS = new HashMap<>();
    private static final HashMap<String, Map<String, String>> HISTOGRAMS = new HashMap<>();
    static {
        GAUGES.put("vllm:num_requests_running", VLLM_REQ_COUNT_NAME);
        GAUGES.put("vllm:num_requests_waiting", VLLM_PENDING_REQ_COUNT_NAME);
        GAUGES.put("vllm:gpu_cache_usage_perc", VLLM_GPU_CACHE_USAGE_PERC_NAME);
        GAUGES.put("vllm:gpu_prefix_cache_hit_rate", VLLM_GPU_CACHE_HIT_RATE_NAME);

        SUMS.put("vllm:prompt_tokens_total", VLLM_PROMPT_TOKENS_NAME);
        SUMS.put("vllm:generation_tokens_total", VLLM_GENERATION_TOKENS_NAME);

        HISTOGRAMS.put("vllm:e2e_request_latency_seconds", Map.of("sum", "llm.total.duration", "count", "llm.total.requests"));
        HISTOGRAMS.put("vllm:time_to_first_token_seconds", Map.of("sum", "llm.total.ttft.duration", "count", "llm.total.ttft.requests"));
    }

    private final Object mutex = new Object();

    public static class OtelMetric {
        public static class Measure {
            private double value;
            private double cumulativeValue;

            public Measure(Measure other) {
                this.value = other.value;
                this.cumulativeValue = other.cumulativeValue;
                this.startTime = other.startTime;
            }

            private long startTime;

            public Measure() {
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

        }

        private final String serviceName;
        Map<String, Map<String, Measure>> metrics;

        public Map<String, Map<String, Measure>> getMetrics() {
            return metrics;
        }

        public String getServiceName() {
            return serviceName;
        }

        public OtelMetric(String instance) {
            this.serviceName = instance;
            this.metrics = new HashMap<>();
        }

        public OtelMetric(OtelMetric other) {
            this.serviceName = other.serviceName;
            this.metrics = deepCopy(other);
        }

        private static Map<String, Map<String, Measure>> deepCopy(OtelMetric other) {
            return other.getMetrics().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, entry -> entry.getValue().entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, modelEntry -> new Measure(modelEntry.getValue())))));
        }

    }

    private final HashMap<String, OtelMetric> exportMetrics = new HashMap<>();

    public List<OtelMetric> getDeltaMetricsList() {
        synchronized (mutex) {
            return exportMetrics.values().stream()
                .filter(Objects::nonNull)
                .map(OtelMetric::new)
                .collect(ImmutableList.toImmutableList());
        }
    }

    @Override
    public void export(
            ExportMetricsServiceRequest request,
            StreamObserver<ExportMetricsServiceResponse> responseObserver) {
        System.out.println(request);

        if (System.getenv(OTEL_EXPORTER_OTLP_HEADERS) == null) {
            HttpRequest httpRequest = RequestContext.current().request();
            RequestHeaders headers = httpRequest!=null ? httpRequest.headers() : null;
            if ( headers != null ) {
                HeadersSupplier supplier = HeadersSupplier.INSTANCE;
                Map<String, String> newHeaders = new HashMap<>();
                String xInstanaKey = headers.get("x-instana-key");
                if (xInstanaKey != null && !xInstanaKey.isEmpty()) {
                    newHeaders.put("x-instana-key", xInstanaKey);
                }
                String xInstanaHost = headers.get("x-instana-host");
                if (xInstanaHost != null && !xInstanaHost.isEmpty()) {
                    newHeaders.put("x-instana-host", xInstanaHost);
                }
                if ( ! newHeaders.isEmpty() ) {
                    supplier.updateHeaders(newHeaders);
                }
            }
        }

        synchronized (mutex) {
            List<ResourceMetrics> allResourceMetrics = request.getResourceMetricsList();
            for (ResourceMetrics resourceMetrics : allResourceMetrics) {
                Resource resource = resourceMetrics.getResource();
                String instance = resource.getAttributesList().stream()
                        .filter(keyValue -> "service.instance.id".equals(keyValue.getKey()))
                        .findAny()
                        .map(KeyValue::getValue)
                        .map(AnyValue::getStringValue)
                        .orElse("");
                System.out.println("Recv Metric --- vLLM instance id: " + instance);
                OtelMetric otelMetric = exportMetrics.computeIfAbsent(instance, key -> new OtelMetric(instance));
                for (ScopeMetrics scopeMetrics : resourceMetrics.getScopeMetricsList()) {
                    for (Metric metric : scopeMetrics.getMetricsList()) {
                        System.out.println("-----------------");
                        System.out.println("Recv Metric --- Scope Name: " + metric.getName());
                        System.out.println("Recv Metric --- Scope Desc: " + metric.getDescription());
                        switch (metric.getDataCase()) {
                            case GAUGE: processGauge(metric, otelMetric);
                                break;
                            case HISTOGRAM: processHistogram(metric, otelMetric);
                                break;
                            case SUM: processSum(metric, otelMetric);
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

    private void processGauge(Metric metric, OtelMetric otelMetric) {
        Optional.ofNullable(GAUGES.get(metric.getName())).ifPresent(gauge -> {
            Map<String, OtelMetric.Measure> measure = otelMetric.getMetrics().computeIfAbsent(gauge, key -> new HashMap<>());
            for (NumberDataPoint dataPoint : metric.getGauge().getDataPointsList()) {
                dataPoint.getAttributesList().stream().filter(attribute -> attribute.getKey().equals("model_name"))
                        .map(KeyValue::getValue).map(AnyValue::getStringValue).findAny().ifPresent(model -> {
                            OtelMetric.Measure modelMeasure = measure.computeIfAbsent(model, key -> new OtelMetric.Measure());
                            modelMeasure.setValue(dataPoint.getAsDouble());
                        });
            }
        });

    }

    private static void processSum(Metric metric, OtelMetric otelMetric) {
        Optional.ofNullable(SUMS.get(metric.getName())).ifPresent(sum -> {
            Map<String, OtelMetric.Measure> measure = otelMetric.getMetrics().computeIfAbsent(sum, key -> new HashMap<>());
            for (NumberDataPoint dataPoint : metric.getSum().getDataPointsList()) {
                dataPoint.getAttributesList().stream().filter(attribute -> attribute.getKey().equals("model_name"))
                        .map(KeyValue::getValue).map(AnyValue::getStringValue).findAny().ifPresent(model -> {
                            OtelMetric.Measure modelMeasure = measure.computeIfAbsent(model, key -> new OtelMetric.Measure());
                            if (dataPoint.getStartTimeUnixNano() == modelMeasure.getStartTime()) {
                                modelMeasure.setValue(dataPoint.getAsDouble() - modelMeasure.getCumulativeValue());
                                modelMeasure.setCumulativeValue(dataPoint.getAsDouble());
                            } else {
                                modelMeasure.setValue(dataPoint.getAsDouble());
                                modelMeasure.setCumulativeValue(dataPoint.getAsDouble() + modelMeasure.getCumulativeValue());
                                modelMeasure.setStartTime(dataPoint.getStartTimeUnixNano());
                            }
                        });
            }
        });
    }

    private static void processHistogram(Metric metric, OtelMetric otelMetric) {
        Optional.ofNullable(HISTOGRAMS.get(metric.getName())).ifPresent(histogram -> {
            Map<String, OtelMetric.Measure> measureRequests = otelMetric.getMetrics().computeIfAbsent(histogram.get("count"), key -> new HashMap<>());
            Map<String, OtelMetric.Measure> measureDuration = otelMetric.getMetrics().computeIfAbsent(histogram.get("sum"), key -> new HashMap<>());
            for (HistogramDataPoint dataPoint : metric.getHistogram().getDataPointsList()) {
                dataPoint.getAttributesList().stream().filter(attribute -> attribute.getKey().equals("model_name"))
                        .map(KeyValue::getValue).map(AnyValue::getStringValue).findAny().ifPresent(model -> {
                            OtelMetric.Measure modelMeasureRequests = measureRequests.computeIfAbsent(model, key -> new OtelMetric.Measure());
                            OtelMetric.Measure modelMeasureDuration = measureDuration.computeIfAbsent(model, key -> new OtelMetric.Measure());
                            if (dataPoint.getStartTimeUnixNano() == modelMeasureRequests.getStartTime()) {
                                modelMeasureRequests.setValue(dataPoint.getCount() - modelMeasureRequests.getCumulativeValue());
                                modelMeasureRequests.setCumulativeValue(dataPoint.getCount());
                                modelMeasureDuration.setValue(dataPoint.getSum() - modelMeasureDuration.getCumulativeValue());
                                modelMeasureDuration.setCumulativeValue(dataPoint.getSum());
                            } else {
                                modelMeasureRequests.setValue(dataPoint.getCount());
                                modelMeasureRequests.setCumulativeValue(dataPoint.getCount() + modelMeasureRequests.getCumulativeValue());
                                modelMeasureDuration.setValue(dataPoint.getSum());
                                modelMeasureDuration.setCumulativeValue(dataPoint.getSum() + modelMeasureDuration.getCumulativeValue());
                                modelMeasureRequests.setStartTime(dataPoint.getStartTimeUnixNano());
                            }
                        });
            }
        });
    }

}
