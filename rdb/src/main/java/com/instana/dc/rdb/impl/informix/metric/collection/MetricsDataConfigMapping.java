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


public class MetricsDataConfigMapping {

    private static final Map<String, MetricDataConfig> metricConfigMap = new HashMap<>();

    public static MetricDataConfig getMetricDataConfig(String metricName) {
        return metricConfigMap.get(metricName);
    }

    public static void subscribeMetricDataConfig(String metricKey, MetricDataConfig metricDataConfig) {
        metricConfigMap.put(metricKey, metricDataConfig);
    }
}
