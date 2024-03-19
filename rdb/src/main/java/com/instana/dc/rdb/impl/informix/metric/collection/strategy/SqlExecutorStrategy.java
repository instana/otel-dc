/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix.metric.collection.strategy;

import com.instana.dc.rdb.impl.informix.OnstatCommandExecutor;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricDataExtractor;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricsDataExtractorMapping;

import java.sql.Connection;

import static com.instana.dc.rdb.DbDcUtil.getMetricWithSql;
import static com.instana.dc.rdb.DbDcUtil.getSimpleMetricWithSql;

public class SqlExecutorStrategy extends MetricsCollectorStrategy {

    @Override
    public <T> T collectMetrics(String metricName, Connection connection) {
        MetricDataExtractor dataExtractor = MetricsDataExtractorMapping.getMetricDataExtractor(metricName);
        T metricValue = null;
        if (dataExtractor != null) {
            metricValue = collectMetricsUsingSQL(dataExtractor, connection);
            // Type checking to avoid mismatching of data type parsing.
            try {
                dataExtractor.getReturnType().cast(metricValue);
            } catch (ClassCastException e) {
                throw new IllegalStateException("Error casting the metric", e);
            }
        } else {
            throw new IllegalArgumentException("Metric not found:" + metricName);
        }
        return metricValue;
    }

    private static <T> T collectMetricsUsingSQL(MetricDataExtractor metricDataExtractor, Connection connection) {
        if (TypeChecker.isNumber(metricDataExtractor.getReturnType())) {
            return (T) getSimpleMetricWithSql(connection, metricDataExtractor.getQuery());
        } else if (TypeChecker.isList(metricDataExtractor.getReturnType())) {
            return (T) getMetricWithSql(connection, metricDataExtractor.getQuery());
        }
        return null;
    }

}
