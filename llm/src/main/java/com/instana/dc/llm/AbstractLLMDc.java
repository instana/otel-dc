/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.llm;

import static com.instana.dc.DcUtil.CALLBACK_INTERVAL;
import static com.instana.dc.DcUtil.INSTANA_PLUGIN;
import static com.instana.dc.DcUtil.OTEL_BACKEND_URL;
import static com.instana.dc.DcUtil.OTEL_BACKEND_USING_HTTP;
import static com.instana.dc.DcUtil.OTEL_SERVICE_NAME;
import static com.instana.dc.DcUtil.POLLING_INTERVAL;
import static com.instana.dc.DcUtil.mergeResourceAttributesFromEnv;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.instana.dc.AbstractDc;
import com.instana.dc.llm.DataCollector.CustomDcConfig;
import com.instana.dc.resources.ContainerResource;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
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

    public static final int DEFAULT_LLM_POLL_INTERVAL = 10;
    public static final int DEFAULT_LLM_CLBK_INTERVAL = 10;

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
        callbackInterval = (Integer) properties.getOrDefault(CALLBACK_INTERVAL, DEFAULT_LLM_CLBK_INTERVAL);
        pollInterval = (Integer) properties.getOrDefault(POLLING_INTERVAL, callbackInterval);
        otelBackendUrl = (String) properties.get(OTEL_BACKEND_URL);
        otelUsingHttp = (Boolean) properties.getOrDefault(OTEL_BACKEND_USING_HTTP, Boolean.FALSE);
        serviceName = (String) properties.get(OTEL_SERVICE_NAME);
        serviceInstanceId = serviceName + "@" + getHostName();
        this.cdcConfig = cdcConfig;
    }

    @Override
    public Resource getResourceAttributes() {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                    stringKey("llm.platform"), "LLM",
                    ResourceAttributes.SERVICE_NAME, serviceName,
                    ResourceAttributes.SERVICE_INSTANCE_ID, serviceInstanceId
                )))
                .merge(Resource.create(Attributes.of(
                    stringKey(INSTANA_PLUGIN), "llmonitor"
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
