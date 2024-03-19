/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix.metric.collection;

import com.instana.agent.sensorsdk.semconv.SemanticAttributes;
import com.instana.dc.rdb.impl.informix.InformixUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.instana.dc.rdb.DbDcUtil.*;


public class MetricsDataExtractorMapping {

    private MetricsDataExtractorMapping() {
        //Private constructor
    }

    private static final Map<String, MetricDataExtractor> dataExtractorMap = new HashMap<>();

    static {
        // long polling metrics

        // medium polling Interval metrics
        dataExtractorMap.put(DB_SQL_COUNT_NAME, new MetricDataExtractor(InformixUtil.SQL_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_SQL_COUNT_NAME), MetricCollectionMode.CMD, Number.class));
        dataExtractorMap.put(DB_SQL_RATE_NAME, new MetricDataExtractor(InformixUtil.SQL_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_SQL_RATE_NAME), MetricCollectionMode.CMD, Number.class));
        dataExtractorMap.put(DB_TRANSACTION_COUNT_NAME, new MetricDataExtractor(InformixUtil.TRANSACTION_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_TRANSACTION_COUNT_NAME), MetricCollectionMode.CMD, Number.class));
        dataExtractorMap.put(DB_TRANSACTION_RATE_NAME, new MetricDataExtractor(InformixUtil.TRANSACTION_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_TRANSACTION_RATE_NAME), MetricCollectionMode.CMD, Number.class));
        dataExtractorMap.put(DB_SQL_ELAPSED_TIME_NAME, new MetricDataExtractor(InformixUtil.SQL_ELAPSED_TIME_SQL, null, MetricCollectionMode.SQL, List.class, DB_SQL_ELAPSED_TIME_KEY, SemanticAttributes.SQL_TEXT.getKey()));
        // short polling Interval metrics
        dataExtractorMap.put(DB_INSTANCE_COUNT_NAME, new MetricDataExtractor(InformixUtil.INSTANCE_COUNT_SQL, null, MetricCollectionMode.SQL, Number.class));
        dataExtractorMap.put(DB_INSTANCE_ACTIVE_COUNT_NAME, new MetricDataExtractor(InformixUtil.INSTANCE_ACTIVE_COUNT_SQL, null, MetricCollectionMode.SQL, Number.class));
        dataExtractorMap.put(DB_SESSION_COUNT_NAME, new MetricDataExtractor(InformixUtil.SESSION_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_SESSION_COUNT_NAME), MetricCollectionMode.CMD, Number.class));
        dataExtractorMap.put(DB_SESSION_ACTIVE_COUNT_NAME, new MetricDataExtractor(InformixUtil.ACTIVE_SESSION, CommandLineConstants.getMetricScriptMapping(DB_SESSION_ACTIVE_COUNT_NAME), MetricCollectionMode.CMD, Number.class));
        dataExtractorMap.put(DB_IO_READ_RATE_NAME, new MetricDataExtractor(InformixUtil.IO_READ_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_IO_READ_RATE_NAME), MetricCollectionMode.CMD, Number.class));
        dataExtractorMap.put(DB_IO_WRITE_RATE_NAME, new MetricDataExtractor(InformixUtil.IO_WRITE_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_IO_WRITE_RATE_NAME), MetricCollectionMode.CMD, Number.class));
        dataExtractorMap.put(DB_MEM_UTILIZATION_NAME, new MetricDataExtractor(InformixUtil.MEMORY_UTILIZATION_SQL, CommandLineConstants.getMetricScriptMapping(DB_MEM_UTILIZATION_NAME), MetricCollectionMode.CMD, Number.class));

    }

    public static MetricDataExtractor getMetricDataExtractor(String metricName) {
        return dataExtractorMap.get(metricName);
    }

    public static void subscribeDataExtractor(String metricKey, MetricDataExtractor metricDataExtractor) {
        dataExtractorMap.put(metricKey, metricDataExtractor);
    }
}
