package com.instana.dc.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.instana.dc.cdc.ApmDc;
import com.instana.dc.cdc.ApmEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Queue;

@RestController
public class CdcController {
    Logger logger = LoggerFactory.getLogger(CdcController.class);

    private final Queue<ApmEvent> evtQueue;

    public CdcController(@Autowired ApmDc apmDc) {
        evtQueue = apmDc.getEvtQueue();
        apmDc.initDC();
        apmDc.start();
    }

    @PostMapping(value = "/webhook")
    public void webhook(@RequestBody JsonNode payload) {
        logger.info("Received: " + payload.toString());
        ApmEvent event = convert(payload);
        if (event == null || evtQueue == null) {
            return;
        }
        logger.info("Converted to: " + event);
        evtQueue.add(event);
    }

    private ApmEvent convert(JsonNode payload) {
        long tm = System.currentTimeMillis();
        String vendor = getInput(payload, "vendor", "N/A");
        String eventId = getInput(payload, "eventId", "Event:" + tm);
        String title = getInput(payload, "title", "N/A");
        String severity = getInput(payload, "severity", "N/A");
        String description = getInput(payload, "description", "N/A");
        String timestamp = getInput(payload, "timestamp", Long.toString(tm));
        String etc = getInput(payload, "etc", "N/A");

        return new ApmEvent(vendor, eventId, title, severity, description, timestamp, etc);
    }

    private static String getInput(JsonNode payload, String key, String defaultValue) {
        JsonNode jValue = payload.get(key);
        if (jValue != null) {
            return jValue.asText();
        }
        return defaultValue;
    }

}