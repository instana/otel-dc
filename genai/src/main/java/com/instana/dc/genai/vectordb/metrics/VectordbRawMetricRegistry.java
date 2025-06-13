package com.instana.dc.genai.vectordb.metrics;

import static com.instana.dc.InstrumentType.GAUGE;
import static com.instana.dc.InstrumentType.UPDOWN_COUNTER;
import static com.instana.dc.genai.vectordb.utils.VectordbDcUtil.*;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.instana.dc.RawMetric;

public final class VectordbRawMetricRegistry {
    private static final Map<String, RawMetric> RAW_METRICS;
    
    static {
        Map<String, RawMetric> metrics = new ConcurrentHashMap<>();
        metrics.put(MILVUS_DB_QUERY_DURATION_NAME, new RawMetric(GAUGE, MILVUS_DB_QUERY_DURATION_NAME, MILVUS_DB_QUERY_DURATION_DESC, MILVUS_DB_QUERY_DURATION_UNIT, false, "server.address"));
        metrics.put(MILVUS_DB_SEARCH_DISTANCE_NAME, new RawMetric(GAUGE, MILVUS_DB_SEARCH_DISTANCE_NAME, MILVUS_DB_SEARCH_DISTANCE_DESC, MILVUS_DB_SEARCH_DISTANCE_UNIT, false, "server.address"));
        metrics.put(MILVUS_DB_INSERT_UNITS_NAME, new RawMetric(UPDOWN_COUNTER, MILVUS_DB_INSERT_UNITS_NAME, MILVUS_DB_INSERT_UNITS_DESC, MILVUS_DB_INSERT_UNITS_UNIT, false, "server.address"));
        metrics.put(MILVUS_DB_UPSERT_UNITS_NAME, new RawMetric(UPDOWN_COUNTER, MILVUS_DB_UPSERT_UNITS_NAME, MILVUS_DB_UPSERT_UNITS_DESC, MILVUS_DB_UPSERT_UNITS_UNIT, false, "server.address"));
        metrics.put(MILVUS_DB_DELETE_UNITS_NAME, new RawMetric(UPDOWN_COUNTER, MILVUS_DB_DELETE_UNITS_NAME, MILVUS_DB_DELETE_UNITS_DESC, MILVUS_DB_DELETE_UNITS_UNIT, false, "server.address"));
        RAW_METRICS = Collections.unmodifiableMap(metrics);
    }

    private VectordbRawMetricRegistry() {
        // Private constructor to prevent instantiation
    }

    public static Map<String, RawMetric> getRawMetrics() {
        return RAW_METRICS;
    }
} 