package com.instana.dc.genai.service;

import static com.instana.dc.DcUtil.OTEL_EXPORTER_OTLP_HEADERS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.instana.dc.HeadersSupplier;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.RequestContext;
import com.linecorp.armeria.common.RequestHeaders;
import com.instana.dc.genai.llm.LLMMetricProcessor;
import com.instana.dc.genai.llm.metrics.LLMOtelMetric;
import com.instana.dc.genai.vectordb.VectordbMetricProcessor;
import com.instana.dc.genai.vectordb.metrics.VectordbOtelMetric;
import com.instana.dc.genai.metrics.OtelMetric;

import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;

public class MetricsCollectorService extends MetricsServiceGrpc.MetricsServiceImplBase {
    private static final Logger logger = Logger.getLogger(MetricsCollectorService.class.getName());
    private static final MetricsCollectorService INSTANCE = new MetricsCollectorService();
    private final Map<String, LLMOtelMetric> llmMetrics = new ConcurrentHashMap<>();
    private final Map<String, VectordbOtelMetric> vectordbMetrics = new ConcurrentHashMap<>();

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
            if (serviceName == null) {
                continue;
            }
            processScopeMetrics(resourceMetrics.getScopeMetricsList(), serviceName);
        }
    }

    private String extractServiceName(ResourceMetrics resourceMetrics) {
        for (KeyValue attribute : resourceMetrics.getResource().getAttributesList()) {
            if (attribute.getKey().equals("service.name")) {
                return attribute.getValue().getStringValue();
            }
        }
        return null;
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
        switch (metric.getDataCase()) {
            case HISTOGRAM:
                processHistogramMetric(metric, serviceName);
                break;
            case SUM:
                processSumMetric(metric, serviceName);
                break;
            case GAUGE:
            case SUMMARY:
            default:
                logger.log(Level.FINE, "Skipping metric with DataCase: {0}", metric.getDataCase());
        }
    }

    private void processHistogramMetric(Metric metric, String serviceName) {
        if (metric.getName().contains("gen_ai.client")) {
            LLMMetricProcessor.processLLMMetric(metric, llmMetrics, serviceName);
        } else if (metric.getName().contains("db.milvus")) {
            VectordbMetricProcessor.processVectordbHistogramMetric(metric, vectordbMetrics, serviceName);
        }
    }

    private void processSumMetric(Metric metric, String serviceName) {
        if (metric.getName().contains("db.milvus")) {
            VectordbMetricProcessor.processVectordbCounterMetric(metric, vectordbMetrics, serviceName);
        }
    }

    private void sendResponse(StreamObserver<ExportMetricsServiceResponse> responseObserver) {
        responseObserver.onNext(ExportMetricsServiceResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }
} 