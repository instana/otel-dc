/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.llm;

import com.instana.dc.AbstractDc;
import com.instana.dc.llm.DataCollector.CustomDcConfig;
import com.instana.dc.resources.ContainerResource;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.*;
import static com.instana.dc.llm.LLMDcUtil.*;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
public abstract class AbstractLLMDc extends AbstractDc {
    private static final Logger logger = Logger.getLogger(AbstractLLMDc.class.getName());

    private final String otelBackendUrl;
    private final boolean otelUsingHttp;
    private final int pollInterval;
    private final int callbackInterval;
    private final String serviceName;
	public final static String INSTRUMENTATION_SCOPE_PREFIX = "otelcol/llmmetricsreceiver/";
    private String serviceInstanceId;
    private CustomDcConfig cdcConfig;

    // Used the fixed 10 seconds for poll interval
    public static final int LLM_POLL_INTERVAL = 10;
    public static final int LLM_CLBK_INTERVAL = 10;

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

    public String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "UnknownName";
        }
    }

    public AbstractLLMDc(Map<String, Object> properties, CustomDcConfig cdcConfig) {
        super(new LLMRawMetricRegistry().getMap());
		// pollInterval = (Integer) properties.getOrDefault(POLLING_INTERVAL, DEFAULT_POLL_INTERVAL);
        // callbackInterval = (Integer) properties.getOrDefault(CALLBACK_INTERVAL, DEFAULT_CALLBACK_INTERVAL);
        pollInterval = LLM_POLL_INTERVAL;
        callbackInterval = LLM_CLBK_INTERVAL;
        otelBackendUrl = (String) properties.get(OTEL_BACKEND_URL);
        otelUsingHttp = (Boolean) properties.getOrDefault(OTEL_BACKEND_USING_HTTP, Boolean.FALSE);
        serviceName = (String) properties.get(OTEL_SERVICE_NAME);
        serviceInstanceId = "LLMONITOR:" + serviceName + "@" + getHostName();
        this.cdcConfig = cdcConfig;
    }

    @Override
    public Resource getResourceAttributes() {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                    stringKey(SERVICE_NAME), serviceName,
                    stringKey(SERVICE_INSTANCE_ID), serviceInstanceId
                )))
                .merge(Resource.create(Attributes.of(
                        stringKey("INSTANA_PLUGIN"), // com.instana.agent.sensorsdk.semconv.ResourceAttributes.INSTANA_PLUGIN
                        "llmonitor" // com.instana.agent.sensorsdk.semconv.ResourceAttributes.LLM
                )));

        resource = resource.merge(ContainerResource.get());
        return mergeResourceAttributesFromEnv(resource);
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(String serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    @Override
    public void initDC() throws Exception {
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
