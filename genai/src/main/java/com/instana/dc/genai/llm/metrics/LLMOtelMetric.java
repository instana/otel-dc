package com.instana.dc.genai.llm.metrics;

import com.instana.dc.genai.metrics.OtelMetric;

public class LLMOtelMetric extends OtelMetric {
    private String modelId;
    private double inputTokenSum;
    private double outputTokenSum;
    private long requestCount;
    private String aiSystem;
    private long inputTokenStartTime;
    private long outputTokenStartTime;
    private long deltaInputTokens;
    private long deltaOutputTokens;
    private long deltaRequestCount;

    public LLMOtelMetric() {
        super();
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

    public long getLastRequestCount() {
        return requestCount;
    }

    public long getDeltaInputTokens() {
        return deltaInputTokens;
    }

    public long getDeltaOutputTokens() {
        return deltaOutputTokens;
    }

    public long getDeltaRequestCount() {
        return deltaRequestCount;
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

    public void setLastRequestCount(long requestCount) {
        this.requestCount = requestCount;
    }

    public void addDeltaInputTokens(double deltaInputTokens) {
        this.deltaInputTokens += (long)deltaInputTokens;
    }

    public void addDeltaOutputTokens(double deltaOutputTokens) {
        this.deltaOutputTokens += (long)deltaOutputTokens;
    }

    public void addDeltaRequestCount(long deltaRequestCount) {
        this.deltaRequestCount += deltaRequestCount;
    }

    public void setLastInputTokenStartTime(long startTime) {
        this.inputTokenStartTime = startTime;
    }

    public void setLastOutputTokenStartTime(long startTime) {
        this.outputTokenStartTime = startTime;
    }

    @Override
    public void resetDeltaValues() {
        super.resetDeltaValues();
        this.deltaInputTokens = 0;
        this.deltaOutputTokens = 0;
        this.deltaRequestCount = 0;
    }
}
