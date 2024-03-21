/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix.metric.collection.strategy;

import com.instana.dc.rdb.impl.informix.OnstatCommandExecutor;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricCollectionMode;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricDataConfig;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricsDataConfigRegister;
import org.apache.commons.dbcp2.BasicDataSource;

import java.util.logging.Logger;

public class MetricsCollector {
    private static final Logger LOGGER = Logger.getLogger(MetricsCollector.class.getName());
    private final MetricsExecutionStrategy sqlExecutorStrategy;
    private final MetricsExecutionStrategy commandExecutorStrategy;

    public MetricsCollector(BasicDataSource dataSource, OnstatCommandExecutor onstatCommandExecutor) {
        this.commandExecutorStrategy = new CommandExecutorStrategy(onstatCommandExecutor);
        this.sqlExecutorStrategy = new SqlExecutorStrategy(dataSource);
    }

    /**
     * Public Method exposed to the DC to get the Metric Data by metricName
     * Based on the metric metadata it will be using the appropriate strategy
     *
     * @param metricName : Name of the metric
     * @param <T>:       Return Type
     * @return : metric value
     */
    public <T> T collectMetrics(String metricName) {
        MetricDataConfig metricDataConfig = MetricsDataConfigRegister.getMetricDataConfig(metricName);

        MetricCollectionMode mode = metricDataConfig.getSelectedMode();
        if (mode == MetricCollectionMode.SQL) {
            T response = sqlExecutorStrategy.collectMetrics(metricDataConfig);
            //TODO: For Testing only
            LOGGER.info("SQL - For Metric: " + metricName + " response: " + response);
            return response;
        } else if (mode == MetricCollectionMode.CMD) {
            T response = commandExecutorStrategy.collectMetrics(metricDataConfig);
            //TODO: For Testing only
            LOGGER.info("CMD - For Metric: " + metricName + " response: " + response);
            return response;
        }
        throw new IllegalStateException("For Metric: " + metricName + " Invalid Mode selected: " + mode);
    }

}
