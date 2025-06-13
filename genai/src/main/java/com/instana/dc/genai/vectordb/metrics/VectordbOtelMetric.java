package com.instana.dc.genai.vectordb.metrics;

import com.instana.dc.genai.metrics.OtelMetric;

public class VectordbOtelMetric extends OtelMetric {
    private String dbSystem;
    private long insertCount;
    private long insertCountStartTime;
    private long deltaInsertCount;
    private long upsertCount;
    private long upsertCountStartTime;
    private long deltaUpsertCount;
    private long deleteCount;
    private long deleteCountStartTime;
    private long deltaDeleteCount;
    private double searchDistance;
    private long deltaSearchDistance;
    private long searchDistanceStartTime;

    public VectordbOtelMetric() {
        super();
    }

    public String getDbSystem() {
        return this.dbSystem;
    }

    public long getLastInsertCount() {
        return this.insertCount;
    }

    public long getLastUpsertCount() {
        return this.upsertCount;
    }

    public long getLastDeleteCount() {
        return this.deleteCount;
    }

    public double getLastSearchDistance() {
        return this.searchDistance;
    }

    public long getLastInsertCountStartTime() {
        return this.insertCountStartTime;
    }

    public long getLastUpsertCountStartTime() {
        return this.upsertCountStartTime;
    }

    public long getLastDeleteCountStartTime() {
        return this.deleteCountStartTime;
    }

    public long getLastSearchDistanceStartTime() {
        return this.searchDistanceStartTime;
    }

    public long getDeltaInsertCount() {
        return this.deltaInsertCount;
    }

    public long getDeltaUpsertCount() {
        return this.deltaUpsertCount;
    }

    public long getDeltaDeleteCount() {
        return this.deltaDeleteCount;
    }

    public long getDeltaSearchDistance() {
        return this.deltaSearchDistance;
    }

    public void setDbSystem(String dbSystem) {
        this.dbSystem = dbSystem;
    }

    public void setLastInsertCount(long insertCount) {
        this.insertCount = insertCount;
    }

    public void setLastUpsertCount(long upsertCount) {
        this.upsertCount = upsertCount;
    }

    public void setLastDeleteCount(long deleteCount) {
        this.deleteCount = deleteCount;
    }

    public void setLastSearchDistance(double searchDistance) {
        this.searchDistance = searchDistance;
    }

    public void setLastInsertCountStartTime(long startTime) {
        this.insertCountStartTime = startTime;
    }

    public void setLastUpsertCountStartTime(long startTime) {
        this.upsertCountStartTime = startTime;
    }

    public void setLastDeleteCountStartTime(long startTime) {
        this.deleteCountStartTime = startTime;
    }

    public void setLastSearchDistanceStartTime(long startTime) {
        this.searchDistanceStartTime = startTime;
    }

    public void addDeltaInsertCount(long deltaInsertCount) {
        this.deltaInsertCount += deltaInsertCount;
    }

    public void addDeltaUpsertCount(long deltaUpsertCount) {
        this.deltaUpsertCount += deltaUpsertCount;
    }

    public void addDeltaDeleteCount(long deltaDeleteCount) {
        this.deltaDeleteCount += deltaDeleteCount;
    }

    public void addDeltaSearchDistance(double deltaSearchDistance) {
        this.deltaSearchDistance += (long)deltaSearchDistance;
    }

    @Override
    public void resetDeltaValues() {
        super.resetDeltaValues();
        this.deltaInsertCount = 0;
        this.deltaUpsertCount = 0;
        this.deltaDeleteCount = 0;
        this.deltaSearchDistance = 0;
    }
}