/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix.metric.collection.strategy;

import com.instana.dc.rdb.impl.informix.OnstatCommandExecutor;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricDataConfig;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandExecutorStrategy extends MetricsExecutionStrategy {
    private static final Logger LOGGER = Logger.getLogger(MetricsCollector.class.getName());
    private final OnstatCommandExecutor onstatCommandExecutor;

    protected CommandExecutorStrategy(OnstatCommandExecutor onstatCommandExecutor) {
        this.onstatCommandExecutor = onstatCommandExecutor;
    }

    protected <T> T collectMetrics(MetricDataConfig metricDataConfig) {
        return (T) collectMetricsUsingCMD(metricDataConfig, onstatCommandExecutor);
    }

    private Number collectMetricsUsingCMD(MetricDataConfig metricDataConfig, OnstatCommandExecutor onstatCommandExecutor) {
        Optional<String[]> result = onstatCommandExecutor.executeCommand(metricDataConfig.getScriptName());
        if (result.isPresent()) {// print result
            if(TypeChecker.isDouble(metricDataConfig.getReturnType())){
                //print result.get()[0] + isDouble
                LOGGER.info("checking for isDouble " + result.get()[0]);
                return Double.parseDouble(result.get()[0]);
            }
            else if (TypeChecker.isNumber(metricDataConfig.getReturnType())) {
                //print result.get()[0]+ isNumber
                LOGGER.info("checking for isNumber " + result.get()[0]);
                return Integer.parseInt(result.get()[0]);
            }
        }
        return null;
    }
}
