package com.instana.dc.rdb.impl.informix.metric.collection;

import com.instana.dc.rdb.impl.informix.OnstatCommandExecutor;
import com.instana.dc.rdb.impl.informix.metric.collection.strategy.CommandExecutorStrategy;
import com.instana.dc.rdb.impl.informix.metric.collection.strategy.MetricsExecutionStrategy;
import com.instana.dc.rdb.impl.informix.metric.collection.strategy.SqlExecutorStrategy;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MetricsCollector {

    private MetricCollectionMode modeSelected;

    private BasicDataSource dataSource;
    private OnstatCommandExecutor onstatCommandExecutor;

    public MetricsCollector(MetricCollectionMode mode) {
        this.modeSelected = mode;
    }

    public MetricsCollector(BasicDataSource dataSource, OnstatCommandExecutor onstatCommandExecutor) {
        this.onstatCommandExecutor = onstatCommandExecutor;
        this.dataSource = dataSource;
    }

    private static final Logger LOGGER = Logger.getLogger(MetricsCollector.class.getName());

    public <T> T collectMetrics(String metricName) {
        MetricDataConfig metricDataConfig = MetricsExecutionStrategy.retrieveMetricDataConfig(metricName);

        MetricCollectionMode mode = metricDataConfig.getSelectedModeMode();


        //TODO: Based on the mode use the strategy
        if (modeSelected == MetricCollectionMode.DEFAULT) { //CMD
            mode = metricDataConfig.getDefaultMode();
        }
        MetricsExecutionStrategy executionStrategy;
        if (mode == MetricCollectionMode.SQL) {
            try (Connection connection = dataSource.getConnection()) {
                executionStrategy = new SqlExecutorStrategy(connection);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else if (mode == MetricCollectionMode.CMD) {
            executionStrategy = new CommandExecutorStrategy(onstatCommandExecutor);
        } else {
            LOGGER.log(Level.SEVERE, "Invalid execution mode: " + mode + " for metrics: " + metricName);
            throw new IllegalStateException("Invalid Mode selected");
        }
        return executionStrategy.collectMetrics(metricName);
    }


}
