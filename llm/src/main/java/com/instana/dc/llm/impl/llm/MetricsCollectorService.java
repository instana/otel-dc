package com.instana.dc.llm.impl.llm;

import com.google.common.collect.ImmutableList;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.HistogramDataPoint;
import io.opentelemetry.proto.resource.v1.Resource;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;

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

        logger.info("--------------------------------------------------------");

        synchronized (mutex) {

            List<ResourceMetrics> allResourceMetrics = request.getResourceMetricsList();
            for (ResourceMetrics resourceMetrics : allResourceMetrics) {

                Resource resource = resourceMetrics.getResource();
                for (KeyValue reskv : resource.getAttributesList()) {
                    logger.info("Received metric --- Resource attrKey: " + reskv.getKey());
                    logger.info("Received metric --- Resource attrVal: " + reskv.getValue().getStringValue());
                }

                for (ScopeMetrics scoMetrics : resourceMetrics.getScopeMetricsList()) {
                    InstrumentationScope instrumentationScope = scoMetrics.getScope();
                    instrumentationScope.getAttributesList();
                    for (KeyValue inskv : instrumentationScope.getAttributesList()) {
                        logger.info("Received metric --- Scope attrKey: " + inskv.getKey());
                        logger.info("Received metric --- Scope attrVal: " + inskv.getValue().getStringValue());
                    }

                    for (Metric metric : scoMetrics.getMetricsList()) {
                        logger.info("Received metric --- Scope Name: " + metric.getName());
                        logger.info("Received metric --- Scope Desc: " + metric.getDescription());
                        logger.info("Received metric --- Scope Unit: " + metric.getUnit());
                        logger.info("Received metric --- Scope Case: " + metric.getDataCase().getNumber());

                        switch (metric.getDataCase()) {
                            case SUM:
                                if (metric.getName().compareTo("llm.watsonx.completions.tokens") == 0 ||
                                        metric.getName().compareTo("llm.openai.chat_completions.tokens") == 0 ||
                                        metric.getName().compareTo("llm.anthropic.completion.tokens") == 0 ||
                                        metric.getName().compareTo("gen_ai.client.token.usage") == 0) {

                                    List<NumberDataPoint> sumDataPoints = metric.getSum().getDataPointsList();
                                    for (NumberDataPoint dataPoint : sumDataPoints) {

                                        List<KeyValue> kvList = dataPoint.getAttributesList();

                                        String modelId = "";
                                        String tokenType = "";
                                        String aiSystem = "";
                                        for (KeyValue kv : kvList) {
                                            logger.info("Received metric --- Tokens attrKey: " + kv.getKey());
                                            logger.info("Received metric --- Tokens attrVal: "
                                                    + kv.getValue().getStringValue());
                                            if (kv.getKey().compareTo("llm.response.model") == 0 || kv.getKey().compareTo("gen_ai.response.model") == 0) {
                                                modelId = kv.getValue().getStringValue();
                                            } else if (kv.getKey().compareTo("llm.usage.token_type") == 0 || kv.getKey().compareTo("gen_ai.token.type") == 0) {
                                                tokenType = kv.getValue().getStringValue();
                                            } else if (kv.getKey().compareTo("gen_ai.system") == 0) {
                                                aiSystem = kv.getValue().getStringValue();
                                            }
                                        }
                                        if (aiSystem.isEmpty() && metric.getName().compareTo("gen_ai.client.token.usage") != 0) {
                                            String[] parts = metric.getName().split("\\.", 3);
                                            aiSystem = parts[1];
                                        } else {
                                            aiSystem = "n/a";
                                        }

                                        long promptTokens = 0;
                                        long completeTokens = 0;
                                        if (tokenType.compareTo("prompt") == 0 || tokenType.compareTo("input") == 0) {
                                            promptTokens = dataPoint.getAsInt();
                                            logger.info("Received metric --- Prompt Value: " + promptTokens);
                                        } else if (tokenType.compareTo("completion") == 0 || tokenType.compareTo("output") == 0) {
                                            completeTokens = dataPoint.getAsInt();
                                            logger.info("Received metric --- Complete Value: " + completeTokens);
                                        }

                                        if (!modelId.isEmpty()) {
                                            OtelMetric otelMetric = new OtelMetric();
                                            otelMetric.setModelId(modelId);
                                            otelMetric.setAiSystem(aiSystem);
                                            if(promptTokens > 0) {
                                                otelMetric.setPromptTokens(promptTokens);
                                            }
                                            if(completeTokens > 0) {
                                                otelMetric.setCompleteTokens(completeTokens);
                                            }
                                            exportMetrics.add(otelMetric);
                                        }
                                    }
                                }
                                break;
                            case HISTOGRAM:
                                if (metric.getName().compareTo("llm.watsonx.completions.duration") == 0 ||
                                        metric.getName().compareTo("llm.openai.chat_completions.duration") == 0 ||
                                        metric.getName().compareTo("llm.anthropic.completion.duration") == 0 ||
                                        metric.getName().compareTo("gen_ai.client.operation.duration") == 0) {

                                    List<HistogramDataPoint> histDataPoints = metric.getHistogram().getDataPointsList();
                                    for (HistogramDataPoint dataPoint : histDataPoints) {

                                        List<KeyValue> kvList = dataPoint.getAttributesList();

                                        String modelId = "";
                                        String aiSystem = "";
                                        for (KeyValue kv : kvList) {
                                            logger.info("Received metric --- Duration attrKey: " + kv.getKey());
                                            logger.info("Received metric --- Duration attrVal: "
                                                    + kv.getValue().getStringValue());
                                            if (kv.getKey().compareTo("llm.response.model") == 0 || kv.getKey().compareTo("gen_ai.response.model") == 0) {
                                                modelId = kv.getValue().getStringValue();
                                            } else if (kv.getKey().compareTo("gen_ai.system") == 0) {
                                                aiSystem = kv.getValue().getStringValue();
                                            }
                                        }
                                        if (aiSystem.isEmpty() && metric.getName().compareTo("gen_ai.client.token.usage") != 0) {
                                            String[] parts = metric.getName().split("\\.", 3);
                                            aiSystem = parts[1];
                                        } else {
                                            aiSystem = "n/a";
                                        }

                                        Double durationSum = dataPoint.getSum();
                                        long requestCount = dataPoint.getCount();
                                        logger.info("Received metric --- Duration Sum Value: " + durationSum);
                                        logger.info("Received metric --- Duration Count Value: " + requestCount);

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
                                break;
                            case GAUGE:
                            case SUMMARY:
                            default:
                                logger.info("Unsupported metric DataCase: " + metric.getDataCase());
                                throw new AssertionError("Unsupported metric DataCase: " + metric.getDataCase());
                        }
                    }
                }
            }
        }

        responseObserver.onNext(ExportMetricsServiceResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
