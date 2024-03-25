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

import static com.instana.dc.DcUtil.*;
import static com.instana.dc.rdb.DbDcUtil.*;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

public abstract class AbstractDbDc extends AbstractDc implements IDc {

    private final String dbSystem;
    private final String dbDriver;
    private String dbAddress;
    private int dbPort;
    private String dbConnUrl;
    private String dbUserName;
    private String dbPassword;
    private String dbName;
    private String serverName;
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
    private Map<String, Object> sysdatabases;

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

    protected AbstractDbDc(Map<String, Object> properties, String dbSystem, String dbDriver) {
        super(new DbRawMetricRegistry().getMap());

        this.dbSystem = dbSystem;
        this.dbDriver = dbDriver;

        pollInterval = (Integer) properties.getOrDefault(POLLING_INTERVAL, DEFAULT_POLL_INTERVAL);
        callbackInterval = (Integer) properties.getOrDefault(CALLBACK_INTERVAL, DEFAULT_CALLBACK_INTERVAL);
        otelBackendUrl = (String) properties.get(OTEL_BACKEND_URL);
        otelUsingHttp = (Boolean) properties.getOrDefault(OTEL_BACKEND_USING_HTTP, Boolean.FALSE);

        serviceName = (String) properties.get(OTEL_SERVICE_NAME);
        serviceInstanceId = (String) properties.get(OTEL_SERVICE_INSTANCE_ID);
        dbEntityParentId = (String) properties.get(DB_ENTITY_PARENT_ID);

        dbAddress = (String) properties.get(DB_ADDRESS);
        dbPort = (Integer) properties.getOrDefault(DB_PORT, 0);
        dbConnUrl = (String) properties.get(DB_CONN_URL);
        dbUserName = (String) properties.get(DB_USERNAME);
        dbPassword = (String) properties.get(DB_PASSWORD);
        serverName = (String) properties.get(DB_SERVER_NAME);
        dbEntityType = (String) properties.get(DB_ENTITY_TYPE);
        if (dbEntityType == null) {
            dbEntityType = DEFAULT_DB_ENTITY_TYPE;
        }
        dbEntityType = dbEntityType.toUpperCase();
        dbTenantId = (String) properties.get(DB_TENANT_ID);
        dbTenantName = (String) properties.get(DB_TENANT_NAME);
        dbName = (String) properties.get(DB_NAME);
        dbVersion = (String) properties.get(DB_VERSION);
    }
    
    @Override
    public Resource getResourceAttributes() {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName,
                        SemanticAttributes.DB_SYSTEM, dbSystem,
                        com.instana.agent.sensorsdk.semconv.ResourceAttributes.SERVER_ADDRESS, dbAddress,
                        com.instana.agent.sensorsdk.semconv.ResourceAttributes.SERVER_PORT, (long)dbPort,
                        SemanticAttributes.DB_NAME, dbName,
                        com.instana.agent.sensorsdk.semconv.ResourceAttributes.DB_VERSION, dbVersion
                )))
                .merge(Resource.create(convertMapToAttributes(getSysdatabases())))
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

    public void setDbPort(int dbPort) {
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
    public void setDbEntityParentId(String dbEntityParentId) {
        this.dbEntityParentId = dbEntityParentId;
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

    public String getServerName() {
        return serverName;
    }

    public Map<String, Object> getSysdatabases() {
        return sysdatabases;
    }

    public void setSysdatabases(Map<String, Object> sysdatabases) {
        this.sysdatabases = sysdatabases;
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
