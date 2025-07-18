package com.instana.dc.genai.llm;

import java.util.Map;
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
            String[] modelId = new String[1];
            String[] aiSystem = new String[1];
            String[] aiVendor = new String[1];
            String[] tokenType = new String[1];

            dataPoint.getAttributesList().forEach(attr -> {
                switch (attr.getKey()) {
                    case MODEL_ID_KEY:
                        modelId[0] = attr.getValue().getStringValue();
                        System.out.println("Recv Metric --- Model ID: " + modelId[0]);
                        break;
                    case AI_SYSTEM_KEY:
                        aiSystem[0] = attr.getValue().getStringValue();
                        System.out.println("Recv Metric --- AI System: " + aiSystem[0]);
                        break;
                    case AI_VENDOR_KEY:
                        aiVendor[0] = attr.getValue().getStringValue();
                        System.out.println("Recv Metric --- AI vendor: " + aiVendor[0]);
                        break;
                    case TOKEN_TYPE_KEY:
                        tokenType[0] = attr.getValue().getStringValue();
                        System.out.println("Recv Metric --- Token Type: " + tokenType[0]);
                        break;
                    default:
                        logger.log(Level.WARNING, "Invalid attribute key.");
                }
            });

            if (modelId[0] != null && aiVendor[0] != null) {
                modelId[0] = aiVendor[0] + "." + modelId[0];
            }

            if (modelId[0] == null) {
                return;
            }

            String modelKey = String.format("%s:%s:%s", serviceName, aiSystem[0], modelId[0]);
            LLMOtelMetric llmOtelMetric = getOrCreateMetric(llmMetrics, modelKey, serviceName);
            llmOtelMetric.setModelId(modelId[0]);
            llmOtelMetric.setAiSystem(aiSystem[0]);

            double metricSum = dataPoint.getSum();
            long requestCount = dataPoint.getCount();
            long startTime = dataPoint.getStartTimeUnixNano();
            long endTime = dataPoint.getTimeUnixNano();

            if (metric.getName().compareTo("gen_ai.client.token.usage") == 0) {
                processTokenMetric(llmOtelMetric, tokenType[0], startTime, endTime, metricSum, requestCount);
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
