/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.llm.impl.llm;

import com.instana.dc.llm.AbstractLLMDc;
import com.instana.dc.llm.DataCollector.CustomDcConfig;
import com.instana.dc.llm.impl.llm.MetricsCollectorService.OtelMetric;

import java.util.logging.Logger;
import java.util.*;

import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.server.healthcheck.HealthCheckService;

//import static com.instana.agent.sensorsdk.semconv.SemanticAttributes.*;
import static com.instana.dc.llm.LLMDcUtil.*;

@SuppressWarnings("null")
public class LLMDc extends AbstractLLMDc {
    private static final Logger logger = Logger.getLogger(LLMDc.class.getName());

    public static final String SENSOR_NAME = "com.instana.plugin.watsonx";
    private HashMap<String, ModelAggregation> modelAggrMap = new HashMap<>();
    private MetricsCollectorService metricsCollector = new MetricsCollectorService();
    private Double pricePromptTokens = 0.0;
    private Double priceCompleteTokens = 0.0;

    /**
     * The poll rate in the configuration, in seconds. In other words, the number of
     * seconds between calls to Watsonx.
     */

    private class ModelAggregation { 
        private final String modelId;
        private final String userId;
        private int deltaPromptTokens;
        private int deltaCompleteTokens;
        private int deltaDuration;
        private int deltaReqCount;
        private int maxDuration;
        private int lastTotalPromptTokens;
        private int lastTotalCompleteTokens;
        private int lastTotalDuration;
        private int lastTotalReqCount;
        
        public ModelAggregation(String modelId, String userId) {
            this.modelId = modelId;
            this.userId = userId;
        }
        public String getModelId() {
            return modelId;
        }
        public String getUserId() {
            return userId;
        }
        public void addDeltaPromptTokens(int currTokens) {
            if(currTokens == 0) {
                return;
            }
            int diffPromptTokens = 0;
            if(currTokens > lastTotalPromptTokens && lastTotalPromptTokens != 0) {
                diffPromptTokens = currTokens - lastTotalPromptTokens;
            }
            lastTotalPromptTokens = currTokens;
            deltaPromptTokens += diffPromptTokens;
        }

        public int getDeltaPromptTokens() {
            return deltaPromptTokens;
        }
        public void addDeltaCompleteTokens(int currTokens) {
            if(currTokens == 0) {
                return;
            }
            int diffCompleteTokens = 0;
            if(currTokens > lastTotalCompleteTokens && lastTotalCompleteTokens != 0) {
                diffCompleteTokens = currTokens - lastTotalCompleteTokens;
            }
            lastTotalCompleteTokens = currTokens;
            deltaCompleteTokens += diffCompleteTokens;
        }
        public int getDeltaCompleteTokens() {
            return deltaCompleteTokens;
        }
        public void addDeltaDuration(int currDuration) {
            if(currDuration == 0) {
                return;
            }
            int diffDuration = 0;
            if(currDuration > lastTotalDuration && lastTotalDuration != 0) {
                diffDuration = currDuration - lastTotalDuration;
            }
            lastTotalDuration = currDuration;
            deltaDuration += diffDuration;

            if(deltaDuration > maxDuration) {
               maxDuration = deltaDuration;
            }
        }
        public int getDeltaDuration() {
            return deltaDuration;
        }
        public int getMaxDuration() {
            return maxDuration;
        }
        public void addDeltaReqCount(int currCount) {
            if(currCount == 0) {
                return;
            }
            int diffReqCount = 0;
            if(currCount > lastTotalReqCount && lastTotalReqCount != 0) {
                diffReqCount = currCount - lastTotalReqCount;
            }
            lastTotalReqCount = currCount;
            deltaReqCount += diffReqCount;
        }
        public int getDeltaReqCount() {
            return deltaReqCount;
        }
        public void resetMetrics() {
            deltaPromptTokens = 0;
            deltaCompleteTokens = 0;
            deltaDuration = 0;
            maxDuration = 0;
            deltaReqCount = 0;
        }
    }

    public LLMDc(Map<String, Object> properties, CustomDcConfig cdcConfig) throws Exception {
        super(properties, cdcConfig);
        pricePromptTokens = (Double) properties.getOrDefault(PRICE_PROMPT_TOKES_PER_KILO, 0.03);
        priceCompleteTokens = (Double) properties.getOrDefault(PRICE_COMPLETE_TOKES_PER_KILO, 0.03);
    }

    @Override
    public void initOnce() throws ClassNotFoundException {
        var server = Server.builder()
                .http(8000)
                .service(
                        GrpcService.builder()
                                .addService(metricsCollector)
                                .build())
                .service(
                        "/",
                        (ctx, req) -> {
                            var requests = metricsCollector.getMetrics();
                            if (requests != null) {
                                return HttpResponse.of(
                                        HttpStatus.OK, MediaType.JSON, HttpData.wrap("OK".getBytes()));
                            } else {
                                return HttpResponse.of(
                                        HttpStatus.BAD_REQUEST, MediaType.JSON,
                                        HttpData.wrap("Bad Request".getBytes()));
                            }
                        })
                .service("/health", HealthCheckService.of())
                .build();

        server.start().join();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop().join()));
    }

    @Override
    public void registerMetrics() {
        super.registerMetrics();
    }

    @Override
    public void collectData() {
        logger.info("Start to collect metrics");
  
        for(Map.Entry<String,ModelAggregation> entry : modelAggrMap.entrySet()){
            ModelAggregation aggr = entry.getValue();
            aggr.resetMetrics();
        }

        List<OtelMetric> otelMetrics = metricsCollector.getMetrics();
        metricsCollector.clearMetrics();
        for (OtelMetric metric : otelMetrics) {
            try {
                String modelId = metric.getModelId();
                long promptTokens = metric.getPromtTokens();
                long completeTokens = metric.getCompleteTokens();
                double duration = metric.getDuration();
                long requestCount = metric.getReqCount();

                ModelAggregation modelAggr = modelAggrMap.get(modelId);
                if (modelAggr == null) {
                    modelAggr = new ModelAggregation(modelId, "llmUser");
                    modelAggrMap.put(modelId, modelAggr);
                }

                modelAggr.addDeltaPromptTokens((int)(promptTokens));
                modelAggr.addDeltaCompleteTokens((int)(completeTokens));
                modelAggr.addDeltaDuration((int)(duration*1000));
                modelAggr.addDeltaReqCount((int)(requestCount));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("-----------------------------------------");
        for(Map.Entry<String,ModelAggregation> entry : modelAggrMap.entrySet()){
            ModelAggregation aggr = entry.getValue();
            String modelId = aggr.getModelId();
            String userId = aggr.getUserId();
            int deltaRequestCount = aggr.getDeltaReqCount();
            int deltaDuration = aggr.getDeltaDuration();
            int deltaPromptTokens = aggr.getDeltaPromptTokens();
            int deltaCompleteTokens = aggr.getDeltaCompleteTokens();
            int maxDuration = aggr.getMaxDuration();

            int avgDuration = deltaDuration/(deltaRequestCount==0?1:deltaRequestCount);
            double intervalReqCount = (double)deltaRequestCount/LLM_POLL_INTERVAL;
            double intervalPromptTokens = (double)deltaPromptTokens/LLM_POLL_INTERVAL;
            double intervalCompleteTokens = (double)deltaCompleteTokens/LLM_POLL_INTERVAL;
            double intervalTotalTokens = intervalPromptTokens + intervalCompleteTokens;
            double intervalPromptCost = (intervalPromptTokens/1000) * pricePromptTokens;
            double intervalCompleteCost = (intervalCompleteTokens/1000) * priceCompleteTokens;
            double intervalTotalCost = intervalPromptCost + intervalCompleteCost;
            aggr.resetMetrics();

            System.out.println("ModelId         : " + modelId);
            System.out.println("UserId          : " + userId);
            System.out.println("AvgDuration     : " + avgDuration);
            System.out.println("MaxDuration     : " + maxDuration);
            System.out.println("IntervalTokens  : " + intervalTotalTokens);
            System.out.println("IntervalCost    : " + intervalTotalCost);
            System.out.println("IntervalRequest : " + intervalReqCount);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("model_id", modelId);
            attributes.put("user_id", userId);
            getRawMetric(LLM_STATUS_NAME).setValue(1);
            getRawMetric(LLM_DURATION_NAME).getDataPoint(modelId).setValue(avgDuration, attributes);
            getRawMetric(LLM_DURATION_MAX_NAME).getDataPoint(modelId).setValue(maxDuration, attributes);
            getRawMetric(LLM_COST_NAME).getDataPoint(modelId).setValue(intervalTotalCost, attributes);
            getRawMetric(LLM_TOKEN_NAME).getDataPoint(modelId).setValue(intervalTotalTokens, attributes);
            getRawMetric(LLM_REQ_COUNT_NAME).getDataPoint(modelId).setValue(intervalReqCount, attributes);
        }
        System.out.println("-----------------------------------------");
    }
}
