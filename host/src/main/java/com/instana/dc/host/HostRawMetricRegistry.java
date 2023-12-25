/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.host;

import com.instana.dc.RawMetric;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.instana.dc.InstrumentType.*;
import static com.instana.dc.host.HostDcUtil.*;

public class HostRawMetricRegistry {
    /* The purpose to set special meter name for metrics is to comply with the "hostmetrics" receiver of
     * OpenTelemetry Contrib Collector.
     **/
    private final Map<String, RawMetric> map = new ConcurrentHashMap<String, RawMetric>() {{
        put(SYSTEM_CPU_TIME_NAME, new RawMetric(COUNTER, SYSTEM_CPU_TIME_NAME, SYSTEM_CPU_TIME_DESC, SYSTEM_CPU_TIME_UNIT, false, "cpu:state", MeterName.CPU));
        put(SYSTEM_MEMORY_USAGE_NAME, new RawMetric(UPDOWN_COUNTER, SYSTEM_MEMORY_USAGE_NAME, SYSTEM_MEMORY_USAGE_DESC, SYSTEM_MEMORY_USAGE_UNIT, true, "state", MeterName.MEMORY));
        put(SYSTEM_CPU_LOAD1_NAME, new RawMetric(GAUGE, SYSTEM_CPU_LOAD1_NAME, SYSTEM_CPU_LOAD1_DESC, SYSTEM_CPU_LOAD1_UNIT, false, null, MeterName.LOAD));
        put(SYSTEM_CPU_LOAD5_NAME, new RawMetric(GAUGE, SYSTEM_CPU_LOAD5_NAME, SYSTEM_CPU_LOAD5_DESC, SYSTEM_CPU_LOAD5_UNIT, false, null, MeterName.LOAD));
        put(SYSTEM_CPU_LOAD15_NAME, new RawMetric(GAUGE, SYSTEM_CPU_LOAD15_NAME, SYSTEM_CPU_LOAD15_DESC, SYSTEM_CPU_LOAD15_UNIT, false, null, MeterName.LOAD));
    }};

    public Map<String, RawMetric> getMap() {
        return map;
    }
}
