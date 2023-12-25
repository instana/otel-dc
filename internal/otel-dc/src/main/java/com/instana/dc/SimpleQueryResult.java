/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc;

import java.util.HashMap;
import java.util.Map;

public class SimpleQueryResult {
    private final Number value;
    private String key;
    private final Map<String, Object> attributes = new HashMap<>();

    public SimpleQueryResult(Number value) {
        this.value = value;
    }

    public Number getValue() {
        return value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public SimpleQueryResult setAttribute(String key, Object attribute) {
        attributes.put(key, attribute);
        return this;
    }
}
