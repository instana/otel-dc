/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb.impl;

public class Oceanbase4Util {
    public static final String TYPE_CLUSTER = "CLUSTER";
    public static final String TYPE_TENANT = "TENANT";
    public static final String TENANT_HOLDER = "XXXX";

    public static final String DB_VERSION_SQL = "select @@version";
    public static final String DB_TENANT_ID2NAME_SQL = "select tenant_name from __all_tenant where tenant_id='XXXX'";
    public static final String DB_TENANT_NAME2ID_SQL = "select tenant_id from __all_tenant where tenant_name='XXXX'";

    public static final String INSTANCE_COUNT_SQL = "select count(1) from __all_tenant";
    public static final String INSTANCE_ACTIVE_COUNT_SQL = "select count(1) from __all_tenant where locked=0";

    public static final String SESSION_COUNT_SQL0 = "SELECT SUM(cnt) AS total_cnt FROM ( SELECT COUNT(1) AS cnt FROM GV$OB_PROCESSLIST WHERE tenant IN ( SELECT tenant_name FROM DBA_OB_TENANTS WHERE tenant_type <> 'META' ) );";
    public static final String SESSION_COUNT_SQL1 = "select case when cnt is null then 0 else cnt end as cnt from (select cnt from DBA_OB_TENANTS left join (select count(1) as cnt, tenant as tenant_name from GV$OB_PROCESSLIST group by tenant) t1 on DBA_OB_TENANTS.tenant_name = t1.tenant_name where DBA_OB_TENANTS.tenant_type<>'META' and DBA_OB_TENANTS.tenant_id = 'XXXX')";
    public static final String SESSION_ACTIVE_COUNT_SQL0 = "SELECT SUM(cnt) AS total_cnt FROM ( select cnt from DBA_OB_TENANTS left join (select count(`state`='ACTIVE' OR NULL) as cnt, tenant as tenant_name from GV$OB_PROCESSLIST group by tenant) t1 on DBA_OB_TENANTS.tenant_name = t1.tenant_name where DBA_OB_TENANTS.tenant_type<>'META' )";
    public static final String SESSION_ACTIVE_COUNT_SQL1 = "select case when cnt is null then 0 else cnt end as cnt from (select cnt from DBA_OB_TENANTS left join (select count(`state`='ACTIVE' OR NULL) as cnt, tenant as tenant_name from GV$OB_PROCESSLIST group by tenant) t1 on DBA_OB_TENANTS.tenant_name = t1.tenant_name where DBA_OB_TENANTS.tenant_type<>'META' and DBA_OB_TENANTS.tenant_id = 'XXXX')";
    public static final String TRANSACTION_COUNT_SQL0 = "select SUM(value) AS total_value from v$sysstat where stat_id IN (30007,30009,30011) and (con_id > 1000 or con_id = 1) and class < 1000";
    public static final String TRANSACTION_COUNT_SQL1 = "select SUM(value) AS value from v$sysstat where con_id = XXXX and stat_id IN (30007,30009,30011) and (con_id > 1000 or con_id = 1) and class < 1000";
    public static final String TRANSACTION_LATENCY_SQL0 = "SELECT SUM(value)/1000000/ ( SELECT SUM(value) FROM v$sysstat WHERE stat_id IN (30007,30009,30011) AND (con_id > 1000 OR con_id = 1) AND class < 1000 ) AS total_latency_value FROM v$sysstat WHERE stat_id = 30006 AND (con_id > 1000 OR con_id = 1) AND class < 1000";
    public static final String TRANSACTION_LATENCY_SQL1 = "SELECT SUM(t1.total_time) / t2.totals AS tenant_latency_value FROM ( SELECT con_id AS tenant_id, sum(value) / 1000000 AS total_time FROM v$sysstat WHERE con_id = XXXX and stat_id = 30006 AND (con_id > 1000 OR con_id = 1) AND class < 1000) t1 JOIN ( SELECT con_id AS tenant_id, SUM(value) AS totals FROM v$sysstat WHERE con_id = XXXX and stat_id IN (30007, 30009, 30011) AND (con_id > 1000 OR con_id = 1) AND class < 1000) t2 ON t1.tenant_id = t2.tenant_id";
    public static final String SQL_COUNT_SQL0 = "select SUM(value) AS total_value from v$sysstat where stat_id IN (40000, 40002, 40004, 40006, 40008, 40018) and (con_id > 1000 or con_id = 1) and class < 1000";
    public static final String SQL_COUNT_SQL1 = "select sum(value) from v$sysstat where con_id = XXXX and  stat_id IN (40000, 40002, 40004, 40006, 40008, 40018) and (con_id > 1000 or con_id = 1) and class < 1000 group by con_id";
    public static final String IO_READ_COUNT_SQL0 = "select SUM(value) AS total_value from v$sysstat where stat_id IN (40000, 40002, 40004, 40006, 40008, 40018) and (con_id > 1000 or con_id = 1) and class < 1000";
    public static final String IO_READ_COUNT_SQL1 = "select value from v$sysstat where con_id = XXXX and stat_id IN (60000) and (con_id > 1000 or con_id = 1) and class < 1000";
    public static final String IO_WRITE_COUNT_SQL0 = "select SUM(value) AS total_value  from v$sysstat where stat_id IN (60003) and (con_id > 1000 or con_id = 1) and class < 1000";
    public static final String IO_WRITE_COUNT_SQL1 = "select value from v$sysstat where con_id = XXXX and stat_id IN (60003) and (con_id > 1000 or con_id = 1) and class < 1000";
}
