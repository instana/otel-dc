package com.instana.dc;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
    public void initMeters(OpenTelemetry openTelemetry) {
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

    @Override
    public SdkMeterProvider getDefaultSdkMeterProvider(Resource resource, String otelBackendUrl, long callbackInterval, boolean usingHTTP, long timeout) {
        Map<String, String> map = DcUtil.getHeaderFromEnv();
        if (!usingHTTP)
            return SdkMeterProvider.builder().setResource(resource)
                    .registerMetricReader(PeriodicMetricReader.builder(
                                    OtlpGrpcMetricExporter.builder()
                                            .setEndpoint(otelBackendUrl)
                                            .setTimeout(timeout, TimeUnit.SECONDS)
                                            .setHeaders(() -> map)
                                            .setRetryPolicy(RetryPolicy.builder().setMaxAttempts(5).build())
                                            .build())
                            .setInterval(Duration.ofSeconds(callbackInterval)).build())
                    .build();
        return SdkMeterProvider.builder().setResource(resource)
                .registerMetricReader(PeriodicMetricReader.builder(
                                OtlpHttpMetricExporter.builder()
                                        .setEndpoint(otelBackendUrl)
                                        .setTimeout(timeout, TimeUnit.SECONDS)
                                        .setHeaders(() -> map)
                                        .setRetryPolicy(RetryPolicy.builder().setMaxAttempts(5).build())
                                        .build())
                        .setInterval(Duration.ofSeconds(callbackInterval)).build())
                .build();
    }
}
