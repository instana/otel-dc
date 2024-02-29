package com.instana.dc.cdc;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/*
 * The payload in this test is like:
{
  "timestamp": 1709190814.136849342,
  "observedTimestamp": 1709190814.136849342,
  "traceId": null,
  "spanId": null,
  "traceFlags": 0,
  "severityText": "ERROR",
  "severityNumber": 20,
  "body": "This is an error example.",
  "resource": {
    "dataCenter": "rtp",
    "name": "dt21.fyre.ibm.com"
  },
  "instrumentationScope": {
    "name": "sample-error"
  },
  "attributes": {
    "duration": 600,
    "previous": {
      "metric1": 1.03
    },
    "metric1": 0.02,
    "description": "The value of metric1 should be greater than 1."
  },
  "id": "rtp:dt21.fyre.ibm.com:sample-error"
}
*/

class ApmEventTest {
    ApmEvent event;

    @BeforeEach
    void prepare() {
        Instant ts = Instant.now();
        int severityNumber = 20;
        String severityText = "ERROR";
        String body = "This is an error example.";
        Map<String, Object> resource = new HashMap<String, Object>();
        resource.put("dataCenter", "rtp");
        resource.put("name", "dt21.fyre.ibm.com");
        Map<String, String> instrumentationScope = new HashMap<String, String>();
        instrumentationScope.put("name", "sample-error");
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("duration", 600);
        attributes.put("metric1", 0.02);
        attributes.put("description", "The value of metric1 should be greater than 1.");
        Map<String, Object> previous = new HashMap<String, Object>();
        previous.put("metric1", 1.03);
        attributes.put("previous", previous);
        event = new ApmEvent(ts, ts, null, null, (byte) 0, severityText, severityNumber, body, resource, instrumentationScope, attributes);
    }

    @Test
    void testGetId() {
        assertEquals("rtp:dt21.fyre.ibm.com:sample-error", event.getId() );
    }

    @Test
    void testToString() {
        String fixed = "\"traceId\":null,\"spanId\":null,\"traceFlags\":0,\"severityText\":\"ERROR\",\"severityNumber\":20,\"body\":\"This is an error example.\",\"resource\":{\"dataCenter\":\"rtp\",\"name\":\"dt21.fyre.ibm.com\"},\"instrumentationScope\":{\"name\":\"sample-error\"},\"attributes\":{\"duration\":600,\"previous\":{\"metric1\":1.03},\"metric1\":0.02,\"description\":\"The value of metric1 should be greater than 1.\"},\"id\":\"rtp:dt21.fyre.ibm.com:sample-error\"}";
        String json = event.toString();
        assertNotNull(json);
        assertTrue(json.indexOf(fixed) > 0);
    }
}
