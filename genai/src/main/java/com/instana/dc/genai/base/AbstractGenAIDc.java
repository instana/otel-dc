package com.instana.dc.genai.base;

import com.instana.dc.AbstractDc;
import com.instana.dc.genai.DataCollector.CustomDcConfig;
import com.instana.dc.genai.service.MetricsCollectorService;
import com.instana.dc.resources.ContainerResource;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.server.healthcheck.HealthCheckService;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.*;
import static com.instana.dc.genai.util.GenAIDcUtil.SERVICE_LISTEN_PORT;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

public abstract class AbstractGenAIDc extends AbstractDc {
    private static final Logger logger = Logger.getLogger(AbstractGenAIDc.class.getName());
    private static final int DEFAULT_CALLBACK_INTERVAL = 10;
    private static final int DEFAULT_SERVICE_LISTEN_PORT = 8000;
    private static final int INITIAL_DELAY_SECONDS = 1;

    protected final CustomDcConfig config;
    protected final String otelBackendUrl;
    protected final boolean otelUsingHttp;
    protected final int pollInterval;
    protected final int callbackInterval;
    protected final String serviceName;
    protected final String serviceInstanceId;
    protected final int listenPort;

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private final MetricsCollectorService metricsCollector = MetricsCollectorService.getInstance();
    private Server server;

    protected AbstractGenAIDc(Map<String, Object> properties, CustomDcConfig config) {
        super(Map.of());
        this.config = config;
        this.callbackInterval = (Integer) properties.getOrDefault(CALLBACK_INTERVAL, DEFAULT_CALLBACK_INTERVAL);
        this.pollInterval = (Integer) properties.getOrDefault(POLLING_INTERVAL, callbackInterval);
        this.otelBackendUrl = (String) properties.get(OTEL_BACKEND_URL);
        this.listenPort = (int) properties.getOrDefault(SERVICE_LISTEN_PORT, DEFAULT_SERVICE_LISTEN_PORT);
        this.otelUsingHttp = Boolean.parseBoolean(properties.getOrDefault(OTEL_BACKEND_USING_HTTP, "false").toString());
        this.serviceName = (String) properties.get(OTEL_SERVICE_NAME);
        this.serviceInstanceId = serviceName + "@" + getHostName();
    }

    @Override
    public void initOnce() {
        initializeServer();
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));
    }

    private void initializeServer() {
        server = Server.builder()
                .http(listenPort)
                .service(GrpcService.builder()
                        .addService(metricsCollector)
                        .build())
                .service("/", this::handleMetricsRequest)
                .service("/health", HealthCheckService.of())
                .build();

        server.start().join();
    }

    private HttpResponse handleMetricsRequest(com.linecorp.armeria.server.ServiceRequestContext ctx, com.linecorp.armeria.common.HttpRequest req) {
        var requests = metricsCollector.getDeltaMetrics();
        if (requests != null) {
            return HttpResponse.of(HttpStatus.OK, MediaType.JSON, HttpData.wrap("OK".getBytes()));
        }
        return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON, HttpData.wrap("Bad Request".getBytes()));
    }

    private void cleanup() {
        try {
            if (server != null) {
                server.stop().join();
            }
            exec.shutdown();
            if (!exec.awaitTermination(5, TimeUnit.SECONDS)) {
                exec.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Cleanup interrupted: " + e.getMessage());
        }
    }

    protected String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.warning("Failed to get hostname: " + e.getMessage());
            return "UnknownName";
        }
    }

    @Override
    public Resource getResourceAttributes() {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        stringKey("genai.platform"), getPlatformName(),
                        ResourceAttributes.SERVICE_NAME, serviceName,
                        ResourceAttributes.SERVICE_INSTANCE_ID, serviceInstanceId
                )))
                .merge(Resource.create(Attributes.of(
                        stringKey(INSTANA_PLUGIN), getPluginName()
                )));

        resource = resource.merge(ContainerResource.get());
        return mergeResourceAttributesFromEnv(resource);
    }

    @Override
    public void initDC() {
        Resource resource = getResourceAttributes();
        SdkMeterProvider sdkMeterProvider = getDefaultSdkMeterProvider(resource, otelBackendUrl, callbackInterval, otelUsingHttp, 10);
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProvider).build();
        initMeters(openTelemetry);
        registerMetrics();
    }

    @Override
    public void start() {
        exec.scheduleWithFixedDelay(this::collectData, INITIAL_DELAY_SECONDS, pollInterval, TimeUnit.SECONDS);
    }

    protected abstract String getPlatformName();

    protected abstract String getPluginName();
}
