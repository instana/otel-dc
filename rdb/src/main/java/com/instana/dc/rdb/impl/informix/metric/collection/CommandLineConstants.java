/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix.metric.collection;

import java.util.HashMap;
import java.util.Map;

import static com.instana.dc.rdb.DbDcUtil.*;

public class CommandLineConstants {

    private static final Map<String, String> metricScriptMapping = new HashMap<>();

    static {
        metricScriptMapping.put(DB_SQL_COUNT_NAME, "sql_count.sh");
        metricScriptMapping.put(DB_SQL_RATE_NAME, "sql_count.sh");
        metricScriptMapping.put(DB_TRANSACTION_COUNT_NAME, "transaction_count.sh");
        metricScriptMapping.put(DB_TRANSACTION_RATE_NAME, "transaction_count.sh");

        //short polling interval metrics
        metricScriptMapping.put(DB_MEM_UTILIZATION_NAME, "memory_utilization.sh");
        metricScriptMapping.put(DB_IO_READ_RATE_NAME, "io_read_count.sh");
        metricScriptMapping.put(DB_IO_WRITE_RATE_NAME, "io_write_count.sh");
        metricScriptMapping.put(DB_SESSION_COUNT_NAME, "session_count.sh");
        metricScriptMapping.put(DB_SESSION_ACTIVE_COUNT_NAME, "session_count.sh");
    }

    public static String getMetricScriptMapping(String metricName) {
        return metricScriptMapping.get(metricName);
    }
}
