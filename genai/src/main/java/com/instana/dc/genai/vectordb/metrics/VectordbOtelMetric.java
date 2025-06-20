package com.instana.dc.genai.vectordb.metrics;

import com.instana.dc.genai.metrics.OtelMetric;
import java.util.HashMap;
import java.util.Map;

public class VectordbOtelMetric extends OtelMetric {
    private String dbSystem;
    private final Map<String, MetricValue> metrics;

    public VectordbOtelMetric() {
        super();
        this.metrics = new HashMap<>();
    }

    public String getDbSystem() {
        return this.dbSystem;
    }

    public void setDbSystem(String dbSystem) {
        this.dbSystem = dbSystem;
    }

    public Map<String, MetricValue> getMetrics() {
        return metrics;
    }

    public MetricValue getMetric(String name) {
        return metrics.computeIfAbsent(name, k -> new MetricValue());
    }

    public void setMetricSum(String name, double count) {
        getMetric(name).setSum(count);
    }

    public void setMetricStartTime(String name, long startTime) {
        getMetric(name).setStartTime(startTime);
    }

    public void addMetricDelta(String name, double delta) {
        getMetric(name).addDelta(delta);
    }

    public double getMetricSum(String name) {
        return getMetric(name).getSum();
    }

    public long getMetricStartTime(String name) {
        return getMetric(name).getStartTime();
    }

    @Override
    public void resetDeltaValues() {
        super.resetDeltaValues();
        metrics.values().forEach(MetricValue::resetDelta);
    }
}
