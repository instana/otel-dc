package com.instana.dc.controllers;

import com.instana.dc.cdc.ApmDc;
import com.instana.dc.cdc.ApmEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CdcControllerTest {
    CdcController cdc;

    @BeforeEach
    void prepare() {
        cdc = new CdcController();
    }

    @Test
    void testParseValidJson() {
        String validJson = "{\"timestamp\":\"2024-02-27T15:02:25Z\",\"observedTimestamp\":\"2024-02-27T15:02:30Z\",\"traceId\":\"12345\",\"spanId\":\"56789\",\"traceFlags\":1,\"severityText\":\"INFO\",\"severityNumber\":2,\"body\":\"Sample body\",\"resource\":{\"key1\":\"value1\"},\"instrumentationScope\":{\"key2\":\"value2\"},\"attributes\":{\"key3\":\"value3\"}}";

        ApmEvent apmEvent = cdc.parseJson(validJson);

        assertEquals("12345", apmEvent.getTraceId() );
        assertEquals("56789", apmEvent.getSpanId() );
        assertEquals("INFO", apmEvent.getSeverityText() );
    }

    @Test
    void testParseInvalidJson() {
        String invalidJson = "invalid json"; // Replace with actual invalid JSON

        ApmEvent apmEvent = cdc.parseJson(invalidJson);

        assertNull(apmEvent); // Expecting null due to parsing failure
    }
}
