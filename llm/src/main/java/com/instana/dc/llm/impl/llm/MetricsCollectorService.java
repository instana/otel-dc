package com.instana.dc.llm.impl.llm;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableList;

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

    private final BlockingQueue<OtelMetric> exportMetrics = new LinkedBlockingDeque<>();

    public List<OtelMetric> getMetrics() {
        synchronized (mutex) {
            return ImmutableList.copyOf(exportMetrics);
        }
    }

    public void clearMetrics() {
        exportMetrics.clear();
    }

    @Override
    public void export(
            ExportMetricsServiceRequest request,
            StreamObserver<ExportMetricsServiceResponse> responseObserver) {

        System.out.println("--------------------------------------------------------");

        synchronized (mutex) {

            System.out.println(request.toString());

            List<ResourceMetrics> allResourceMetrics = request.getResourceMetricsList();
            for (ResourceMetrics resourceMetrics : allResourceMetrics) {

                Resource resource = resourceMetrics.getResource();
                for (KeyValue reskv : resource.getAttributesList()) {
                    System.out.println("Received metric --- Resource attrKey: " + reskv.getKey());
                    System.out.println("Received metric --- Resource attrVal: " + reskv.getValue().getStringValue());
                }

                for (ScopeMetrics scoMetrics : resourceMetrics.getScopeMetricsList()) {
                    InstrumentationScope instrumentationScope = scoMetrics.getScope();
                    instrumentationScope.getAttributesList();
                    for (KeyValue inskv : instrumentationScope.getAttributesList()) {
                        System.out.println("Received metric --- Scope attrKey: " + inskv.getKey());
                        System.out.println("Received metric --- Scope attrVal: " + inskv.getValue().getStringValue());
                    }

                    for (Metric metric : scoMetrics.getMetricsList()) {
                        System.out.println("Received metric --- Scope Name: " + metric.getName());
                        System.out.println("Received metric --- Scope Desc: " + metric.getDescription());
                        System.out.println("Received metric --- Scope Unit: " + metric.getUnit());
                        System.out.println("Received metric --- Scope Case: " + metric.getDataCase().getNumber());

                        switch (metric.getDataCase()) {
                            case HISTOGRAM:
                                if (metric.getName().compareTo("gen_ai.client.operation.duration") == 0) {
                                    List<HistogramDataPoint> histDataPoints = metric.getHistogram().getDataPointsList();
                                    for (HistogramDataPoint dataPoint : histDataPoints) {
                                        List<KeyValue> kvList = dataPoint.getAttributesList();
                                        String modelId = "";
                                        String aiSystem = "";
                                        for (KeyValue kv : kvList) {
                                            System.out.println("Received metric --- HISTOGRAM attrKey: " + kv.getKey());
                                            System.out.println("Received metric --- HISTOGRAM attrVal: "
                                                    + kv.getValue().getStringValue());
                                            if (kv.getKey().compareTo("gen_ai.response.model") == 0) {
                                                modelId = kv.getValue().getStringValue();
                                            } else if (kv.getKey().compareTo("gen_ai.system") == 0) {
                                                aiSystem = kv.getValue().getStringValue();
                                            }
                                        }
                                        Double durationSum = dataPoint.getSum();
                                        long requestCount = dataPoint.getCount();
                                        System.out.println("Received metric --- HISTOGRAM Sum Value: " + durationSum);
                                        System.out.println("Received metric --- HISTOGRAM Count Value: " + requestCount);

                                        if (!modelId.isEmpty()) {
                                            OtelMetric otelMetric = new OtelMetric();
                                            otelMetric.setModelId(modelId);
                                            otelMetric.setAiSystem(aiSystem);
                                            otelMetric.setDuration(durationSum);
                                            otelMetric.setReqCount(requestCount);
                                            exportMetrics.add(otelMetric);
                                        }
                                    }
                                }
                                if (metric.getName().compareTo("gen_ai.client.token.usage") == 0) {
                                    List<HistogramDataPoint> histDataPoints = metric.getHistogram().getDataPointsList();
                                    for (HistogramDataPoint dataPoint : histDataPoints) {
                                        List<KeyValue> kvList = dataPoint.getAttributesList();
                                        String modelId = "";
                                        String tokenType = "";
                                        String aiSystem = "";
                                        for (KeyValue kv : kvList) {
                                            System.out.println("Received metric --- HISTOGRAM attrKey: " + kv.getKey());
                                            System.out.println("Received metric --- HISTOGRAM attrVal: "
                                                    + kv.getValue().getStringValue());
                                            if (kv.getKey().compareTo("gen_ai.response.model") == 0) {
                                                modelId = kv.getValue().getStringValue();
                                            } else if (kv.getKey().compareTo("gen_ai.token.type") == 0) {
                                                tokenType = kv.getValue().getStringValue();
                                            } else if (kv.getKey().compareTo("gen_ai.system") == 0) {
                                                aiSystem = kv.getValue().getStringValue();
                                            }
                                        }

                                        long inputTokens = 0;
                                        long outputTokens = 0;
                                        //long requestCount = 0;
                                        if (tokenType.compareTo("input") == 0) {
                                            inputTokens = (long)dataPoint.getSum();
                                            //requestCount = dataPoint.getCount();
                                            System.out.println("Received metric --- input Value: " + inputTokens);
                                        } else if (tokenType.compareTo("output") == 0) {
                                            outputTokens = (long)dataPoint.getSum();
                                            System.out.println("Received metric --- output Value: " + outputTokens);
                                        }

                                        if (!modelId.isEmpty()) {
                                            OtelMetric otelMetric = new OtelMetric();
                                            otelMetric.setModelId(modelId);
                                            otelMetric.setAiSystem(aiSystem);
                                            if(inputTokens > 0) {
                                                otelMetric.setPromptTokens(inputTokens);
                                            }
                                            if(outputTokens > 0) {
                                                otelMetric.setCompleteTokens(outputTokens);
                                            }
                                            exportMetrics.add(otelMetric);
                                        }
                                    }
                                }
                                break;
                            case SUM:
                            case GAUGE:
                            case SUMMARY:
                            default:
                                System.out.println("Unsupported metric DataCase: " + metric.getDataCase());
                                //throw new AssertionError("Unsupported metric DataCase: " + metric.getDataCase());
                        }
                    }
                }
            }
        }

        responseObserver.onNext(ExportMetricsServiceResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
