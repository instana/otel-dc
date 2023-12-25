package com.instana.dc;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.resources.Resource;

import java.util.Map;

public interface IDc {
    void initOnce() throws Exception;
    Resource getResourceAttributes();
    void initDC() throws Exception;

    void initMeters(OpenTelemetry openTelemetry);
    void registerMetrics();

    void collectData();
    void start();

    RawMetric getRawMetric(String name);
    Map<String, RawMetric> getRawMetricsMap();
    Map<String, Meter> getMeters();
}
