package com.instana.dc.cdc;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Refer to https://opentelemetry.io/docs/specs/otel/logs/data-model/
//
public class ApmEvent {
    private final Logger logger = LoggerFactory.getLogger(ApmEvent.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Instant timestamp;
    private Instant observedTimestamp;
    private String traceId;
    private String spanId;
    private byte traceFlags;
    private String severityText;
    private int severityNumber;
    private Object body;
    private Map<String, Object> resource;
    private String instrumentationScope;
    private Map<String, Object> attributes;
    private String id;

    // Constructors
    public ApmEvent(Instant timestamp, Instant observedTimestamp, String traceId, String spanId, byte traceFlags, String severityText, int severityNumber, Object body, Map<String, Object> resource, String instrumentationScope, Map<String, Object> attributes, String id) {
        objectMapper.registerModule(new JavaTimeModule() );
        this.timestamp = timestamp;
        this.observedTimestamp = observedTimestamp;
        this.traceId = traceId;
        this.spanId = spanId;
        this.traceFlags = traceFlags;
        this.severityText = severityText;
        this.severityNumber = severityNumber;
        this.body = body;
        this.resource = resource;
        this.instrumentationScope = instrumentationScope;
        this.attributes = attributes;
        this.id = id;
    }

    // For JSON constructor
    public ApmEvent() {
        objectMapper.registerModule(new JavaTimeModule() );
    }

    // Getter methods
    public String getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Instant getObservedTimestamp() {
        return observedTimestamp;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public byte getTraceFlags() {
        return traceFlags;
    }

    public String getSeverityText() {
        return severityText;
    }

    public int getSeverityNumber() {
        return severityNumber;
    }

    public Object getBody() {
        return body;
    }

    public Map<String, Object> getResource() {
        return resource;
    }

    public String getInstrumentationScope() {
        return instrumentationScope;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    // toString method
    @Override
    public String toString() {
        try {
            String json = objectMapper.writeValueAsString(this);
            return json;
        } catch (Exception e) {
            logger.error("toString got exception: " + e.getMessage() );
            return null;
        }
    }
}
