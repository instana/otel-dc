/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */
package com.instana.dc;

import java.util.HashMap;
import java.util.Map;

public enum HeadersSupplier {
    INSTANCE;
    
    private final Map<String, String> headers = new HashMap<>();
    
    public synchronized void updateHeaders(Map<String, String> newHeaders) {
        headers.clear();
        headers.putAll(newHeaders);
    }
    
    public Map<String, String> getHeaders() {
        return new HashMap<>(headers);
    }
}
