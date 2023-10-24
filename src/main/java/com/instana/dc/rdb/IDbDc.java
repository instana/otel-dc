/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.resources.Resource;

import java.sql.Connection;
import java.sql.SQLException;

public interface IDbDc {
    Connection getConnection() throws SQLException, ClassNotFoundException;
    Resource getResourceAttributes(String serviceName, String dbSystem);
    void initializeMeter(OpenTelemetry openTelemetry);
    Meter getDefaultMeter();
    void registerMetrics();
    void collectData();
}
