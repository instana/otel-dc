package com.instana.dc;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractDc implements IDc {
    private final Map<String, Meter> meters = new ConcurrentHashMap<>();
    private final Map<String, RawMetric> rawMetricsMap;


    public AbstractDc(Map<String, RawMetric> rawMetricsMap) {
        this.rawMetricsMap = rawMetricsMap;
    }

    @Override
    public void initOnce() throws Exception {
    }

    @Override
    public Map<String, Meter> getMeters() {
        return meters;
    }

    @Override
    public void initMeters(OpenTelemetry openTelemetry){
        Meter defaultMeter = openTelemetry.meterBuilder("instana.otel.sensor.sdk").setInstrumentationVersion("1.0.0").build();
        meters.put(RawMetric.DEFAULT, defaultMeter);
    }

    @Override
    public void registerMetrics() {
        for (RawMetric rawMetric : rawMetricsMap.values()) {
            DcUtil.registerMetric(meters, rawMetric);
        }
    }

    @Override
    public RawMetric getRawMetric(String name) {
        return rawMetricsMap.get(name);
    }

    @Override
    public Map<String, RawMetric> getRawMetricsMap() {
        return rawMetricsMap;
    }

}
