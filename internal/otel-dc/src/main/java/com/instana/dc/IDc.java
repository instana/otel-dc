package com.instana.dc;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;

import java.util.Map;

public interface IDc {
    void initOnce() throws Exception;
    void initDC() throws Exception;

    void registerMetrics(OpenTelemetry openTelemetry);

    void collectData();
    void start();

    RawMetric getRawMetric(String name);
    Map<String, RawMetric> getRawMetricsMap();
    Meter getDefaultMeter();
    void setDefaultMeter(Meter defaultMeter);
    String getDefaultInstrumentationScope();
    void setDefaultInstrumentationScope(String defaultInstrumentationScope);
    String getDefaultInstrumentationVersion();
    void setDefaultInstrumentationVersion(String defaultInstrumentationVersion);
}
