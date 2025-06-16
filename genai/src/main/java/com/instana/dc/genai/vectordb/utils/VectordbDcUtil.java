package com.instana.dc.genai.vectordb.utils;

public class VectordbDcUtil {

    private VectordbDcUtil() { }

    // Milvus metrics
    public static final String MILVUS_DB_QUERY_DURATION_NAME = "db.milvus.query.duration";
    public static final String MILVUS_DB_QUERY_DURATION_DESC = "Duration of Milvus queries";
    public static final String MILVUS_DB_QUERY_DURATION_UNIT = "ms";

    public static final String MILVUS_DB_SEARCH_DISTANCE_NAME = "db.milvus.search.distance";
    public static final String MILVUS_DB_SEARCH_DISTANCE_DESC = "Cosine distance between search query vector and matched vectors";
    public static final String MILVUS_DB_SEARCH_DISTANCE_UNIT = "{cosine}";

    public static final String MILVUS_DB_INSERT_UNITS_NAME = "db.milvus.usage.insert_units";
    public static final String MILVUS_DB_INSERT_UNITS_DESC = "Insert units used by Milvus";
    public static final String MILVUS_DB_INSERT_UNITS_UNIT = "{count}";

    public static final String MILVUS_DB_UPSERT_UNITS_NAME = "db.milvus.usage.upsert_units";
    public static final String MILVUS_DB_UPSERT_UNITS_DESC = "Upsert units used by Milvus";
    public static final String MILVUS_DB_UPSERT_UNITS_UNIT = "{count}";

    public static final String MILVUS_DB_DELETE_UNITS_NAME = "db.milvus.usage.delete_units";
    public static final String MILVUS_DB_DELETE_UNITS_DESC = "Delete units used by Milvus";
    public static final String MILVUS_DB_DELETE_UNITS_UNIT = "{count}";


    public static final String MILVUS_DB_SERVICE_QUERY_DURATION_NAME = "db.milvus.service.query.duration";
    public static final String MILVUS_DB_SERVICE_QUERY_DURATION_DESC = "Duration of Milvus queries";
    public static final String MILVUS_DB_SERVICE_QUERY_DURATION_UNIT = "ms";

    public static final String MILVUS_DB_SERVICE_SEARCH_DISTANCE_NAME = "db.milvus.service.search.distance";
    public static final String MILVUS_DB_SERVICE_SEARCH_DISTANCE_DESC = "Cosine distance between search query vector and matched vectors";
    public static final String MILVUS_DB_SERVICE_SEARCH_DISTANCE_UNIT = "{cosine}";

    public static final String MILVUS_DB_SERVICE_INSERT_UNITS_NAME = "db.milvus.service.usage.insert_units";
    public static final String MILVUS_DB_SERVICE_INSERT_UNITS_DESC = "Insert units used by Milvus";
    public static final String MILVUS_DB_SERVICE_INSERT_UNITS_UNIT = "{count}";

    public static final String MILVUS_DB_SERVICE_UPSERT_UNITS_NAME = "db.milvus.service.usage.upsert_units";
    public static final String MILVUS_DB_SERVICE_UPSERT_UNITS_DESC = "Upsert units used by Milvus";
    public static final String MILVUS_DB_SERVICE_UPSERT_UNITS_UNIT = "{count}";

    public static final String MILVUS_DB_SERVICE_DELETE_UNITS_NAME = "db.milvus.service.usage.delete_units";
    public static final String MILVUS_DB_SERVICE_DELETE_UNITS_DESC = "Delete units used by Milvus";
    public static final String MILVUS_DB_SERVICE_DELETE_UNITS_UNIT = "{count}";
}
