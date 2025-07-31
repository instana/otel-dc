package com.instana.dc.genai.llm;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.instana.dc.genai.llm.metrics.LLMOtelMetric;
import io.opentelemetry.proto.metrics.v1.HistogramDataPoint;
import io.opentelemetry.proto.metrics.v1.Metric;

public class LLMMetricProcessor {
    private static final Logger logger = Logger.getLogger(LLMMetricProcessor.class.getName());
    private static final String MODEL_ID_KEY = "gen_ai.response.model";
    private static final String AI_SYSTEM_KEY = "gen_ai.system";
    private static final String AI_VENDOR_KEY = "gen_ai.vendor";
    private static final String TOKEN_TYPE_KEY = "gen_ai.token.type";

    private LLMMetricProcessor(){ }

    public static void processLLMMetric(Metric metric, Map<String, LLMOtelMetric> llmMetrics, String serviceName) {
        for (HistogramDataPoint dataPoint : metric.getHistogram().getDataPointsList()) {
            AtomicReference<String> modelId = new AtomicReference<>();
            AtomicReference<String> aiSystem = new AtomicReference<>();
            AtomicReference<String> aiVendor = new AtomicReference<>();
            AtomicReference<String> tokenType = new AtomicReference<>();

            dataPoint.getAttributesList().forEach(attr -> {
                switch (attr.getKey()) {
                    case MODEL_ID_KEY:
                        modelId.set(attr.getValue().getStringValue());
                        System.out.println("Recv Metric --- Model ID: " + modelId.get());
                        break;
                    case AI_SYSTEM_KEY:
                        aiSystem.set(attr.getValue().getStringValue());
                        System.out.println("Recv Metric --- AI System: " + aiSystem.get());
                        break;
                    case AI_VENDOR_KEY:
                        aiVendor.set(attr.getValue().getStringValue());
                        System.out.println("Recv Metric --- AI vendor: " + aiVendor.get());
                        break;
                    case TOKEN_TYPE_KEY:
                        tokenType.set(attr.getValue().getStringValue());
                        System.out.println("Recv Metric --- Token Type: " + tokenType.get());
                        break;
                    default:
                        logger.log(Level.WARNING, "Invalid attribute key.");
                }
            });

            if (modelId.get() != null && aiVendor.get() != null) {
                modelId.set(aiVendor.get() + "." + modelId.get());
            }

            if (modelId.get() == null) {
                return;
            }

            String modelKey = String.format("%s:%s:%s", serviceName, aiSystem.get(), modelId.get());
            LLMOtelMetric llmOtelMetric = getOrCreateMetric(llmMetrics, modelKey, serviceName);
            llmOtelMetric.setModelId(modelId.get());
            llmOtelMetric.setAiSystem(aiSystem.get());

            double metricSum = dataPoint.getSum();
            long requestCount = dataPoint.getCount();
            long startTime = dataPoint.getStartTimeUnixNano();
            long endTime = dataPoint.getTimeUnixNano();

            if (metric.getName().compareTo("gen_ai.client.token.usage") == 0) {
                processTokenMetric(llmOtelMetric, tokenType.get(), startTime, endTime, metricSum, requestCount);
            } else if (metric.getName().compareTo("gen_ai.client.operation.duration") == 0) {
                processDurationMetric(llmOtelMetric, startTime, endTime, metricSum, requestCount);
            }
        }
    }

    private static LLMOtelMetric getOrCreateMetric(Map<String, LLMOtelMetric> llmMetrics, String modelKey, String serviceName) {
        return llmMetrics.computeIfAbsent(modelKey, k -> {
            LLMOtelMetric metric = new LLMOtelMetric();
            metric.setServiceName(serviceName);
            return metric;
        });
    }

    private static void processTokenMetric(LLMOtelMetric metric, String tokenType, long startTime, long endTime, double tokenSum, long requestCount) {
        System.out.println("Recv Metric --- Token Sum: " + tokenSum);
        System.out.println("Recv Metric --- Request Count: " + requestCount);
        System.out.println("Recv Metric --- Start Time : " + startTime);
        System.out.println("Recv Metric --- End Time : " + endTime);
        long lastStartTime = "input".equals(tokenType) ? metric.getLastInputTokenStartTime() : metric.getLastOutputTokenStartTime();
        double lastTokenSum = "input".equals(tokenType) ? metric.getLastInputTokenSum() : metric.getLastOutputTokenSum();

        if (startTime != lastStartTime) {
            if ("input".equals(tokenType)) {
                metric.setLastInputTokenStartTime(startTime);
                metric.addDeltaInputTokens(tokenSum);
            } else {
                metric.setLastOutputTokenStartTime(startTime);
                metric.addDeltaOutputTokens(tokenSum);
            }
        } else {
            if ("input".equals(tokenType)) {
                metric.addDeltaInputTokens(tokenSum - lastTokenSum);
            } else {
                metric.addDeltaOutputTokens(tokenSum - lastTokenSum);
            }
        }

        if ("input".equals(tokenType)) {
            metric.setLastInputTokenSum(tokenSum);
        } else {
            metric.setLastOutputTokenSum(tokenSum);
        }
    }

    private static void processDurationMetric(LLMOtelMetric metric, long startTime, long endTime, double durationSum, long requestCount) {
        System.out.println("Recv Metric --- Duration Sum: " + durationSum);
        System.out.println("Recv Metric --- Request Count: " + requestCount);
        System.out.println("Recv Metric --- Start Time : " + startTime);
        System.out.println("Recv Metric --- End Time : " + endTime);

        long lastStartTime = metric.getLastDurationStartTime();
        double lastDurationSum = metric.getLastDurationSum();
        long lastRequestCount = metric.getLastRequestCount();

        if (startTime != lastStartTime) {
            metric.setLastDurationStartTime(startTime);
            metric.addDeltaDuration(durationSum);
            metric.addDeltaRequestCount(requestCount);
        } else {
            metric.addDeltaDuration(durationSum - lastDurationSum);
            metric.addDeltaRequestCount(requestCount - lastRequestCount);
        }
        metric.setLastDurationSum(durationSum);
        metric.setLastRequestCount(requestCount);
    }
}
