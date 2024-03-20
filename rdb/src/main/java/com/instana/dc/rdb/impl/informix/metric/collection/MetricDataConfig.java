/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix.metric.collection;


public class MetricDataConfig {
    private  String query;
    private  String command;
    private final Class<?> returnType;
    private final MetricCollectionMode selectedMode;
    private String[] attr;

    public MetricDataConfig(String query, Class<?> returnType,String... attr) {
        this.query = query;
        this.returnType = returnType;
        this.selectedMode = MetricCollectionMode.SQL;
        this.attr = attr;
    }

    public MetricDataConfig(String query, String command, MetricCollectionMode mode, Class<?> returnType, String... attr) {
        this.query = query;
        this.command = command;
        this.returnType = returnType;
        this.attr = attr;
        this.selectedMode = mode;
    }

    public MetricCollectionMode getSelectedMode() {
        return selectedMode;
    }

    public String[] getAttr() {
        return attr;
    }
    public String getQuery() {
        return query;
    }

    public String getCommand() {
        return command;
    }

    public Class<?> getReturnType() {
        return returnType;
    }
}
