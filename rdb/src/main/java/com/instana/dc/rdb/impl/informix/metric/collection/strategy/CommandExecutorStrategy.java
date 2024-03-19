/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix.metric.collection.strategy;

import com.instana.dc.rdb.impl.informix.OnstatCommandExecutor;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricDataExtractor;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricsDataExtractorMapping;

import java.sql.Connection;

public class CommandExecutorStrategy extends MetricsCollectorStrategy {

    private OnstatCommandExecutor onstatCommandExecutor;

    public CommandExecutorStrategy(OnstatCommandExecutor onstatCommandExecutor) {
        this.onstatCommandExecutor = onstatCommandExecutor;
    }

    @Override
    public <T> T collectMetrics(String metricName) {
        MetricDataExtractor dataExtractor = MetricsDataExtractorMapping.getMetricDataExtractor(metricName);
        T metricValue = null;
        if (dataExtractor != null) {
            metricValue = collectMetricsUsingCMD(dataExtractor, onstatCommandExecutor);
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

    private <T> T collectMetricsUsingCMD(MetricDataExtractor metricCMD, OnstatCommandExecutor onstatCommandExecutor) {
        return (T) onstatCommandExecutor.executeCommand(metricCMD.getCommand())[0]; //TODO: Need to verify this
    }
}
