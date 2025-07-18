package com.instana.dc.genai.metrics;

public abstract class OtelMetric {
    protected String serviceName;
    protected double durationSum;
    protected long durationStartTime;
    protected long deltaDuration;
    protected long durationCount;

    protected OtelMetric() {}

    protected OtelMetric(OtelMetric other) {
        this.serviceName = other.serviceName;
        this.durationSum = other.durationSum;
        this.durationStartTime = other.durationStartTime;
        this.deltaDuration = other.deltaDuration;
        this.durationCount = other.durationCount;
    }

    public String getServiceName() {
        return serviceName;
    }

    public double getLastDurationSum() {
        return durationSum;
    }

    public long getLastDurationStartTime() {
        return durationStartTime;
    }

    public long getDeltaDuration() {
        return deltaDuration;
    }

    public long getDurationCount() {
        return durationCount;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setLastDurationSum(double durationSum) {
        this.durationSum = durationSum;
    }

    public void setLastDurationStartTime(long startTime) {
        this.durationStartTime = startTime;
    }

    public void addDeltaDuration(double deltaDuration) {
        this.deltaDuration += (long)(deltaDuration*1000); // seconds to milliseconds
    }

    public void incrementDurationCount() {
        this.durationCount++;
    }

    public void resetDeltaValues() {
        this.deltaDuration = 0;
    }
} 