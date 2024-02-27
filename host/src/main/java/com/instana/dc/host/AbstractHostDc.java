/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.host;

import com.instana.dc.AbstractDc;
import com.instana.dc.IDc;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.*;

public abstract class AbstractHostDc extends AbstractDc implements IDc {
    private static final Logger logger = Logger.getLogger(AbstractHostDc.class.getName());


    private final String otelBackendUrl;
    private final boolean otelUsingHttp;
    private final int pollInterval;
    private final int callbackInterval;
    private final String serviceName;
    public final static String INSTRUMENTATION_SCOPE_PREFIX = "otelcol/hostmetricsreceiver/";

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

    public ScheduledExecutorService getExec() {
        return exec;
    }

    public AbstractHostDc(Map<String, Object> properties, String hostSystem) {
        super(new HostRawMetricRegistry().getMap());
        pollInterval = (Integer) properties.getOrDefault(POLLING_INTERVAL, DEFAULT_POLL_INTERVAL);
        callbackInterval = (Integer) properties.getOrDefault(CALLBACK_INTERVAL, DEFAULT_CALLBACK_INTERVAL);
        otelBackendUrl = (String) properties.get(OTEL_BACKEND_URL);
        otelUsingHttp = (Boolean) properties.getOrDefault(OTEL_BACKEND_USING_HTTP, Boolean.FALSE);
        serviceName = (String) properties.get(OTEL_SERVICE_NAME);
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getPollInterval() {
        return pollInterval;
    }

    @Override
    public void initDC() throws Exception {
        Resource resource = getResourceAttributes();
        SdkMeterProvider sdkMeterProvider = this.getDefaultSdkMeterProvider(resource, otelBackendUrl, callbackInterval, otelUsingHttp, 10);
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProvider).build();
        initMeters(openTelemetry);
        registerMetrics();
    }

    protected void initMeter(OpenTelemetry openTelemetry, String name) {
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
