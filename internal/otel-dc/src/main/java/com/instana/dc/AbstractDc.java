package com.instana.dc;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporterBuilder;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporterBuilder;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.instana.dc.DcUtil.getCert;

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

    public static OtlpGrpcMetricExporter createOtlpGrpcMetricExporter(String otelBackendUrl, long timeout, Map<String, String> headers, byte[] cert) {
        OtlpGrpcMetricExporterBuilder builder = OtlpGrpcMetricExporter.builder()
                .setEndpoint(otelBackendUrl)
                .setTimeout(timeout, TimeUnit.SECONDS);

        if (headers != null) {
            builder.setHeaders(() -> headers);
        }
        if (cert != null) {
            builder.setTrustedCertificates(cert);
        }

        return builder.build();
    }

    public static OtlpHttpMetricExporter createOtlpHttpMetricExporter(String otelBackendUrl, long timeout, Map<String, String> headers, byte[] cert) {
        OtlpHttpMetricExporterBuilder builder = OtlpHttpMetricExporter.builder()
                .setEndpoint(otelBackendUrl)
                .setTimeout(timeout, TimeUnit.SECONDS);

        if (headers != null) {
            builder.setHeaders(() -> headers);
        }
        if (cert != null) {
            builder.setTrustedCertificates(cert);
        }
        return builder.build();
    }

    @Override
    public SdkMeterProvider getDefaultSdkMeterProvider(Resource resource, String otelBackendUrl, long callbackInterval, boolean usingHTTP, long timeout) {
        Map<String, String> headers = DcUtil.getHeadersFromEnv();
        byte[] cert = getCert();

        if (!usingHTTP)
            return SdkMeterProvider.builder().setResource(resource)
                    .registerMetricReader(PeriodicMetricReader.builder(
                                    createOtlpGrpcMetricExporter(otelBackendUrl, timeout, headers, cert))
                            .setInterval(Duration.ofSeconds(callbackInterval)).build())
                    .build();
        return SdkMeterProvider.builder().setResource(resource)
                .registerMetricReader(PeriodicMetricReader.builder(
                                createOtlpHttpMetricExporter(otelBackendUrl, timeout, headers, cert))
                        .setInterval(Duration.ofSeconds(callbackInterval)).build())
                .build();
    }
}
