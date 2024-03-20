/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix.metric.collection.strategy;

import com.instana.dc.rdb.impl.informix.metric.collection.MetricDataConfig;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricsDataConfigMapping;

import java.util.List;

public abstract class MetricsExecutionStrategy {
    public static MetricDataConfig retrieveMetricDataConfig(String metricName){
        MetricDataConfig metricDataConfig = MetricsDataConfigMapping.getMetricDataConfig(metricName);
        if (metricDataConfig == null) {
            throw new IllegalArgumentException("Metric not found:" + metricName);
        }
        return metricDataConfig;
    }
    public abstract <T> T collectMetrics(String metricName);

    static class TypeChecker {

        public static <T> T checkCast(Object object, Class<T> returnType){
            try{
                return returnType.cast(object);
            }
            catch (ClassCastException e){
                throw new IllegalStateException("Error casting the metric", e);
            }
        }
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
