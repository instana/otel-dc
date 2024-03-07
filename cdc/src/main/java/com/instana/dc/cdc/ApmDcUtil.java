/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.cdc;

import java.util.logging.Logger;

public class ApmDcUtil {
    private static final Logger logger = Logger.getLogger(ApmDcUtil.class.getName());

    /* Configurations for the Data Collector:
     */
    public static final String CDC_APM = "cdc_apm";
    public static final String DEFAULT_INSTRUMENTATION_SCOPE = "instana.sensor-sdk.dc.cdc";
    public static final String DEFAULT_INSTRUMENTATION_SCOPE_VER = "1.0.0";

    /* Configurations for Metrics:
     */
    public static final String UNIT_S = "s";
    public static final String UNIT_BY = "By";
    public static final String UNIT_1 = "1";

    public static final String M_EVENT_NAME = "EVENT";
    public static final String M_EVENT_DESC = "The event from 3rd party APM";
    public static final String M_EVENT_UNIT = "{event}";
    public static final String EVENT_KEY = "key";

    //
    // Constants for field names
    public static final String EVENT_TIMESTAMP = "timestamp";
    public static final String EVENT_OBSERVED_TIMESTAMP = "observedTimestamp";
    public static final String EVENT_TRACE_ID = "traceId";
    public static final String EVENT_SPAN_ID = "spanId";
    public static final String EVENT_TRACE_FLAGS = "traceFlags";
    public static final String EVENT_SEVERITY_TEXT = "severityText";
    public static final String EVENT_SEVERITY_NUMBER = "severityNumber";
    public static final String EVENT_BODY = "body";
    public static final String EVENT_RESOURCE = "resource";
    public static final String EVENT_INSTRUMENTATION_SCOPE = "instrumentationScope";
    public static final String EVENT_ATTRIBUTES = "attributes";
}
