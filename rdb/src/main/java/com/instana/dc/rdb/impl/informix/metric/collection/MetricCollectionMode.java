/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix.metric.collection;

public enum MetricCollectionMode {
    DEFAULT,
    CMD,
    SQL
}

/**
 *
 * CMD      :  Using onstat command
 * SQL      :  Using SQL
 * Default  :  When we have support for both
 *
 *  XYZ - CMD
 *
 *
 *
 * 10 Metrics
 *      -  6 onstat   - X
 *      -  4  SQL
 *
 *
 *
 */