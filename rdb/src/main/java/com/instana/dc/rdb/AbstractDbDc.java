/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb;

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
import io.opentelemetry.semconv.SemanticAttributes;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.*;
import static com.instana.dc.rdb.DbDcUtil.*;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

public abstract class AbstractDbDc extends AbstractDc implements IDc {
    private static final Logger logger = Logger.getLogger(AbstractDbDc.class.getName());

    private final String dbSystem;
    private final String dbDriver;
    private String dbAddress;
    private long dbPort;
    private String dbConnUrl;
    private String dbUserName;
    private String dbPassword;
    private String dbName;
    private String dbVersion;
    private String dbEntityType;
    private String dbTenantId;
    private String dbTenantName;

    private final String otelBackendUrl;
    private final boolean otelUsingHttp;

    private final int pollInterval;
    private final int callbackInterval;
    private final String serviceName;
    private String serviceInstanceId;
    private String dbEntityParentId;

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

    public AbstractDbDc(Map<String, String> properties, String dbSystem, String dbDriver) {
        super(new DbRawMetricRegistry().getMap());

        this.dbSystem = dbSystem;
        this.dbDriver = dbDriver;

        String pollInt = properties.get(POLLING_INTERVAL);
        pollInterval = pollInt == null ? DEFAULT_POLL_INTERVAL : Integer.parseInt(pollInt);
        String callbackInt = properties.get(CALLBACK_INTERVAL);
        callbackInterval = callbackInt == null ? DEFAULT_CALLBACK_INTERVAL : Integer.parseInt(callbackInt);
        otelBackendUrl = properties.get(OTEL_BACKEND_URL);
        otelUsingHttp = "true".equalsIgnoreCase(properties.get(OTEL_BACKEND_USING_HTTP));

        serviceName = properties.get(OTEL_SERVICE_NAME);
        serviceInstanceId = properties.get(OTEL_SERVICE_INSTANCE_ID);
        dbEntityParentId = properties.get(DB_ENTITY_PARENT_ID);

        dbAddress = properties.get(DB_ADDRESS);
        dbPort = Long.parseLong(properties.get(DB_PORT));
        dbConnUrl = properties.get(DB_CONN_URL);
        dbUserName = properties.get(DB_USERNAME);
        dbPassword = properties.get(DB_PASSWORD);
        dbEntityType = properties.get(DB_ENTITY_TYPE);
        if (dbEntityType == null) {
            dbEntityType = DEFAULT_DB_ENTITY_TYPE;
        }
        dbEntityType = dbEntityType.toUpperCase();
        dbTenantId = properties.get(DB_TENANT_ID);
        dbTenantName = properties.get(DB_TENANT_NAME);
        dbName = properties.get(DB_NAME);
        dbVersion = properties.get(DB_VERSION);
    }

    @Override
    public Resource getResourceAttributes() {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName,
                        SemanticAttributes.DB_SYSTEM, dbSystem,
                        com.instana.agent.sensorsdk.semconv.ResourceAttributes.SERVER_ADDRESS, dbAddress,
                        com.instana.agent.sensorsdk.semconv.ResourceAttributes.SERVER_PORT, dbPort,
                        SemanticAttributes.DB_NAME, dbName,
                        com.instana.agent.sensorsdk.semconv.ResourceAttributes.DB_VERSION, dbVersion
                )))
                .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_INSTANCE_ID, serviceInstanceId,
                        com.instana.agent.sensorsdk.semconv.ResourceAttributes.DB_ENTITY_TYPE, dbEntityType,
                        stringKey(DcUtil.INSTANA_PLUGIN),
                        com.instana.agent.sensorsdk.semconv.ResourceAttributes.DATABASE
                )));

        try {
            resource = resource.merge(
                    Resource.create(Attributes.of(ResourceAttributes.HOST_NAME, InetAddress.getLocalHost().getHostName()))
            );
        } catch (UnknownHostException e) {
            // Ignore
        }

        String tenantName = this.getDbTenantName();
        if (tenantName != null) {
            resource = resource.merge(
                    Resource.create(Attributes.of(stringKey("tenant.name"), tenantName))
            );
        }

        String entityParentId = this.getDbEntityParentId();
        if (entityParentId != null) {
            resource = resource.merge(
                    Resource.create(Attributes.of(com.instana.agent.sensorsdk.semconv.ResourceAttributes.DB_ENTITY_PARENT_ID, entityParentId))
            );
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

    public String getDbDriver() {
        return dbDriver;
    }

    public String getDbAddress() {
        return dbAddress;
    }

    public void setDbAddress(String dbAddress) {
        this.dbAddress = dbAddress;
    }

    public long getDbPort() {
        return dbPort;
    }

    public void setDbPort(long dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbConnUrl() {
        return dbConnUrl;
    }

    public void setDbConnUrl(String dbConnUrl) {
        this.dbConnUrl = dbConnUrl;
    }

    public String getDbUserName() {
        return dbUserName;
    }

    public void setDbUserName(String dbUserName) {
        this.dbUserName = dbUserName;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbVersion() {
        return dbVersion;
    }

    public void setDbVersion(String dbVersion) {
        this.dbVersion = dbVersion;
    }

    public String getDbEntityType() {
        return dbEntityType;
    }

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(String serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public void setDbEntityType(String dbEntityType) {
        this.dbEntityType = dbEntityType;
    }

    public String getDbTenantId() {
        return dbTenantId;
    }

    public void setDbTenantId(String dbTenantId) {
        this.dbTenantId = dbTenantId;
    }

    public String getDbTenantName() {
        return dbTenantName;
    }

    public void setDbTenantName(String dbTenantName) {
        this.dbTenantName = dbTenantName;
    }

    public String getDbEntityParentId() {
        return dbEntityParentId;
    }

    public void setDbEntityParentId(String dbEntityParentId) {
        this.dbEntityParentId = dbEntityParentId;
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
    public void initOnce() throws ClassNotFoundException {
        Class.forName(getDbDriver());
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getDbConnUrl(), getDbUserName(), getDbPassword());
    }

    @Override
    public void start() {
        exec.scheduleWithFixedDelay(this::collectData, 1, pollInterval, TimeUnit.SECONDS);
    }
}
