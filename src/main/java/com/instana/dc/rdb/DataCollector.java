/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb;

import com.instana.dc.rdb.impl.DbDcRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.*;
import static com.instana.dc.rdb.DbDcUtil.DB_SYSTEM;

public class DataCollector {
    static {
        System.setProperty("java.util.logging.config.file", LOGGING_PROP);
    }

    private static final Logger logger = Logger.getLogger(DataCollector.class.getName());

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private IDbDc dc;

    private String dbSystem;
    private String otelBackendUrl;
    private int pollInterval;
    private int callbackInterval;
    private String serviceName;


    private void getConfiguration() throws Exception {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_PROP)) {
            properties.load(fis);
            String pollInt = properties.getProperty(POLLING_INTERVAL);
            pollInterval = pollInt == null ? DEFAULT_POLL_INTERVAL : Integer.parseInt(properties.getProperty(POLLING_INTERVAL));
            String callbackInt = properties.getProperty(CALLBACK_INTERVAL);
            callbackInterval = callbackInt == null ? DEFAULT_CALLBACK_INTERVAL : Integer.parseInt(properties.getProperty(CALLBACK_INTERVAL));
            dbSystem = properties.getProperty(DB_SYSTEM).toUpperCase();
            logger.info("Find DC for dbSystem: " + dbSystem);
            otelBackendUrl = properties.getProperty(OTEL_BACKEND_URL);
            serviceName = properties.getProperty(OTEL_SERVICE_NAME);
            dc = new DbDcRegistry().findDatabaseDc(dbSystem).getConstructor(Properties.class).newInstance(properties);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read in or parse the configuration");
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        logger.info("Instana DC for Database is started...");

        DataCollector dcol = new DataCollector();
        dcol.initDC();
        dcol.startCollect();

        logger.info("Instana DC for Database is ended...");
    }

    private void initDC() throws Exception {
        getConfiguration();
        Resource resource = dc.getResourceAttributes(serviceName, dbSystem);
        SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder().registerMetricReader(PeriodicMetricReader.builder(OtlpGrpcMetricExporter.builder().setEndpoint(otelBackendUrl).build()).setInterval(Duration.ofSeconds(callbackInterval)).build()).setResource(resource).build();
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProvider).buildAndRegisterGlobal();
        dc.initializeMeter(openTelemetry);
        dc.registerMetrics();
    }

    private void startCollect() {
        exec.scheduleWithFixedDelay(dc::collectData, 1, pollInterval, TimeUnit.SECONDS);
    }

}
