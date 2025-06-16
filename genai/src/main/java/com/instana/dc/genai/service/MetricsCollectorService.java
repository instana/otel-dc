package com.instana.dc.genai.service;

import com.google.common.collect.ImmutableList;
import com.instana.dc.HeadersSupplier;
import com.instana.dc.genai.llm.LLMMetricProcessor;
import com.instana.dc.genai.llm.metrics.LLMOtelMetric;
import com.instana.dc.genai.metrics.OtelMetric;
import com.instana.dc.genai.vectordb.VectordbMetricProcessor;
import com.instana.dc.genai.vectordb.metrics.VectordbOtelMetric;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.RequestContext;
import com.linecorp.armeria.common.RequestHeaders;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.OTEL_EXPORTER_OTLP_HEADERS;

public class MetricsCollectorService extends MetricsServiceGrpc.MetricsServiceImplBase {
    private static final Logger logger = Logger.getLogger(MetricsCollectorService.class.getName());
    private static final MetricsCollectorService INSTANCE = new MetricsCollectorService();
    private final Map<String, LLMOtelMetric> llmMetrics = new ConcurrentHashMap<>();
    private final Map<String, VectordbOtelMetric> vectordbMetrics = new ConcurrentHashMap<>();
    private static final Set<String> VECTOR_DB_SYSTEMS = Set.of("milvus");

    private MetricsCollectorService() {
        // Private constructor for singleton
    }

    public static MetricsCollectorService getInstance() {
        return INSTANCE;
    }

    public List<OtelMetric> getDeltaMetrics() {
        List<OtelMetric> metrics = new ArrayList<>();
        metrics.addAll(llmMetrics.values());
        metrics.addAll(vectordbMetrics.values());
        return metrics;
    }

    public List<VectordbOtelMetric> getVectordbDeltaMetrics() {
        return vectordbMetrics.values().stream().collect(ImmutableList.toImmutableList());
    }

    public List<LLMOtelMetric> getLLMDeltaMetrics() {
        return llmMetrics.values().stream().collect(ImmutableList.toImmutableList());
    }

    public void resetDeltaMetrics() {
        resetLLMMetrics();
        resetVectordbMetrics();
    }

    private void resetLLMMetrics() {
        for (LLMOtelMetric metric : llmMetrics.values()) {
            metric.resetDeltaValues();
        }
    }

    private void resetVectordbMetrics() {
        for (VectordbOtelMetric metric : vectordbMetrics.values()) {
            metric.resetDeltaValues();
        }
    }

    @Override
    public void export(
            ExportMetricsServiceRequest request,
            StreamObserver<ExportMetricsServiceResponse> responseObserver) {
        processHeaders();
        processMetrics(request);
        sendResponse(responseObserver);
    }

    private void processHeaders() {
        if (System.getenv(OTEL_EXPORTER_OTLP_HEADERS) == null) {
            HttpRequest httpRequest = RequestContext.current().request();
            RequestHeaders headers = httpRequest != null ? httpRequest.headers() : null;
            if (headers != null) {
                updateHeadersFromRequest(headers);
            }
        }
    }

    private void updateHeadersFromRequest(RequestHeaders headers) {
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

        if (!newHeaders.isEmpty()) {
            supplier.updateHeaders(newHeaders);
        }
    }

    private void processMetrics(ExportMetricsServiceRequest request) {
        for (ResourceMetrics resourceMetrics : request.getResourceMetricsList()) {
            String serviceName = extractServiceName(resourceMetrics);
            if (serviceName != null) {
                processScopeMetrics(resourceMetrics.getScopeMetricsList(), serviceName);
            }
        }
    }

    private String extractServiceName(ResourceMetrics resourceMetrics) {
        return resourceMetrics.getResource().getAttributesList().stream()
                .filter(attr -> "service.name".equals(attr.getKey()))
                .map(attr -> attr.getValue().getStringValue())
                .findFirst()
                .orElse(null);
    }

    private void processScopeMetrics(List<ScopeMetrics> scopeMetricsList, String serviceName) {
        for (ScopeMetrics scopeMetrics : scopeMetricsList) {
            for (Metric metric : scopeMetrics.getMetricsList()) {
                logMetricInfo(metric);
                processMetric(metric, serviceName);
            }
            logger.log(Level.FINE, "Completed processing scope metrics");
        }
    }

    private void logMetricInfo(Metric metric) {
        logger.log(Level.FINE, "Processing metric - Name: {0}, Description: {1}",
                new Object[]{metric.getName(), metric.getDescription()});
    }

    private void processMetric(Metric metric, String serviceName) {
        if (metric.getName().contains("gen_ai.client")) {
            LLMMetricProcessor.processLLMMetric(metric, llmMetrics, serviceName);
        } else if (isVectordbMetric(metric)) {
            VectordbMetricProcessor.processVectordbMetric(metric, vectordbMetrics, serviceName);
        } else {
            logger.log(Level.FINE, "Skipping unknown metric type: {0}", metric.getName());
        }
    }

    private boolean isVectordbMetric(Metric metric) {
        if (!metric.getName().startsWith("db.")) {
            return false;
        }

        switch (metric.getDataCase()) {
            case HISTOGRAM:
                return metric.getHistogram().getDataPointsList().stream()
                        .anyMatch(dp -> hasVectordbSystem(dp.getAttributesList()));
            case SUM:
                return metric.getSum().getDataPointsList().stream()
                        .anyMatch(dp -> hasVectordbSystem(dp.getAttributesList()));
            default:
                return false;
        }
    }

    private boolean hasVectordbSystem(List<io.opentelemetry.proto.common.v1.KeyValue> attributes) {
        return attributes.stream()
                .anyMatch(attr -> "db.system".equals(attr.getKey()) &&
                        VECTOR_DB_SYSTEMS.contains(attr.getValue().getStringValue()));
    }

    private void sendResponse(StreamObserver<ExportMetricsServiceResponse> responseObserver) {
        responseObserver.onNext(ExportMetricsServiceResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
