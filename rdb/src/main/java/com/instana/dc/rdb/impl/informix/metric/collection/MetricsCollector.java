package com.instana.dc.rdb.impl.informix.metric.collection;

import com.instana.dc.rdb.impl.informix.OnstatCommandExecutor;
import com.instana.dc.rdb.impl.informix.metric.collection.strategy.CommandExecutorStrategy;
import com.instana.dc.rdb.impl.informix.metric.collection.strategy.MetricsExecutionStrategy;
import com.instana.dc.rdb.impl.informix.metric.collection.strategy.SqlExecutorStrategy;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;


public class MetricsCollector {
    private final BasicDataSource dataSource;
    private final OnstatCommandExecutor onstatCommandExecutor;
    public MetricsCollector(BasicDataSource dataSource, OnstatCommandExecutor onstatCommandExecutor) {
        this.onstatCommandExecutor = onstatCommandExecutor;
        this.dataSource = dataSource;
    }

    private static final Logger LOGGER = Logger.getLogger(MetricsCollector.class.getName());

    public <T> T collectMetrics(String metricName) {
        MetricDataConfig metricDataConfig = MetricsExecutionStrategy.retrieveMetricDataConfig(metricName);

        MetricCollectionMode mode = metricDataConfig.getSelectedMode();
        if (mode == MetricCollectionMode.SQL) {
            try (Connection connection = dataSource.getConnection()) {
                 return new SqlExecutorStrategy(connection). collectMetrics(metricName);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else if (mode == MetricCollectionMode.CMD) {
             return new CommandExecutorStrategy(onstatCommandExecutor).collectMetrics(metricName);
        } else {
            throw new IllegalStateException("Invalid Mode selected");
        }
    }


}
