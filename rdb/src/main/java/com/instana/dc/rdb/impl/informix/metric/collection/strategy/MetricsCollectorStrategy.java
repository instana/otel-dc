/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix.metric.collection.strategy;

import com.instana.dc.rdb.impl.informix.OnstatCommandExecutor;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricCollectionMode;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricDataExtractor;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricsDataExtractorMapping;

import java.sql.Connection;
import java.util.List;
import java.util.logging.Logger;

import static com.instana.dc.rdb.DbDcUtil.getMetricWithSql;
import static com.instana.dc.rdb.DbDcUtil.getSimpleMetricWithSql;


public abstract class MetricsCollectorStrategy {

    private SqlExecutorStrategy sqlExecutorStrategy;
    private CommandExecutorStrategy commandExecutorStrategy;

    public MetricsCollectorStrategy(OnstatCommandExecutor onstatCommandExecutor) {
        this.sqlExecutorStrategy = new SqlExecutorStrategy();
        this.commandExecutorStrategy = new CommandExecutorStrategy(onstatCommandExecutor);
    }

    protected MetricsCollectorStrategy() {
    }


    public abstract <T> T collectMetrics(String metricName);

    static class TypeChecker {
        public static boolean isNumber(Class<?> type) {
            return Number.class.isAssignableFrom(type);
        }

        public static boolean isString(Class<?> type) {
            return String.class.isAssignableFrom(type);
        }

        public static boolean isList(Class<?> type) {
            return List.class.isAssignableFrom(type);
        }
    }
}
