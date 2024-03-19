/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix.metric.collection;


public class MetricDataExtractor {
    private final String query;
    private final String command;

    private final Class<?> returnType;

    private final String[] attr;

    MetricDataExtractor(String query, String command, Class<?> returnType) {
        this.query = query;
        this.command = command;
        this.returnType = returnType;
        this.attr = null;
    }

    public MetricDataExtractor(String query, String command, Class<?> returnType, String... attr) {
        this.query = query;
        this.command = command;
        this.returnType = returnType;
        this.attr = attr;
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
