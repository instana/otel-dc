/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.cdc;

import com.instana.dc.RawMetric;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.instana.dc.InstrumentType.GAUGE;
import static com.instana.dc.cdc.ApmDcUtil.*;

public class ApmRawMetricRegistry {
    private final Map<String, RawMetric> map = new ConcurrentHashMap<String, RawMetric>() {{
        put(M_EVENT_NAME, new RawMetric(GAUGE, M_EVENT_NAME, M_EVENT_DESC, M_EVENT_UNIT, true, EVENT_KEY).setClearDps(true));
    }};

    public Map<String, RawMetric> getMap() {
        return map;
    }
}
