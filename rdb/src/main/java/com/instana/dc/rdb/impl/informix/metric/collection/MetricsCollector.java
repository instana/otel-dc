package com.instana.dc.rdb.impl.informix.metric.collection;

import com.instana.dc.rdb.impl.informix.OnstatCommandExecutor;
import com.instana.dc.rdb.impl.informix.metric.collection.strategy.CommandExecutorStrategy;
import com.instana.dc.rdb.impl.informix.metric.collection.strategy.MetricsExecutionStrategy;
import com.instana.dc.rdb.impl.informix.metric.collection.strategy.SqlExecutorStrategy;

import java.sql.Connection;
import java.util.logging.Logger;


public class MetricsCollector {

    private MetricCollectionMode modeSelected;

    public MetricsCollector(MetricCollectionMode mode){
        this.modeSelected = mode;
    }
    private static final Logger LOGGER = Logger.getLogger(MetricsCollector.class.getName());

    public <T> T collectMetrics(String metricName, Connection connection, OnstatCommandExecutor onstatCommandExecutor){
        MetricDataConfig metricDataConfig = MetricsExecutionStrategy.retrieveMetricDataConfig(metricName);
        MetricCollectionMode mode = modeSelected;
        if(modeSelected==MetricCollectionMode.DEFAULT){
            mode = metricDataConfig.getDefaultMode();
        }
        MetricsExecutionStrategy executionStrategy;
        if(mode==MetricCollectionMode.SQL){
            executionStrategy = new SqlExecutorStrategy(connection);
        }
        else if(mode==MetricCollectionMode.CMD){
            executionStrategy = new CommandExecutorStrategy(onstatCommandExecutor);
        }
        else{
            throw new IllegalStateException("Invalid Mode selected");
        }
        return executionStrategy.collectMetrics(metricName);
    }


}
