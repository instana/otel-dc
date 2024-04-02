/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb.impl.mssql;

public class MssqlUtil {


    //TODO



    public static final String DB_NAME_VERSION_SQL = "select db_name(),@@VERSION;";
//
    public static final String INSTANCE_COUNT_SQL = "SELECT count(1) FROM sys.databases";
    public static final String INSTANCE_ACTIVE_COUNT_SQL = "SELECT count(1) FROM sys.databases WHERE state_desc = 'ONLINE';";
//
//    public static final String SESSION_COUNT_SQL = "SELECT a.user_connections from (select DB_NAME(database_id) as db_name, count(*) as user_connections FROM sys.dm_exec_connections AS c JOIN sys.dm_exec_sessions AS s ON c.session_id = s.session_id GROUP BY database_id) a, (SELECT CAST(value AS int) AS maximum_connections FROM sys.configurations WHERE configuration_id = 103)b";
//    public static final String SESSION_ACTIVE_COUNT_SQL = "SELECT a.user_connections from (select DB_NAME(database_id) as db_name, count(*) as user_connections FROM sys.dm_exec_connections AS c JOIN sys.dm_exec_sessions AS s ON c.session_id = s.session_id GROUP BY database_id) a,(SELECT CAST(value AS int) AS maximum_connections FROM sys.configurations WHERE configuration_id = 103)b";
//    public static final String TRANSACTION_COUNT_SQL = "SELECT count(1) FROM sys.dm_tran_database_transactions";
//    public static final String TRANSACTION_LATENCY_SQL = "SELECT count(1)/60 FROM sys.dm_tran_database_transactions";
//    public static final String SQL_COUNT_SQL = "SELECT COUNT(1) FROM sys.dm_exec_query_stats";
//    public static final String IO_READ_COUNT_SQL = "SELECT SUM(num_of_bytes_read) num_of_bytes_read  FROM sys.dm_io_virtual_file_stats(NULL, NULL)";
//    public static final String IO_WRITE_COUNT_SQL = "SELECT SUM(num_of_bytes_written) as num_of_bytes_written FROM sys.dm_io_virtual_file_stats(NULL, NULL)";
//    public static final String TASK_WAIT_COUNT_SQL = "SELECT sum(waiting_tasks_count) FROM sys.dm_os_wait_stats";
//    public static final String TASK_AVG_WAIT_TIME_SQL = "SELECT avg(wait_time_ms)/1000 FROM sys.dm_os_wait_stats";
//    public static final String CACHE_HIT_SQL = "SELECT (a.cntr_value * 1.0 / b.cntr_value) * 100.0 as rate,a.object_name as name FROM sys.dm_os_performance_counters  a JOIN (SELECT cntr_value,OBJECT_NAME FROM sys.dm_os_performance_counters WHERE counter_name = 'Buffer cache hit ratio base' AND OBJECT_NAME LIKE '%Buffer Manager%') b ON  a.OBJECT_NAME = b.OBJECT_NAME WHERE a.counter_name = 'Buffer cache hit ratio' AND a.OBJECT_NAME LIKE '%Buffer Manager%'";
//


}
