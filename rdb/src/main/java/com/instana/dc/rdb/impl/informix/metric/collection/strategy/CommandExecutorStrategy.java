/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix.metric.collection.strategy;

import com.instana.dc.rdb.impl.informix.OnstatCommandExecutor;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricDataConfig;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricsDataConfigMapping;

public class CommandExecutorStrategy implements MetricsExecutionStrategy {

    private final OnstatCommandExecutor onstatCommandExecutor;

    public CommandExecutorStrategy(OnstatCommandExecutor onstatCommandExecutor) {
        this.onstatCommandExecutor = onstatCommandExecutor;
    }

    @Override
    public <T> T collectMetrics(String metricName) {
        MetricDataConfig metricDataConfig = MetricsDataConfigMapping.getMetricDataConfig(metricName);
        return (T) collectMetricsUsingCMD(metricDataConfig, onstatCommandExecutor);
    }

    private Number collectMetricsUsingCMD(MetricDataConfig metricDataConfig, OnstatCommandExecutor onstatCommandExecutor) {
        if (TypeChecker.isNumber(metricDataConfig.getReturnType())) {
            return Integer.parseInt(onstatCommandExecutor.executeCommand(metricDataConfig.getCommand())[0]); //TODO: Need to verify this
        }
        return null;
    }
}
