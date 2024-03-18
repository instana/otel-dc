package com.instana.dc.rdb.impl.informix.metriccollection;

import com.instana.agent.sensorsdk.semconv.SemanticAttributes;
import com.instana.dc.rdb.impl.informix.InformixUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.instana.dc.rdb.DbDcUtil.*;


public class MetricCommandMapping {

    private static final Map<String,MetricCMD> metricCMDMap = new HashMap<>();
    static {
        // long polling metrics

        // medium polling Interval metrics
        metricCMDMap.put(DB_SQL_COUNT_NAME,new MetricCMD(InformixUtil.SQL_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_SQL_COUNT_NAME),MetricCollectionMode.CMD,Number.class));
        metricCMDMap.put(DB_SQL_RATE_NAME,new MetricCMD(InformixUtil.SQL_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_SQL_RATE_NAME),MetricCollectionMode.CMD,Number.class));
        metricCMDMap.put(DB_TRANSACTION_COUNT_NAME,new MetricCMD(InformixUtil.TRANSACTION_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_TRANSACTION_COUNT_NAME),MetricCollectionMode.CMD,Number.class));
        metricCMDMap.put(DB_TRANSACTION_RATE_NAME,new MetricCMD(InformixUtil.TRANSACTION_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_TRANSACTION_RATE_NAME),MetricCollectionMode.CMD,Number.class));
        metricCMDMap.put(DB_SQL_ELAPSED_TIME_NAME,new MetricCMD(InformixUtil.SQL_ELAPSED_TIME_SQL,null,MetricCollectionMode.SQL, List.class,DB_SQL_ELAPSED_TIME_KEY,SemanticAttributes.SQL_TEXT.getKey()));
        // short polling Interval metrics
        metricCMDMap.put(DB_INSTANCE_COUNT_NAME,new MetricCMD(InformixUtil.INSTANCE_COUNT_SQL,null,MetricCollectionMode.SQL,Number.class));
        metricCMDMap.put(DB_INSTANCE_ACTIVE_COUNT_NAME,new MetricCMD(InformixUtil.INSTANCE_ACTIVE_COUNT_SQL,null,MetricCollectionMode.SQL,Number.class));
        metricCMDMap.put(DB_SESSION_COUNT_NAME,new MetricCMD(InformixUtil.SESSION_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_SESSION_COUNT_NAME),MetricCollectionMode.CMD,Number.class));
        metricCMDMap.put(DB_SESSION_ACTIVE_COUNT_NAME,new MetricCMD(InformixUtil.ACTIVE_SESSION, CommandLineConstants.getMetricScriptMapping(DB_SESSION_ACTIVE_COUNT_NAME),MetricCollectionMode.CMD,Number.class));
        metricCMDMap.put(DB_IO_READ_RATE_NAME,new MetricCMD(InformixUtil.IO_READ_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_IO_READ_RATE_NAME),MetricCollectionMode.CMD,Number.class));
        metricCMDMap.put(DB_IO_WRITE_RATE_NAME,new MetricCMD(InformixUtil.IO_WRITE_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_IO_WRITE_RATE_NAME),MetricCollectionMode.CMD,Number.class));
        metricCMDMap.put(DB_MEM_UTILIZATION_NAME,new MetricCMD(InformixUtil.MEMORY_UTILIZATION_SQL, CommandLineConstants.getMetricScriptMapping(DB_MEM_UTILIZATION_NAME),MetricCollectionMode.CMD,Number.class));

    }
    public static MetricCMD getMetricCMDMap(String metricName) {
        return metricCMDMap.get(metricName);
    }

    public static void addMetricCMD(String metricKey,MetricCMD cmd){
        metricCMDMap.put(metricKey,cmd);
    }
}
