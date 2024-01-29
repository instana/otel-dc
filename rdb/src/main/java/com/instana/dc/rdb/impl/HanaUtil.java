package com.instana.dc.rdb.impl;

public class HanaUtil {

    public static final String DB_NAME_VERSION_SQL = "select DATABASE_NAME, VERSION from SYS.M_DATABASE";
    public static final String TRANSACTION_COUNT_SQL = "SELECT COUNT(*) AS transaction_count from M_TRANSACTIONS";

    public static final String TRANSACTION_RATE_SQL = "SELECT CURRENT_TRANSACTION_RATE FROM SYS.M_WORKLOAD";
    public static final String TRANSACTION_LATENCY_SQL = "SELECT SUM(floor( mod( (seconds_between(START_TIME,END_TIME)) , 3600) / 60 ) ) AS TOTAL_TRANSACTION_LATENCY_SECONDS FROM M_TRANSACTIONS";

    public static final String SQL_COUNT_SQL = "SELECT COUNT(*) AS sql_statement_count FROM M_SQL_PLAN_CACHE";
    public static final String SQL_RATE_SQL = "SELECT SUM(AVG_EXECUTION_FETCH_TIME) As latency FROM M_SQL_PLAN_CACHE";

    public static final String IO_READ_COUNT_SQL = "SELECT EXECUTION_COUNT FROM SYS.M_WORKLOAD";
    public static final String IO_WRITE_COUNT_SQL = "SELECT EXECUTION_COUNT FROM SYS.M_WORKLOAD";
    public static final String TASK_WAIT_COUNT_SQL = "SELECT EXECUTION_COUNT FROM SYS.M_WORKLOAD";
    public static final String  TASK_AVG_WAIT_TIME_SQL = "SELECT TOTAL_LOCK_WAIT_TIME FROM M_LOCK_WAITS_STATISTICS";

    public static final String SESSION_COUNT_SQL = "SELECT COUNT(*) as TOTAL_SESSIONS FROM M_SESSION_CONTEXT";

    public static final String SESSION_ACTIVE_COUNT_SQL = "SELECT COUNT(*) as TOTAL_SESSIONS FROM M_SESSION_CONTEXT";
    public static final String INSTANCE_COUNT_SQL = "SELECT  COUNT(*) FROM SYS.M_SYSTEM_OVERVIEW where Name = 'All Started'";
    public static final String  INSTANCE_ACTIVE_COUNT_SQL = "SELECT  COUNT(*) FROM SYS.M_SYSTEM_OVERVIEW where Name = 'All Started'";
    public static final String CPU_UTILIZATION_SQL = "SELECT TOTAL_CPU_SYSTEM_TIME FROM SYS.M_HOST_RESOURCE_UTILIZATION";
    public static final String MEMORY_UTILIZATION = "SELECT USED_PHYSICAL_MEMORY FROM SYS.M_HOST_RESOURCE_UTILIZATION";

}
