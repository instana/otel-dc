/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix.metric.collection;

import java.util.HashMap;
import java.util.Map;


public class MetricsDataConfigMapping {
    private static final Map<String, MetricDataConfig> metricConfigMap = new HashMap<>();

    public static MetricDataConfig getMetricDataConfig(String metricName) {
        return metricConfigMap.get(metricName);
    }

    public static void subscribeMetricDataConfig(String metricKey, MetricDataConfig metricDataConfig) {
        metricConfigMap.put(metricKey, metricDataConfig);
    }
}
