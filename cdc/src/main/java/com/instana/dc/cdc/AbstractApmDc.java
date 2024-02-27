/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.cdc;

import com.instana.dc.AbstractDc;
import com.instana.dc.DcUtil;
import com.instana.dc.IDc;
import com.instana.dc.resources.ContainerResource;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.*;
import static com.instana.dc.cdc.ApmDcUtil.CDC_APM;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

public abstract class AbstractApmDc extends AbstractDc implements IDc {
    private static final Logger logger = Logger.getLogger(AbstractApmDc.class.getName());
    private final String otelBackendUrl;
    private final boolean otelUsingHttp;
    private final int pollInterval;
    private final int callbackInterval;
    private final String serviceName;
    private String serviceInstanceId;

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

    public AbstractApmDc(Environment env) {
        super(new ApmRawMetricRegistry().getMap());
        String pollInt = env.getProperty(POLLING_INTERVAL);
        pollInterval = pollInt == null ? DEFAULT_POLL_INTERVAL : Integer.parseInt(pollInt);
        String callbackInt = env.getProperty(CALLBACK_INTERVAL);
        callbackInterval = callbackInt == null ? DEFAULT_CALLBACK_INTERVAL : Integer.parseInt(callbackInt);
        otelBackendUrl = env.getProperty(OTEL_BACKEND_URL, DEFAULT_OTEL_BACKEND_URL);
        otelUsingHttp = env.getProperty(OTEL_BACKEND_USING_HTTP, Boolean.class, false);
        serviceName = env.getProperty(OTEL_SERVICE_NAME, DEFAULT_OTEL_SERVICE_NAME);
        serviceInstanceId = env.getProperty(OTEL_SERVICE_INSTANCE_ID, DEFAULT_OTEL_SERVICE_INSTANCE_ID);
    }

    @Override
    public Resource getResourceAttributes() {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName
                )))
                .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_INSTANCE_ID, serviceInstanceId,
                        stringKey(INSTANA_PLUGIN), CDC_APM
                )));

        try {
            resource = resource.merge(
                    Resource.create(Attributes.of(ResourceAttributes.HOST_NAME, InetAddress.getLocalHost().getHostName()))
            );
        } catch (UnknownHostException e) {
            // Ignore
        }

        long pid = DcUtil.getPid();
        if (pid >= 0) {
            resource = resource.merge(
                    Resource.create(Attributes.of(ResourceAttributes.PROCESS_PID, pid))
            );
        }

        resource = resource.merge(ContainerResource.get());
        return mergeResourceAttributesFromEnv(resource);
    }


    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(String serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    @Override
    public void initDC() {
        Resource resource = getResourceAttributes();
        SdkMeterProvider sdkMeterProvider = this.getDefaultSdkMeterProvider(resource, otelBackendUrl, callbackInterval, otelUsingHttp, 10);
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProvider).build();
        initMeters(openTelemetry);
        registerMetrics();
    }

    @Override
    public void start() {
        exec.scheduleWithFixedDelay(this::collectData, 1, pollInterval, TimeUnit.SECONDS);
    }
}
