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
    void initOnce() throws ClassNotFoundException;
    void initDC() throws Exception;

    Connection getConnection() throws SQLException;

    void initMeter(OpenTelemetry openTelemetry);
    void registerMetrics();

    void collectData();
    void start();
}
