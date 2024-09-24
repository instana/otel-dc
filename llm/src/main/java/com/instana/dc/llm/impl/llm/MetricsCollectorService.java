package com.instana.dc.llm.impl.llm;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
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
        private double inputTokenSum;
        private double outputTokenSum;
        private double durationSum;
        private long requestCount;
        private String aiSystem;
        private long durationStartTime;
        private long inputTokenStartTime;
        private long outputTokenStartTime;
        private long deltaInputTokens;
        private long deltaOutputTokens;
        private long deltaDuration;
        private long deltaRequestCount;

        public OtelMetric() {}

        public OtelMetric(OtelMetric other) {
            this.modelId = other.modelId;
            this.inputTokenSum = other.inputTokenSum;
            this.outputTokenSum = other.outputTokenSum;
            this.durationSum = other.durationSum;
            this.requestCount = other.requestCount;
            this.aiSystem = other.aiSystem;
            this.durationStartTime = other.durationStartTime;
            this.inputTokenStartTime = other.inputTokenStartTime;
            this.outputTokenStartTime = other.outputTokenStartTime;
            this.deltaInputTokens = other.deltaInputTokens;
            this.deltaOutputTokens = other.deltaOutputTokens;
            this.deltaDuration = other.deltaDuration;
            this.deltaRequestCount = other.deltaRequestCount;
        }

        public String getModelId() {
            return modelId;
        }

        public String getAiSystem() {
            return aiSystem;
        }

        public double getLastInputTokenSum() {
            return inputTokenSum;
        }

        public double getLastOutputTokenSum() {
            return outputTokenSum;
        }

        public double getLastDurationSum() {
            return durationSum;
        }

        public long getLastRequestCount() {
            return requestCount;
        }

        public long getDeltaInputTokens() {
            return deltaInputTokens;
        }

        public long getDeltaOutputTokens() {
            return deltaOutputTokens;
        }

        public long getDeltaDuration() {
            return deltaDuration;
        }

        public long getDeltaRequestCount() {
            return deltaRequestCount;
        }

        public long getLastDurationStartTime() {
            return durationStartTime;
        }

        public long getLastInputTokenStartTime() {
            return inputTokenStartTime;
        }

        public long getLastOutputTokenStartTime() {
            return outputTokenStartTime;
        }

        public void setModelId(String modelId) {
            this.modelId = modelId;
        }

        public void setAiSystem(String aiSystem) {
            this.aiSystem = aiSystem;
        }

        public void setLastInputTokenSum(double inputTokenSum) {
            this.inputTokenSum = inputTokenSum;
        }

        public void setLastOutputTokenSum(double outputTokenSum) {
            this.outputTokenSum = outputTokenSum;
        }

        public void setLastDurationSum(double durationSum) {
            this.durationSum = durationSum;
        }

        public void setLastRequestCount(long requestCount) {
            this.requestCount = requestCount;
        }

        public void addDeltaInputTokens(double deltaInputTokens) {
            this.deltaInputTokens += (long)deltaInputTokens;
        }

        public void addDeltaOutputTokens(double deltaOutputTokens) {
            this.deltaOutputTokens += (long)deltaOutputTokens;
        }

        public void addDeltaDuration(double deltaDuration) {
            this.deltaDuration += (long)(deltaDuration*1000); // seconds to milliseconds
        }

        public void addDeltaRequestCount(long deltaRequestCount) {
            this.deltaRequestCount += deltaRequestCount;
        }

        public void setLastDurationStartTime(long startTime) {
            this.durationStartTime = startTime;
        }

        public void setLastInputTokenStartTime(long startTime) {
            this.inputTokenStartTime = startTime;
        }

        public void setLastOutputTokenStartTime(long startTime) {
            this.outputTokenStartTime = startTime;
        }

        public void resetDeltaValues() {
            this.deltaInputTokens = 0;
            this.deltaOutputTokens = 0;
            this.deltaDuration = 0;
            this.deltaRequestCount = 0;
        }
    }
    private HashMap<String, OtelMetric> exportMetrics = new HashMap<>();

    public List<OtelMetric> getDeltaMetricsList() {
        synchronized (mutex) {
            return exportMetrics.values().stream()
                .filter(Objects::nonNull)
                .map(OtelMetric::new)
                .collect(ImmutableList.toImmutableList());
        }
    }

    public void resetMetricsDetla() {
        for (OtelMetric metric : exportMetrics.values()) {
            metric.resetDeltaValues();
        }
    }

    @Override
    public void export(
            ExportMetricsServiceRequest request,
            StreamObserver<ExportMetricsServiceResponse> responseObserver) {

        synchronized (mutex) {
            List<ResourceMetrics> allResourceMetrics = request.getResourceMetricsList();
            for (ResourceMetrics resourceMetrics : allResourceMetrics) {
                Resource resource = resourceMetrics.getResource();
                String serviceName = "";
                for (KeyValue kv : resource.getAttributesList()) {
                    if (kv.getKey().compareTo("service.name") == 0) {
                        serviceName = kv.getValue().getStringValue();
                        System.out.println("Recv Metric --- Service Name: " + serviceName);
                    }
                }
                for (ScopeMetrics scoMetrics : resourceMetrics.getScopeMetricsList()) {
                    InstrumentationScope instrumentationScope = scoMetrics.getScope();
                    instrumentationScope.getAttributesList();
                    for (Metric metric : scoMetrics.getMetricsList()) {
                        System.out.println("-----------------");
                        System.out.println("Recv Metric --- Scope Name: " + metric.getName());
                        System.out.println("Recv Metric --- Scope Desc: " + metric.getDescription());
                        switch (metric.getDataCase()) {
                            case HISTOGRAM:
                                processHistogramMetrics(metric, serviceName);
                                break;
                            case SUM:
                            case GAUGE:
                            case SUMMARY:
                            default:
                                System.out.println("Skip Metric DataCase: " + metric.getDataCase());
                        }
                    }
                    System.out.println("");
                }
            }
        }
        responseObserver.onNext(ExportMetricsServiceResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private void processHistogramMetrics(Metric metric, String serviceName) {
        if (metric.getName().compareTo("gen_ai.client.token.usage") == 0) {
            processTokenMetric(metric, serviceName);
        } else if (metric.getName().compareTo("gen_ai.client.operation.duration") == 0) {
            processDurationMetric(metric, serviceName);
        }
    }

    private void processTokenMetric(Metric metric, String serviceName) {
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
                    System.out.println("Recv Metric --- Token Type: " + tokenType);
                }
            }
            if (!modelId.isEmpty()) {
                double tokenSum = dataPoint.getSum();
                long requestCount = dataPoint.getCount();
                long startTime = dataPoint.getStartTimeUnixNano();
                long endTime  = dataPoint.getTimeUnixNano();
                System.out.println("Recv Metric --- Token Sum: " + tokenSum);
                System.out.println("Recv Metric --- Request Count: " + requestCount);
                System.out.println("Recv Metric --- Start Time : " + startTime);
                System.out.println("Recv Metric --- End Time : " + endTime);

                String modelKey = String.format("%s:%s", serviceName, modelId);
                OtelMetric otelMetric = exportMetrics.get(modelKey);
                if (otelMetric == null ) {
                    otelMetric = new OtelMetric();
                    exportMetrics.put(modelKey, otelMetric);
                    otelMetric.setModelId(modelId);
                    otelMetric.setAiSystem(aiSystem);
                }
                
                if (tokenType.compareTo("input") == 0) {
                    long lastStartTime = otelMetric.getLastInputTokenStartTime();
                    if (startTime != lastStartTime) {
                        otelMetric.setLastInputTokenStartTime(startTime);
                        otelMetric.addDeltaInputTokens(tokenSum);
                    } else {
                        double lastInputTokenSum = otelMetric.getLastInputTokenSum();
                        otelMetric.addDeltaInputTokens(tokenSum - lastInputTokenSum);
                    }
                    otelMetric.setLastInputTokenSum(tokenSum);
                } else if (tokenType.compareTo("output") == 0) {
                    long lastStartTime = otelMetric.getLastOutputTokenStartTime();
                    if (startTime != lastStartTime) {
                        otelMetric.setLastOutputTokenStartTime(startTime);
                        otelMetric.addDeltaOutputTokens(tokenSum);
                    } else {
                        double lastOutputTokenSum = otelMetric.getLastOutputTokenSum();
                        otelMetric.addDeltaOutputTokens(tokenSum - lastOutputTokenSum);
                    }
                    otelMetric.setLastOutputTokenSum(tokenSum);
                }
            }
        }
    }

    private void processDurationMetric(Metric metric, String serviceName) {
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
                double durationSum = dataPoint.getSum();
                long requestCount = dataPoint.getCount();
                long startTime = dataPoint.getStartTimeUnixNano();
                long endTime  = dataPoint.getTimeUnixNano();
                System.out.println("Recv Metric --- Duration Sum: " + durationSum);
                System.out.println("Recv Metric --- Request Count: " + requestCount);
                System.out.println("Recv Metric --- Start Time : " + startTime);
                System.out.println("Recv Metric --- End Time : " + endTime);

                String modelKey = String.format("%s:%s", serviceName, modelId);
                OtelMetric otelMetric = exportMetrics.get(modelKey);
                if (otelMetric == null ) {
                    otelMetric = new OtelMetric();
                    exportMetrics.put(modelKey, otelMetric);
                    otelMetric.setModelId(modelId);
                    otelMetric.setAiSystem(aiSystem);
                }

                long lastStartTime = otelMetric.getLastDurationStartTime();
                double lastDurationSum = otelMetric.getLastDurationSum();
                long lastRequestCount = otelMetric.getLastRequestCount();
                if (startTime != lastStartTime) {
                    otelMetric.setLastDurationStartTime(startTime);
                    otelMetric.addDeltaDuration(durationSum);
                    otelMetric.addDeltaRequestCount(requestCount);
                } else {
                    otelMetric.addDeltaDuration(durationSum - lastDurationSum);
                    otelMetric.addDeltaRequestCount(requestCount - lastRequestCount);
                }
                otelMetric.setLastDurationSum(durationSum);
                otelMetric.setLastRequestCount(requestCount);
            }
        }
    }
}
