/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix;

import java.util.Base64;

public class InformixUtil {
    private InformixUtil() {
        //Private Constructor
    }

    public static final String DB_HOST_AND_VERSION_SQL = "SELECT  FIRST 1 DBINFO('version','full') AS Version,  DBINFO('dbhostname') AS HostName FROM SYSTABLES;";
    public static final String AVAILABLE_DATA_BASES = "SELECT name, owner, partnum  FROM SYSDATABASES";

    //Instance & Active Instance information (KPI)
    public static final String INSTANCE_COUNT_SQL = "SELECT COUNT(DISTINCT DBSERVERNAME) AS SERVER FROM SYSTABLES;";
    public static final String INSTANCE_ACTIVE_COUNT_SQL = "SELECT COUNT(DISTINCT name) AS ACTIVE_SERVER FROM SYSCLUSTER WHERE server_status = 'Active';";

    //Session & Active Session information (KPI)
    public static final String ACTIVE_SESSION = "SELECT COUNT(1) FROM SYSSESSIONS;";
    public static final String SESSION_COUNT_SQL = "SELECT COUNT(1) FROM SYSSESSIONS;";


    //I/O Read & Write information (KPI) : As number of Reads/Writes PER CHUNK hence doing the SUM
    public static final String IO_READ_COUNT_SQL = "SELECT SUM(SYSCHUNKS.pagesize * SYSCHKIO.pagesread) FROM SYSCHKIO INNER JOIN SYSCHUNKS ON SYSCHUNKS.chknum=SYSCHKIO.chunknum;";
    public static final String IO_WRITE_COUNT_SQL = "SELECT SUM(SYSCHUNKS.pagesize * SYSCHKIO.pageswritten) FROM SYSCHKIO INNER JOIN SYSCHUNKS ON SYSCHUNKS.chknum=SYSCHKIO.chunknum;";

    public static final String MEMORY_UTILIZATION_SQL = "SELECT (SUM(seg_blkused) * 100) / (SUM(seg_blkused) + SUM(seg_blkfree)) FROM SYSSEGLST;";

    public static final String SQL_COUNT_SQL = "SELECT COUNT(1) FROM SYSSQLTRACE WHERE (dbinfo('utc_current') - sql_finishtime)<24*60*60;";
    public static final String TRANSACTION_COUNT_SQL = "SELECT COUNT(1) FROM SYSTRANS;";
    public static final String SQL_ELAPSED_TIME_SQL = "SELECT sql_runtime*1000 AS ELAPSED_TIME_MILLIS, sql_id AS sql_id, sql_statement AS sql_text FROM SYSMASTER:SYSSQLTRACE  WHERE sql_statement NOT LIKE '%syssqltrace%' ORDER BY sql_runtime DESC LIMIT 20;";
    //Table Space Queries
    public static final String TABLESPACE_SIZE_SQL = "SELECT(pt.nptotal * pt.pagesize)  * 1024 AS total_kb,tabname FROM SYSMASTER:SYSPTNHDR pt INNER JOIN SYSMASTER:SYSTABNAMES tn ON tn.partnum = pt.partnum WHERE (tn.dbsname IN ( %s )) ORDER BY tabname DESC LIMIT 40;";
    public static final String TABLESPACE_USED_SQL = "SELECT(pt.npused  * pt.pagesize)  * 1024 AS used_kb,tabname FROM SYSMASTER:SYSPTNHDR pt INNER JOIN SYSMASTER:SYSTABNAMES tn ON tn.partnum = pt.partnum WHERE (tn.dbsname IN (%s)) ORDER BY tabname DESC LIMIT 40;";
    public static final String TABLESPACE_UTILIZATION_SQL = "SELECT CASE WHEN (pt.nptotal > 0) THEN ((pt.npused) /pt.nptotal) * 100 ELSE 0 END AS table_utilization, tabname FROM SYSMASTER:SYSPTNHDR pt INNER JOIN SYSMASTER:SYSTABNAMES tn ON tn.partnum = pt.partnum WHERE (tn.dbsname IN ('instana')) ORDER BY tabname DESC LIMIT 40;";
    public static final String TABLESPACE_MAX_SQL = "SELECT(pt.nptotal * pt.pagesize)  * 1024 AS total_kb, tabname FROM SYSMASTER:SYSPTNHDR pt INNER JOIN SYSMASTER:SYSTABNAMES tn ON tn.partnum = pt.partnum WHERE (tn.dbsname IN ('instana')) ORDER BY tabname DESC LIMIT 40;";

    public static final String DB_DISK_WRITE_COUNT_SQL = "SELECT value FROM SYSPROFILE WHERE name = 'dskwrites';";
    public static final String DB_DISK_READ_COUNT_SQL = "SELECT value FROM SYSPROFILE WHERE name = 'dskreads';";

    public static final String DB_DATABASE_LOG_ENABLED_SQL = "SELECT is_logging, name AS database_name FROM SYSDATABASES;";
    public static final String DB_DATABASE_BUFF_LOG_ENABLED_SQL = "SELECT is_buff_log, name AS database_name FROM SYSDATABASES;";
    public static final String DB_DATABASE_ANSI_COMPLAINT_SQL = "SELECT is_ansi, name AS database_name FROM SYSDATABASES;";
    public static final String DB_DATABASE_NLS_ENABLED_SQL = "SELECT is_nls, name AS database_name FROM SYSDATABASES;";
    public static final String DB_DATABASE_CASE_INCENSITIVE_SQL = "SELECT is_case_insens, name AS database_name FROM SYSDATABASES;";
    public static String decodePassword(String encodedPwd) {
        return new String(Base64.getDecoder().decode(encodedPwd));
    }
}
