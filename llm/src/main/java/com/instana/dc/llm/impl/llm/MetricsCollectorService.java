package com.instana.dc.llm.impl.llm;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.Map;

import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.HistogramDataPoint;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import io.opentelemetry.proto.resource.v1.Resource;

class MetricsCollectorService extends MetricsServiceGrpc.MetricsServiceImplBase {
    private static final Logger logger = Logger.getLogger(MetricsCollectorService.class.getName());

    private Object mutex = new Object();

    public class OtelMetric {
        private String modelId;
        private long promptTokens;
        private long completeTokens;
        private double duration;
        private long requestCount;
        private String aiSystem;

        public String getModelId() {
            return modelId;
        }

        public long getPromtTokens() {
            return promptTokens;
        }

        public long getCompleteTokens() {
            return completeTokens;
        }

        public double getDuration() {
            return duration;
        }

        public long getReqCount() {
            return requestCount;
        }

        public String getAiSystem() {
            return aiSystem;
        }

        public void setModelId(String modelId) {
            this.modelId = modelId;
        }

        public void setPromptTokens(long promptTokens) {
            this.promptTokens = promptTokens;
        }

        public void setCompleteTokens(long completeTokens) {
            this.completeTokens = completeTokens;
        }

        public void setDuration(double duration) {
            this.duration = duration;
        }

        public void setReqCount(long requestCount) {
            this.requestCount = requestCount;
        }

        public void setAiSystem(String aiSystem) {
            this.aiSystem = aiSystem;
        }
    }
    private HashMap<String, OtelMetric> exportMetrics = new HashMap<>();

    public List<OtelMetric> getMetrics() {
        synchronized (mutex) {
            return ImmutableList.copyOf(exportMetrics.values());
        }
    }

    public void clearMetrics() {
        exportMetrics.clear();
    }

    @Override
    public void export(
            ExportMetricsServiceRequest request,
            StreamObserver<ExportMetricsServiceResponse> responseObserver) {

        synchronized (mutex) {
            List<ResourceMetrics> allResourceMetrics = request.getResourceMetricsList();
            for (ResourceMetrics resourceMetrics : allResourceMetrics) {
                for (ScopeMetrics scoMetrics : resourceMetrics.getScopeMetricsList()) {
                    InstrumentationScope instrumentationScope = scoMetrics.getScope();
                    instrumentationScope.getAttributesList();
                    for (Metric metric : scoMetrics.getMetricsList()) {
                        System.out.println("-----------------");
                        System.out.println("Recv Metric --- Scope Name: " + metric.getName());
                        System.out.println("Recv Metric --- Scope Desc: " + metric.getDescription());
                        switch (metric.getDataCase()) {
                            case HISTOGRAM:
                                processHistogramMetrics(metric);
                                break;
                            case SUM:
                            case GAUGE:
                            case SUMMARY:
                            default:
                                System.out.println("Skip Metric DataCase: " + metric.getDataCase());
                        }
                    }
                }
            }
        }
        responseObserver.onNext(ExportMetricsServiceResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private void processHistogramMetrics(Metric metric) {
        if (metric.getName().compareTo("gen_ai.client.token.usage") == 0) {
            processTokenMetric(metric);
        } else if (metric.getName().compareTo("gen_ai.client.operation.duration") == 0) {
            processDurationMetric(metric);
        }
    }

    private void processTokenMetric(Metric metric) {
        List<HistogramDataPoint> histDataPoints = metric.getHistogram().getDataPointsList();
        for (HistogramDataPoint dataPoint : histDataPoints) {
            List<KeyValue> kvList = dataPoint.getAttributesList();
            String modelId = "";
            String tokenType = "";
            String aiSystem = "";
            for (KeyValue kv : kvList) {
                if (kv.getKey().compareTo("gen_ai.response.model") == 0) {
                    modelId = kv.getValue().getStringValue();
                    System.out.println("Recv Metric --- Model ID: " + modelId);
                } else if (kv.getKey().compareTo("gen_ai.system") == 0) {
                    aiSystem = kv.getValue().getStringValue();
                    System.out.println("Recv Metric --- AI System: " + aiSystem);
                } else if (kv.getKey().compareTo("gen_ai.token.type") == 0) {
                    tokenType = kv.getValue().getStringValue();
                }
            }
            if (!modelId.isEmpty()) {
                OtelMetric otelMetric = exportMetrics.get(modelId);
                if (otelMetric == null ) {
                    otelMetric = new OtelMetric();
                    exportMetrics.put(modelId, otelMetric);
                    otelMetric.setModelId(modelId);
                    otelMetric.setAiSystem(aiSystem);
                }
                if (tokenType.compareTo("input") == 0) {
                    long inputTokens = (long) dataPoint.getSum();
                    otelMetric.setPromptTokens(inputTokens);
                    System.out.println("Recv Metric --- Token Input: " + inputTokens);
                } else if (tokenType.compareTo("output") == 0) {
                    long outputTokens = (long) dataPoint.getSum();
                    otelMetric.setCompleteTokens(outputTokens);
                    System.out.println("Recv Metric --- Token Output: " + outputTokens);
                }
            }
        }
    }

    private void processDurationMetric(Metric metric) {
        List<HistogramDataPoint> histDataPoints = metric.getHistogram().getDataPointsList();
        for (HistogramDataPoint dataPoint : histDataPoints) {
            List<KeyValue> kvList = dataPoint.getAttributesList();
            String modelId = "";
            String aiSystem = "";
            for (KeyValue kv : kvList) {
                if (kv.getKey().compareTo("gen_ai.response.model") == 0) {
                    modelId = kv.getValue().getStringValue();
                    System.out.println("Recv Metric --- Model ID: " + modelId);
                } else if (kv.getKey().compareTo("gen_ai.system") == 0) {
                    aiSystem = kv.getValue().getStringValue();
                    System.out.println("Recv Metric --- AI System: " + aiSystem);
                }
            }
            if (!modelId.isEmpty()) {
                OtelMetric otelMetric = exportMetrics.get(modelId);
                if (otelMetric == null ) {
                    otelMetric = new OtelMetric();
                    exportMetrics.put(modelId, otelMetric);
                    otelMetric.setModelId(modelId);
                    otelMetric.setAiSystem(aiSystem);
                }
                Double durationSum = dataPoint.getSum();
                long requestCount = dataPoint.getCount();
                otelMetric.setDuration(durationSum);
                otelMetric.setReqCount(requestCount);
                System.out.println("Recv Metric --- Duration Sum: " + durationSum);
                System.out.println("Recv Metric --- Duration Count: " + requestCount);
            }
        }
    }
}
