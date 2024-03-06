package com.instana.dc.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
    private final Logger logger = LoggerFactory.getLogger(CdcController.class);

    private final Queue<ApmEvent> evtQueue;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CdcController(@Autowired ApmDc apmDc) {
        objectMapper.registerModule(new JavaTimeModule() );
        if (apmDc != null) {
            evtQueue = apmDc.getEvtQueue();
            apmDc.initDC();
            apmDc.start();
        }
        else {
            evtQueue = null;
        }
    }

    @PostMapping(value = "/events")
    public void webhook(@RequestBody JsonNode payload) {
        logger.info("Received: " + payload.toString() );
        ApmEvent event = parseJson(payload.toString() );
        if (event == null || evtQueue == null) {
            return;
        }
        logger.info("Converted to: " + event);
        evtQueue.add(event);
    }

    public ApmEvent parseJson(String json) {
        try {
            return objectMapper.readValue(json, ApmEvent.class);
        } catch (Exception e) {
            logger.error("parseJson got exception: " + e.getMessage() );
            return null;
        }
    }
}
