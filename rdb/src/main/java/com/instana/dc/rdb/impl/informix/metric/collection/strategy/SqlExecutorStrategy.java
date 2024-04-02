/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix.metric.collection.strategy;

import com.instana.dc.rdb.impl.informix.metric.collection.MetricDataConfig;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.instana.dc.rdb.DbDcUtil.getMetricWithSql;
import static com.instana.dc.rdb.DbDcUtil.getSimpleMetricWithSql;

public class SqlExecutorStrategy extends MetricsExecutionStrategy {

    private static final Logger LOGGER = Logger.getLogger(SqlExecutorStrategy.class.getName());
    private final BasicDataSource dataSource;

    protected SqlExecutorStrategy(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected <T> T collectMetrics(MetricDataConfig metricDataConfig) {
        try (Connection connection = dataSource.getConnection()) {
            T metricValue = collectMetricsUsingSQL(metricDataConfig, connection);
            TypeChecker.checkCast(metricValue, metricDataConfig.getReturnType());
            return metricValue;
        } catch (SQLException exp) {
            LOGGER.log(Level.SEVERE, "Unable to execute the sql command, Exception: " + exp);
        }
        return null;
    }

    private <T> T collectMetricsUsingSQL(MetricDataConfig metricDataConfig, Connection connection) {
        if (TypeChecker.isNumber(metricDataConfig.getReturnType())) {
            return (T) getSimpleMetricWithSql(connection, metricDataConfig.getQuery());
        } else if (TypeChecker.isList(metricDataConfig.getReturnType())) {
            return (T) getMetricWithSql(connection, metricDataConfig.getQuery(), metricDataConfig.getAttr());
        }
        return null;
    }

}
