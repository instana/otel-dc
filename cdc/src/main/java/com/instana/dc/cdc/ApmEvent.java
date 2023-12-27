package com.instana.dc.cdc;

public class ApmEvent {
    private final String vendor;
    private final String eventId;
    private String title;
    private String severity;
    private String description;
    private String timestamp;
    private String etc;
    private static final String UNKNOWN = "UNKNOWN";

    public ApmEvent(String vendor, String eventId, String title, String severity, String description, String timestamp, String etc) {
        this.vendor = vendor;
        this.eventId = eventId;
        this.title = title;
        this.severity = severity;
        this.description = description;
        this.timestamp = timestamp;
        this.etc = etc;
    }

    public ApmEvent(String vendor, String eventId) {
        this.vendor = vendor;
        this.eventId = eventId;
        this.title = UNKNOWN;
        this.severity = UNKNOWN;
        this.description = UNKNOWN;
        this.timestamp = UNKNOWN;
        this.etc = "";
    }

    public String getEventId() {
        return eventId;
    }

    public String getVendor() {
        return vendor;
    }

    public String getKey() {
        return vendor + ":" + eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getEtc() {
        return etc;
    }

    public void setEtc(String etc) {
        this.etc = etc;
    }

    @Override
    public String toString() {
        return "CdcEvent{" +
                "vendor='" + vendor + '\'' +
                ", eventId='" + eventId + '\'' +
                ", title='" + title + '\'' +
                ", severity='" + severity + '\'' +
                ", description='" + description + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", etc='" + etc + '\'' +
                '}';
    }
}
