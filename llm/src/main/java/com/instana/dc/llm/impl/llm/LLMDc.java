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
    private Double watsonxPricePromptTokens = 0.0;
    private Double watsonxPriceCompleteTokens = 0.0;
    private Double openaiPricePromptTokens = 0.0;
    private Double openaiPriceCompleteTokens = 0.0;
    private Double anthropicPricePromptTokens = 0.0;
    private Double anthropicPriceCompleteTokens = 0.0;
    private int listenPort = 0;

    /**
     * The poll rate in the configuration, in seconds. In other words, the number of
     * seconds between calls to Watsonx.
     */

    private class ModelAggregation { 
        private final String modelId;
        private final String aiSystem;
        private long deltaPromptTokens;
        private long deltaCompleteTokens;
        private long deltaDuration;
        private long deltaReqCount;
        private long maxDuration;
        private long lastTotalPromptTokens;
        private long lastTotalCompleteTokens;
        private long lastTotalDuration;
        private long lastTotalReqCount;
        
        public ModelAggregation(String modelId, String aiSystem) {
            this.modelId = modelId;
            this.aiSystem = aiSystem;
        }
        public String getModelId() {
            return modelId;
        }
        public String getAiSystem() {
            return aiSystem;
        }
        public void addDeltaPromptTokens(long currTokens, long reqCount) {
            if(currTokens == 0) {
                return;
            }
            long diffPromptTokens = 0;
            if(reqCount == 1) {
                diffPromptTokens = currTokens;
            } else if(currTokens > lastTotalPromptTokens && lastTotalPromptTokens != 0) {
                diffPromptTokens = currTokens - lastTotalPromptTokens;
            }
            lastTotalPromptTokens = currTokens;
            deltaPromptTokens += diffPromptTokens;
        }

        public long getDeltaPromptTokens() {
            return deltaPromptTokens;
        }
        public void addDeltaCompleteTokens(long currTokens, long reqCount) {
            if(currTokens == 0) {
                return;
            }
            long diffCompleteTokens = 0;
            if(reqCount == 1) {
                diffCompleteTokens = currTokens;
            } else if(currTokens > lastTotalCompleteTokens && lastTotalCompleteTokens != 0) {
                diffCompleteTokens = currTokens - lastTotalCompleteTokens;
            }
            lastTotalCompleteTokens = currTokens;
            deltaCompleteTokens += diffCompleteTokens;
        }
        public long getDeltaCompleteTokens() {
            return deltaCompleteTokens;
        }
        public void addDeltaDuration(long currDuration, long reqCount) {
            if(currDuration == 0) {
                return;
            }
            long diffDuration = 0;
            if(reqCount == 1) {
                diffDuration = currDuration;
            } else if(currDuration > lastTotalDuration && lastTotalDuration != 0) {
                diffDuration = currDuration - lastTotalDuration;
            }
            lastTotalDuration = currDuration;
            deltaDuration += diffDuration;
        }
        public long getDeltaDuration() {
            return deltaDuration;
        }
        public void setMaxDuration(long maxDuration) {
            this.maxDuration = maxDuration;
        }
        public long getMaxDuration() {
            return maxDuration;
        }
        public void addDeltaReqCount(long currCount) {
            if(currCount == 0) {
                return;
            }
            long diffReqCount = 0;
            if(currCount == 1) {
                diffReqCount = currCount;
            } else if(currCount > lastTotalReqCount && lastTotalReqCount != 0) {
                diffReqCount = currCount - lastTotalReqCount;
            }
            lastTotalReqCount = currCount;
            deltaReqCount += diffReqCount;
        }
        public long getDeltaReqCount() {
            return deltaReqCount;
        }
        public long getCurrentReqCount() {
            return lastTotalReqCount;
        }
        public void resetMetrics() {
            deltaPromptTokens = 0;
            deltaCompleteTokens = 0;
            deltaDuration = 0;
            deltaReqCount = 0;
        }
    }

    public LLMDc(Map<String, Object> properties, CustomDcConfig cdcConfig) throws Exception {
        super(properties, cdcConfig);
        logLLMSpecificConfig(properties);
        watsonxPricePromptTokens = (Double) properties.getOrDefault(WATSONX_PRICE_PROMPT_TOKES_PER_KILO, 0.0);
        watsonxPriceCompleteTokens = (Double) properties.getOrDefault(WATSONX_PRICE_COMPLETE_TOKES_PER_KILO, 0.0);
        openaiPricePromptTokens = (Double) properties.getOrDefault(OPENAI_PRICE_PROMPT_TOKES_PER_KILO, 0.0);
        openaiPriceCompleteTokens = (Double) properties.getOrDefault(OPENAI_PRICE_COMPLETE_TOKES_PER_KILO, 0.0);
        anthropicPricePromptTokens = (Double) properties.getOrDefault(ANTHROPIC_PRICE_PROMPT_TOKES_PER_KILO, 0.0);
        anthropicPriceCompleteTokens = (Double) properties.getOrDefault(ANTHROPIC_PRICE_COMPLETE_TOKES_PER_KILO, 0.0);
        listenPort = (int) properties.getOrDefault(SERVICE_LISTEN_PORT, 8000);
    }

    private void logLLMSpecificConfig(Map<String, Object> properties) {
        logger.info("LLM Specific Configuration:");
        logger.info("  OPENAI_PRICE_PROMPT_TOKES_PER_KILO: " + 
                    properties.getOrDefault(OPENAI_PRICE_PROMPT_TOKES_PER_KILO, "Not set"));
        logger.info("  OPENAI_PRICE_COMPLETE_TOKES_PER_KILO: " + 
                    properties.getOrDefault(OPENAI_PRICE_COMPLETE_TOKES_PER_KILO, "Not set"));
        // 他の関連する設定値もここでログ出力
        Double promptPrice = (Double) properties.getOrDefault("openai.price.prompt.tokens.per.kilo", 0.0);
        logger.info("Prompt price per kilo tokens: " + promptPrice);


    }

    @Override
    public void initOnce() throws ClassNotFoundException {
        var server = Server.builder()
                .http(listenPort)
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
        logger.info("Number of metrics received: " + otelMetrics.size());
        metricsCollector.clearMetrics();
        
        for (OtelMetric metric : otelMetrics) {
            try {
                String modelId = metric.getModelId();
                String aiSystem = metric.getAiSystem();
                long promptTokens = metric.getPromtTokens();
                long completeTokens = metric.getCompleteTokens();
                double duration = metric.getDuration();
                long requestCount = metric.getReqCount();
    
                logger.info("Processing metric - Model: " + modelId + ", AI System: " + aiSystem + 
                            ", Prompt Tokens: " + promptTokens + ", Complete Tokens: " + completeTokens +
                            ", Duration: " + duration + ", Request Count: " + requestCount);
    
                ModelAggregation modelAggr = modelAggrMap.computeIfAbsent(modelId, k -> new ModelAggregation(modelId, aiSystem));
                modelAggr.addDeltaDuration((long)(duration*1000), requestCount);
                modelAggr.addDeltaReqCount(requestCount);
                modelAggr.addDeltaPromptTokens(promptTokens, requestCount);
                modelAggr.addDeltaCompleteTokens(completeTokens, requestCount);
    
                logger.info("After aggregation - Delta Prompt Tokens: " + modelAggr.getDeltaPromptTokens() + 
                            ", Delta Complete Tokens: " + modelAggr.getDeltaCompleteTokens());
            } catch (Exception e) {
                logger.severe("Error processing metric: " + e.getMessage());
                e.printStackTrace();
            }
        }
    
        logger.info("-----------------------------------------");
        for(Map.Entry<String,ModelAggregation> entry : modelAggrMap.entrySet()){
            ModelAggregation aggr = entry.getValue();
            String modelId = aggr.getModelId();
            String aiSystem = aggr.getAiSystem();
            long deltaRequestCount = aggr.getDeltaReqCount();
            long deltaDuration = aggr.getDeltaDuration();
            long deltaPromptTokens = aggr.getDeltaPromptTokens();
            long deltaCompleteTokens = aggr.getDeltaCompleteTokens();
            long maxDuration = aggr.getMaxDuration();
    
            long avgDuration = deltaRequestCount == 0 ? 0 : deltaDuration / deltaRequestCount;
            if(avgDuration > maxDuration) {
                maxDuration = avgDuration;
                aggr.setMaxDuration(maxDuration);
            }
    
            int intervalSeconds = LLM_POLL_INTERVAL;
            String agentLess = System.getenv("AGENTLESS_MODE_ENABLED");
            if (agentLess != null) {
                intervalSeconds = 1;
            }
    
            double pricePromptTokens = getPricePromptTokens(aiSystem);
            double priceCompleteTokens = getPriceCompleteTokens(aiSystem);
    
            double intervalReqCount = (double)deltaRequestCount/intervalSeconds;
            double intervalPromptTokens = (double)deltaPromptTokens/intervalSeconds;
            double intervalCompleteTokens = (double)deltaCompleteTokens/intervalSeconds;
            double intervalTotalTokens = intervalPromptTokens + intervalCompleteTokens;
            double intervalPromptCost = (intervalPromptTokens/1000) * pricePromptTokens;
            double intervalCompleteCost = (intervalCompleteTokens/1000) * priceCompleteTokens;
            double intervalTotalCost = intervalPromptCost + intervalCompleteCost;
    
            logger.info("ModelId         : " + modelId);
            logger.info("AiSystem        : " + aiSystem);
            logger.info("AvgDuration     : " + avgDuration);
            logger.info("MaxDuration     : " + maxDuration);
            logger.info("IntervalTokens  : " + intervalTotalTokens);
            logger.info("IntervalCost    : " + intervalTotalCost);
            logger.info("IntervalRequest : " + intervalReqCount);
    
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("model_id", modelId);
            attributes.put("ai_system", aiSystem);
            getRawMetric(LLM_STATUS_NAME).setValue(1);
            getRawMetric(LLM_DURATION_NAME).getDataPoint(modelId).setValue(avgDuration, attributes);
            getRawMetric(LLM_DURATION_MAX_NAME).getDataPoint(modelId).setValue(maxDuration, attributes);
            getRawMetric(LLM_COST_NAME).getDataPoint(modelId).setValue(intervalTotalCost, attributes);
            getRawMetric(LLM_TOKEN_NAME).getDataPoint(modelId).setValue(intervalTotalTokens, attributes);
            getRawMetric(LLM_REQ_COUNT_NAME).getDataPoint(modelId).setValue(intervalReqCount, attributes);
    
            aggr.resetMetrics();
        }
        logger.info("-----------------------------------------");
    }


    private double getPricePromptTokens(String aiSystem) {
        switch (aiSystem) {
            case "watsonx": return watsonxPricePromptTokens;
            case "openai": return openaiPricePromptTokens;
            case "anthropic": return anthropicPricePromptTokens;
            default: return 0.0;
        }
    }
    
    private double getPriceCompleteTokens(String aiSystem) {
        switch (aiSystem) {
            case "watsonx": return watsonxPriceCompleteTokens;
            case "openai": return openaiPriceCompleteTokens;
            case "anthropic": return anthropicPriceCompleteTokens;
            default: return 0.0;
        }
    }
}
