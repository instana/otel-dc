/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.appliance.impl;

import com.instana.dc.appliance.AbstractApplianceDc;
import com.instana.dc.DcException;

import java.util.HashMap;
import java.util.Map;

public class ApplianceDcRegistry {
    /* Add all DataCollector implementations here:
    **/
    private final Map<String, Class<? extends AbstractApplianceDc>> map = new HashMap<String, Class<? extends AbstractApplianceDc>>() {{
        put("MQ_APPLIANCE", MqApplianceDc.class);
    }};

    public Class<? extends AbstractApplianceDc> findApplianceDc(String applianceSystem) throws DcException {
        Class<? extends AbstractApplianceDc> cls = map.get(applianceSystem.toUpperCase());
        if (cls != null) {
            return cls;
        } else {
            throw new DcException("Unsupported appliance: " + applianceSystem);
        }
    }
}
