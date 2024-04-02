package com.instana.dc.ai.impl.llm;

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


class MetricsCollectorService extends MetricsServiceGrpc.MetricsServiceImplBase {

    public class OtelMetric { 
        private String modelId;
        private long promptTokens;
        private long completeTokens;
        private double duration;
 
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
    }

    private final BlockingQueue<OtelMetric> exportMetrics = new LinkedBlockingDeque<>();

    public List<OtelMetric> getMetrics() {
        return ImmutableList.copyOf(exportMetrics);
    }

    public void clearMetrics() {
        exportMetrics.clear();
    }

    @Override
    public void export(
            ExportMetricsServiceRequest request,
            StreamObserver<ExportMetricsServiceResponse> responseObserver) {
 
        System.out.println("--------------------------------------------------------");

        List<ResourceMetrics> allResourceMetrics = request.getResourceMetricsList();
        for (ResourceMetrics resourceMetrics : allResourceMetrics) {

            OtelMetric otelMetric = new OtelMetric();
            Resource resource = resourceMetrics.getResource();
            for (KeyValue reskv : resource.getAttributesList()) {
                System.out.println("Received metric resouce --- attrKey: " + reskv.getKey());
                System.out.println("Received metric resouce --- attrVal: " + reskv.getValue().getStringValue());
            }

            for (ScopeMetrics scoMetrics : resourceMetrics.getScopeMetricsList()) {
                InstrumentationScope instrumentationScope = scoMetrics.getScope();
                instrumentationScope.getAttributesList();
                for (KeyValue inskv : instrumentationScope.getAttributesList()) {
                    System.out.println("Received metric scope --- attrKey: " + inskv.getKey());
                    System.out.println("Received metric scope --- attrVal: " + inskv.getValue().getStringValue());
                }

                for (Metric metric : scoMetrics.getMetricsList()) {
                    System.out.println("Received metric --- Name: " + metric.getName());
                    System.out.println("Received metric --- Desc: " + metric.getDescription());
                    System.out.println("Received metric --- Unit: " + metric.getUnit());
                    System.out.println("Received metric --- Case: " + metric.getDataCase().getNumber());

                    switch(metric.getDataCase()) {
                        case GAUGE:
                            List<NumberDataPoint> gaugeDataPoints = metric.getGauge().getDataPointsList();
                            for (NumberDataPoint dataPoint : gaugeDataPoints) {

                                List<KeyValue> kvList = dataPoint.getAttributesList();

                                for (KeyValue kv : kvList) {
                                    System.out.println("Received metric --- Gauge attrKey: " + kv.getKey());
                                    System.out.println("Received metric --- Gauge attrVal: " + kv.getValue().getStringValue());
                                }
                                switch(dataPoint.getValueCase()) {
                                    case AS_INT:
                                        System.out.println("Received metric --- Gauge Int Value: " + dataPoint.getAsInt());
                                        break;
                                    case AS_DOUBLE:
                                        System.out.println("Received metric --- Gauge Double Value: " + dataPoint.getAsDouble());
                                        break;
                                    default:
                                        System.out.println("Unsupported metric Gauge ValueCase: " + dataPoint.getValueCase());
                                }
                            }
                            break;
                        case SUM:
                            List<NumberDataPoint> sumDataPoints = metric.getSum().getDataPointsList();
                            for (NumberDataPoint dataPoint : sumDataPoints) {

                                List<KeyValue> kvList = dataPoint.getAttributesList();

                                String tokenType = "";
                                String modelId = "";
                                for (KeyValue kv : kvList) {
                                    System.out.println("Received metric --- Sum attrKey: " + kv.getKey());
                                    System.out.println("Received metric --- Sum attrVal: " + kv.getValue().getStringValue());
                                    if(kv.getKey().compareTo("llm.response.model") == 0) {
                                        modelId = kv.getValue().getStringValue();
                                        otelMetric.setModelId(modelId);
                                    } else if(kv.getKey().compareTo("llm.usage.token_type") == 0) {
                                        tokenType = kv.getValue().getStringValue();
                                    }
                                }
                                switch(dataPoint.getValueCase()) {
                                    case AS_INT:
                                        System.out.println("Received metric --- Sum Int Value: " + dataPoint.getAsInt());
                                       if(tokenType.compareTo("prompt") == 0) {
                                            otelMetric.setPromptTokens(dataPoint.getAsInt());  
                                        } else if(tokenType.compareTo("completion") == 0) {
                                            otelMetric.setCompleteTokens(dataPoint.getAsInt());  
                                        }
                                        break;
                                    case AS_DOUBLE:
                                        System.out.println("Received metric --- Sum Double Value: " + dataPoint.getAsDouble());
                                        break;
                                    default:
                                        System.out.println("Unsupported metric Sum ValueCase: " + dataPoint.getValueCase());
                                }
                            }
                            break;
                        case HISTOGRAM:
                            List<HistogramDataPoint> histDataPoints = metric.getHistogram().getDataPointsList();
                            for (HistogramDataPoint dataPoint : histDataPoints) {

                                List<KeyValue> kvList = dataPoint.getAttributesList();

                                for (KeyValue kv : kvList) {
                                    System.out.println("Received metric --- Histogram attrKey: " + kv.getKey());
                                    System.out.println("Received metric --- Histogram attrVal: " + kv.getValue().getStringValue());
                                }
                                System.out.println("Received metric --- Histogram Double Value: " + dataPoint.getSum());

                                if(metric.getName().compareTo("llm.watsonx.completions.duration") == 0) {
                                    otelMetric.setDuration(dataPoint.getSum());
                                }
                                
                            }
                            break;
                        case SUMMARY:
                        default:
                            System.out.println("Unsupported metric DataCase: " + metric.getDataCase());
                            throw new AssertionError("Unsupported metric DataCase: " + metric.getDataCase());
                    }
                }
            }
            exportMetrics.add(otelMetric);
        }
        
        responseObserver.onNext(ExportMetricsServiceResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }
}