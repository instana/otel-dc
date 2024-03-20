/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix.metric.collection;


public class MetricDataConfig {
    private  String query;
    private  String command;

    private final Class<?> returnType;

    private final MetricCollectionMode defaultMode;
    private final MetricCollectionMode selectedModeMode;
    private final String[] attr;

    //TODO: Two different Constructor
    MetricDataConfig(String query, MetricCollectionMode mode, Class<?> returnType) {
        this.query = query;
        this.returnType = returnType;
        this.attr = null;
        this.selectedModeMode = mode;
    }

    MetricDataConfig(String query, MetricCollectionMode mode, Class<?> returnType) {
        this.query = query;
        this.returnType = returnType;
        this.attr = null;
        this.selectedModeMode = mode;
    }

    public MetricDataConfig(String query, String command, MetricCollectionMode mode, Class<?> returnType, String... attr) {
        this.query = query;
        this.command = command;
        this.returnType = returnType;
        this.attr = attr;
        this.defaultMode = mode;
    }

    public MetricCollectionMode getSelectedModeMode() {
        return selectedModeMode;
    }

    public String[] getAttr() {
        return attr;
    }

    public MetricCollectionMode getDefaultMode() {
        return defaultMode;
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
