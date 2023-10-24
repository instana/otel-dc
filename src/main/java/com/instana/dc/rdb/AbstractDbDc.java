/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb;

import com.instana.dc.DcUtil;
import com.instana.dc.RawMetric;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.OTEL_SERVICE_INSTANCE_ID;
import static com.instana.dc.DcUtil.mergeResourceAttributesFromEnv;
import static com.instana.dc.rdb.DbDcUtil.*;

public abstract class AbstractDbDc implements IDbDc {
    private static final Logger logger = Logger.getLogger(AbstractDbDc.class.getName());

    private String dbDriver;
    private String dbAddress;
    private long dbPort;
    private String dbConnUrl;
    private String dbUserName;
    private String dbPassword;
    private String dbName;
    private String dbVersion;
    private String dbEntityType;
    private String serviceInstanceId;

    private Meter meter;
    private final Map<String, RawMetric> rawMetricsMap = new DbRawMetricRegistry().getMap();

    public AbstractDbDc(Properties properties) {
        dbDriver = properties.getProperty(DB_DRIVER);
        dbAddress = properties.getProperty(DB_ADDRESS);
        dbPort = Long.parseLong(properties.getProperty(DB_PORT));
        dbConnUrl = properties.getProperty(DB_CONN_URL);
        dbUserName = properties.getProperty(DB_USERNAME);
        dbPassword = properties.getProperty(DB_PASSWORD);
        dbEntityType = properties.getProperty(DB_ENTITY_TYPE, DEFAULT_DB_ENTITY_TYPE);
        dbName = properties.getProperty(DB_NAME);
        dbVersion = properties.getProperty(DB_VERSION);
        serviceInstanceId = properties.getProperty(OTEL_SERVICE_INSTANCE_ID);
    }

    @Override
    public Resource getResourceAttributes(String serviceName, String dbSystem) {
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
                        com.instana.agent.sensorsdk.semconv.ResourceAttributes.INSTANA_PLUGIN,
                        com.instana.agent.sensorsdk.semconv.ResourceAttributes.DATABASE
                )));
        return mergeResourceAttributesFromEnv(resource);
    }

    @Override
    public void initializeMeter(OpenTelemetry openTelemetry) {
        meter = openTelemetry.meterBuilder(DEFAULT_INSTRUMENTATION_SCOPE)
                .setInstrumentationVersion(DEFAULT_INSTRUMENTATION_SCOPE_VER).build();
    }

    @Override
    public void registerMetrics() {
        for (RawMetric rawMetric : rawMetricsMap.values()) {
            DcUtil.registerMetric(meter, rawMetric);
        }
    }

    public RawMetric getRawMetric(String name) {
        return rawMetricsMap.get(name);
    }


    @Override
    public Meter getDefaultMeter() {
        return meter;
    }

    public String getDbDriver() {
        return dbDriver;
    }

    public void setDbDriver(String dbDriver) {
        this.dbDriver = dbDriver;
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

    public void setDbEntityType(String dbEntityType) {
        this.dbEntityType = dbEntityType;
    }

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(String serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }
}
