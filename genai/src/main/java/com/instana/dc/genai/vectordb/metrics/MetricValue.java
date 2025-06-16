package com.instana.dc.genai.vectordb.metrics;

public class MetricValue {
    private long count;
    private long startTime;
    private long delta;

    public MetricValue() {
        this.count = 0;
        this.startTime = 0;
        this.delta = 0;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getDelta() {
        return delta;
    }

    public void addDelta(long delta) {
        this.delta += delta;
    }

    public void resetDelta() {
        this.delta = 0;
    }
}
