/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.host.impl;

import com.instana.dc.host.AbstractHostDc;
import com.instana.dc.DcException;

import java.util.HashMap;
import java.util.Map;

public class HostDcRegistry {
    /* Add all DataCollector implementations here:
    **/
    private final Map<String, Class<? extends AbstractHostDc>> map = new HashMap<String, Class<? extends AbstractHostDc>>() {{
        put("SIMP_HOST", SimpHostDc.class);
    }};

    public Class<? extends AbstractHostDc> findHostDc(String hostSystem) throws DcException {
        Class<? extends AbstractHostDc> cls = map.get(hostSystem.toUpperCase());
        if (cls != null) {
            return cls;
        } else {
            throw new DcException("Unsupported Host system: " + hostSystem);
        }
    }
}
