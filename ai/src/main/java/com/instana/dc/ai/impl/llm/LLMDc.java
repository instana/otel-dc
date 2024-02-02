/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.ai.impl.llm;

import com.instana.dc.ai.AbstractLLMDc;
import com.instana.dc.ai.DataCollector.CustomDcConfig;

import java.util.logging.Logger;
import java.util.*;
import java.io.IOException;

import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.MetricsData;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import io.opentelemetry.proto.metrics.v1.Sum;
import io.opentelemetry.proto.metrics.v1.Metric;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

//import static com.instana.agent.sensorsdk.semconv.SemanticAttributes.*;
import static com.instana.dc.ai.LLMDcUtil.*;

public class LLMDc extends AbstractLLMDc {
    private static final Logger logger = Logger.getLogger(LLMDc.class.getName());

    public static final String SENSOR_NAME = "com.instana.plugin.watsonx";
    private static List<byte[]> payloadList = new ArrayList<>();
    private HashMap<String, ModelAggregation> modelAggrMap = new HashMap<>();

    /**
     * The poll rate in the configuration, in seconds. In other words, the number of
     * seconds between calls to Watsonx.
     */
    
    private class ModelAggregation { 
        private final String modelId;
        private final String userId;
        private int totalTokens;
        private double totalCost;
        private int totalDuration;
        private int maxDuration;
        private int totalReqCount;
    
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
        public void addTotalTokens(int tokens) {
            totalTokens += tokens;
        }
        public int getTotalTokens() {
            return totalTokens;
        }
        public void addTotalCost(double cost) {
            totalCost += cost;
        }
        public double getTotalCost() {
            return totalCost;
        }
        public void addTotalDuration(int duration) {
            totalDuration += duration;
            if(duration > maxDuration)
               maxDuration = duration;
        }
        public int getTotalDuration() {
            return totalDuration;
        }
        public int getMaxDuration() {
            return maxDuration;
        }
        public void addReqCount(int count) {
            totalReqCount += count;
        }
        public int getReqCount() {
            return totalReqCount;
        }
        public void resetMetrics() {
            totalTokens = 0;
            totalCost = 0;
            totalDuration = 0;
            maxDuration = 0;
            totalReqCount = 0;
        }
    }

    static class PostHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream requestBody = exchange.getRequestBody();
                byte[] payload = requestBody.readAllBytes();
                payloadList.add(payload);

                String response = "OK";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                // os.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    public LLMDc(Map<String, Object> properties, CustomDcConfig cdcConfig) throws Exception {
        super(properties, cdcConfig);
    }

    @Override
    public void initOnce() throws ClassNotFoundException {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/v1/metrics", new PostHandler());
            server.setExecutor(null);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("Server is running on port 8000");        
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

        for (byte[] payload : payloadList) {
            try {
                MetricsData md = MetricsData.parseFrom(payload);

                List<ResourceMetrics> rmList = md.getResourceMetricsList();
                for ( ResourceMetrics rm : rmList ) {
                    List<ScopeMetrics> smList = rm.getScopeMetricsList();
                    for ( ScopeMetrics sm : smList ) {
                        List<Metric> mList = sm.getMetricsList();
                        for ( Metric m : mList ) {
                            String platform = "";
                            String modelId = "";
                            String userId = "llmUser";
                            long asInt = 0;

                            String name = m.getName();
                            Sum s = m.getSum();
                            List<NumberDataPoint> ndpList = s.getDataPointsList();
                            for ( NumberDataPoint ndp : ndpList ) {
                                List<KeyValue> kvList = ndp.getAttributesList();
                                for ( KeyValue kv : kvList ) {
                                    String key = kv.getKey();
                                    String val = kv.getValue().getStringValue();
                                    if (key.compareTo("model_id") == 0) {
                                        modelId = val;
                                    } else if (key.compareTo("llm_platform") == 0) {
                                        platform = val;
                                    }
                                }
                                asInt = ndp.getAsInt();
                            }
            
                            ModelAggregation modelAggr = modelAggrMap.get(modelId);
                            if(modelAggr == null) {
                                modelAggr = new ModelAggregation(modelId, userId);
                                modelAggrMap.put(modelId, modelAggr);
                            }

                            if (name.compareTo("llm.usage.total_tokens") == 0) {
                                modelAggr.addTotalTokens((int)asInt);
                            } else if (name.compareTo("llm.request.count") == 0) {
                                modelAggr.addReqCount((int)asInt);
                            } else if (name.compareTo("llm.response.duration") == 0) {
                                long ms = (long)asInt/1000000;
                                modelAggr.addTotalDuration((int)ms);
                            }
                        }
                    }
                }               
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
        payloadList.clear();

        getRawMetric(LLM_STATUS_NAME).setValue(1);
        for(Map.Entry<String,ModelAggregation> entry : modelAggrMap.entrySet()){
            ModelAggregation aggr = entry.getValue();
            int requestCount = aggr.getReqCount();
            int totalDuration = aggr.getTotalDuration();
            int maxDuration = aggr.getMaxDuration();
            int totalTokens = aggr.getTotalTokens();

            int avgDuration = totalDuration/(requestCount==0?1:requestCount);
            double intervalTokens = (double)totalTokens/LLM_POLL_INTERVAL;
            double intervalCost = intervalTokens * 0.01;
            double intervalRequests = (double)requestCount/LLM_POLL_INTERVAL;
            aggr.resetMetrics();

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("model_id", aggr.getModelId());
            attributes.put("user_id", aggr.getUserId());
            getRawMetric(LLM_DURATION_NAME).setValue(avgDuration, attributes);
            getRawMetric(LLM_DURATION_MAX_NAME).setValue(maxDuration, attributes);
            getRawMetric(LLM_COST_NAME).setValue(intervalCost, attributes);
            getRawMetric(LLM_TOKEN_NAME).setValue(intervalTokens, attributes);
            getRawMetric(LLM_REQ_COUNT_NAME).setValue(intervalRequests, attributes);

            logger.info("LLM_DURATION_NAME: " + avgDuration);
            logger.info("LLM_DURATION_MAX_NAME: " + maxDuration);
            logger.info("LLM_COST_NAME: " + intervalCost);
            logger.info("LLM_TOKEN_NAME: " + intervalTokens);
            logger.info("LLM_REQ_COUNT_NAME: " + intervalRequests);
        }
    }
}
