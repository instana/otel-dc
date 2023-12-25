package com.instana.dc;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;

import java.util.Map;

public abstract class AbstractDc implements IDc {
    private Meter defaultMeter;
    private final Map<String, RawMetric> rawMetricsMap;

    private String defaultInstrumentationScope = "instana.otel.sensor.sdk";
    private String defaultInstrumentationVersion = "1.0.0";


    public AbstractDc(Map<String, RawMetric> rawMetricsMap) {
        this.rawMetricsMap = rawMetricsMap;
    }

    @Override
    public void initOnce() throws Exception {
    }

    @Override
    public void registerMetrics(OpenTelemetry openTelemetry) {
        defaultMeter = openTelemetry.meterBuilder(defaultInstrumentationScope).setInstrumentationVersion(defaultInstrumentationVersion).build();
        for (RawMetric rawMetric : rawMetricsMap.values()) {
            DcUtil.registerMetric(defaultMeter, rawMetric);
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

    @Override
    public Meter getDefaultMeter() {
        return defaultMeter;
    }

    @Override
    public void setDefaultMeter(Meter defaultMeter) {
        this.defaultMeter = defaultMeter;
    }

    @Override
    public String getDefaultInstrumentationScope() {
        return defaultInstrumentationScope;
    }

    @Override
    public void setDefaultInstrumentationScope(String defaultInstrumentationScope) {
        this.defaultInstrumentationScope = defaultInstrumentationScope;
    }

    @Override
    public String getDefaultInstrumentationVersion() {
        return defaultInstrumentationVersion;
    }

    @Override
    public void setDefaultInstrumentationVersion(String defaultInstrumentationVersion) {
        this.defaultInstrumentationVersion = defaultInstrumentationVersion;
    }
}
