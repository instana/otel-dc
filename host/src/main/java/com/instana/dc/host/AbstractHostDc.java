/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.host;

import com.instana.dc.AbstractDc;
import com.instana.dc.IDc;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.*;

public abstract class AbstractHostDc extends AbstractDc implements IDc {
    private static final Logger logger = Logger.getLogger(AbstractHostDc.class.getName());

    private final String otelBackendUrl;
    private final int pollInterval;
    private final int callbackInterval;
    private final String serviceName;
    public final static String INSTRUMENTATION_SCOPE_PREFIX = "otelcol/hostmetricsreceiver/";

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

    public AbstractHostDc(Map<String, String> properties, String hostSystem) {
        super(new HostRawMetricRegistry().getMap());

        String pollInt = properties.get(POLLING_INTERVAL);
        pollInterval = pollInt == null ? DEFAULT_POLL_INTERVAL : Integer.parseInt(pollInt);
        String callbackInt = properties.get(CALLBACK_INTERVAL);
        callbackInterval = callbackInt == null ? DEFAULT_CALLBACK_INTERVAL : Integer.parseInt(callbackInt);
        otelBackendUrl = properties.get(OTEL_BACKEND_URL);
        String svcName = properties.get(OTEL_SERVICE_NAME);
        serviceName = svcName == null ? hostSystem : svcName;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public void initDC() throws Exception {
        Resource resource = getResourceAttributes();
        SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder().setResource(resource)
                .registerMetricReader(PeriodicMetricReader.builder(OtlpGrpcMetricExporter.builder().setEndpoint(otelBackendUrl).setTimeout(10, TimeUnit.SECONDS).build()).setInterval(Duration.ofSeconds(callbackInterval)).build())
                .build();
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProvider).build();
        initMeters(openTelemetry);
        registerMetrics();
    }

    private void initMeter(OpenTelemetry openTelemetry, String name) {
        Meter meter1 = openTelemetry.meterBuilder(INSTRUMENTATION_SCOPE_PREFIX + name).setInstrumentationVersion("1.0.0").build();
        getMeters().put(name, meter1);
    }

    /* The purpose to overwrite this method is to comply with the "hostmetrics" receiver of
     * OpenTelemetry Contrib Collector.
     **/
    @Override
    public void initMeters(OpenTelemetry openTelemetry) {
        initMeter(openTelemetry, HostDcUtil.MeterName.CPU);
        initMeter(openTelemetry, HostDcUtil.MeterName.MEMORY);
        initMeter(openTelemetry, HostDcUtil.MeterName.NETWORK);
        initMeter(openTelemetry, HostDcUtil.MeterName.LOAD);
        initMeter(openTelemetry, HostDcUtil.MeterName.DISK);
        initMeter(openTelemetry, HostDcUtil.MeterName.FILESYSTEM);
        initMeter(openTelemetry, HostDcUtil.MeterName.PROCESSES);
        initMeter(openTelemetry, HostDcUtil.MeterName.PAGING);
    }


    @Override
    public void start() {
        exec.scheduleWithFixedDelay(this::collectData, 1, pollInterval, TimeUnit.SECONDS);
    }
}
