package com.instana.dc.cdc;

import java.time.Instant;
import java.util.Map;
import java.util.Map;

// Refer to https://opentelemetry.io/docs/specs/otel/logs/data-model/
//
public class ApmEvent {
    private Instant timestamp;
    private Instant observedTimestamp;
    private String traceId;
    private String spanId;
    private byte traceFlags;
    private String severityText;
    private int severityNumber;
    private Object body;
    private Map<String, Object> resource;
    private Map<String, String> instrumentationScope;
    private Map<String, Object> attributes;

    // Constructors
    public ApmEvent(Instant timestamp, Instant observedTimestamp, String traceId, String spanId, byte traceFlags, String severityText, int severityNumber, Object body, Map<String, Object> resource, Map<String, String> instrumentationScope, Map<String, Object> attributes) {
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
    }

    // Getter methods
    public String getId() {
        StringBuilder idBuilder = new StringBuilder();

        // Append resource values (if not null)
        if (resource != null) {
            for (Map.Entry<String, Object> entry : resource.entrySet() ) {
                if (entry.getValue() instanceof String) {
                    String value = (String) entry.getValue();
                    if (value != null) {
                        idBuilder.append(value);
                        idBuilder.append(":");
                    }
                }
            }
        }
        // Append instrumentationScope values (if not null)
        if (instrumentationScope != null) {
            for (Map.Entry<String, String> entry : instrumentationScope.entrySet()) {
                String value = entry.getValue();
                if (value != null) {
                    idBuilder.append(value);
                    idBuilder.append(":");
                }
            }
        }

        // Remove the tailing ':'
        String eventId = idBuilder.toString().substring(0, idBuilder.length()-1);
        return eventId;
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

    public Map<String, String> getInstrumentationScope() {
        return instrumentationScope;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    // toString method
    @Override
    public String toString() {
        return "ApmEvent{" +
                "timestamp=" + timestamp +
                ", observedTimestamp=" + observedTimestamp +
                ", traceId='" + traceId + '\'' +
                ", spanId='" + spanId + '\'' +
                ", traceFlags=" + traceFlags +
                ", severityText='" + severityText + '\'' +
                ", severityNumber=" + severityNumber +
                ", body=" + body +
                ", resource=" + resource +
                ", instrumentationScope=" + instrumentationScope +
                ", attributes=" + attributes +
                '}';
    }
}
