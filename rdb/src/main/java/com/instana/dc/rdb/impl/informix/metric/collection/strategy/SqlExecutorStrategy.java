/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix.metric.collection.strategy;

import com.instana.dc.rdb.impl.informix.metric.collection.MetricDataConfig;

import java.sql.Connection;

import static com.instana.dc.rdb.DbDcUtil.getMetricWithSql;
import static com.instana.dc.rdb.DbDcUtil.getSimpleMetricWithSql;

public class SqlExecutorStrategy extends MetricsExecutionStrategy {

    private final Connection connection;
    public SqlExecutorStrategy(Connection connection){
        this.connection = connection;
    }
    @Override
    public <T> T collectMetrics(String metricName) {
        MetricDataConfig metricDataConfig = MetricsExecutionStrategy.retrieveMetricDataConfig(metricName);
        T metricValue = null;
        metricValue = collectMetricsUsingSQL(metricDataConfig,this.connection);
        TypeChecker.checkCast(metricValue,metricDataConfig.getReturnType());

        return metricValue;
    }

    private static <T> T collectMetricsUsingSQL(MetricDataConfig metricDataConfig, Connection connection) {
        if (TypeChecker.isNumber(metricDataConfig.getReturnType())) {
            return (T) getSimpleMetricWithSql(connection, metricDataConfig.getQuery());
        } else if (TypeChecker.isList(metricDataConfig.getReturnType())) {
            return (T) getMetricWithSql(connection, metricDataConfig.getQuery(),metricDataConfig.getAttr());
        }
        return null;
    }

}
