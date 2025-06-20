package com.instana.dc.genai.vectordb.metrics;

public class MetricValue {
    private double sum;
    private long startTime;
    private double delta;

    public MetricValue() {
        this.sum = 0;
        this.startTime = 0;
        this.delta = 0;
    }

    public double getSum() {
        return sum;
    }

    public void setSum(double sum) {
        this.sum = sum;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public double getDelta() {
        return delta;
    }

    public void addDelta(double delta) {
        this.delta += delta;
    }

    public void resetDelta() {
        this.delta = 0;
    }
}
