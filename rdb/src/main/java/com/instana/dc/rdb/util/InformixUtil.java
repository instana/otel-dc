/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.util;

import java.util.Base64;

public class InformixUtil {
    private InformixUtil() {
        //Private Constructor
    }

    public static final String DB_HOST_AND_VERSION_SQL = "SELECT  FIRST 1 DBINFO('version','full') as Version,  DBINFO('dbhostname') as HostName FROM systables;";
    public static final String AVAILABLE_DATA_BASES = "select name, owner, partnum  from sysdatabases";
    //Instance & Active Instance information (KPI)
    public static final String INSTANCE_COUNT_SQL = "SELECT count(distinct DBSERVERNAME) as SERVER FROM systables;";
    public static final String INSTANCE_ACTIVE_COUNT_SQL = "select count(distinct name) as ACTIVE_SERVER from syscluster where server_status = 'Active';";

    //Session & Active Session information (KPI)
    public static final String ACTIVE_SESSION = "select count(1) from syssessions;";
    public static final String SESSION_COUNT_SQL = "select count(1) from syssessions;";


    //I/O Read & Write information (KPI) : As number of Reads/Writes PER CHUNK hence doing the sum
    public static final String IO_READ_COUNT_SQL = "SELECT SUM(syschunks.pagesize * syschkio.pagesread) FROM SYSCHKIO INNER JOIN syschunks ON syschunks.chknum=syschkio.chunknum;";
    public static final String IO_WRITE_COUNT_SQL = "SELECT SUM(syschunks.pagesize * syschkio.pageswritten) FROM SYSCHKIO INNER JOIN syschunks ON syschunks.chknum=syschkio.chunknum;";

    public static final String MEMORY_UTILIZATION_SQL = "SELECT (sum(seg_blkused) * 100) / (sum(seg_blkused) + sum(seg_blkfree)) FROM SYSSEGLST;";

    public static final String SQL_COUNT_SQL = "SELECT count(1) from syssqltrace;";
    public static final String TRANSACTION_COUNT_SQL = "select count(1) from systrans;";
    public static final String SQL_ELAPSED_TIME_SQL = "SELECT sql_runtime*1000 as ELAPSED_TIME_MILLIS, sql_id as sql_id, sql_statement as sql_text FROM sysmaster:syssqltrace  where sql_statement not like '%syssqltrace%' ORDER BY sql_runtime desc limit 20;";
    //Table Space Queries
    public static final String TABLESPACE_SIZE_SQL = "SELECT(pt.nptotal * pt.pagesize)  * 1024 AS total_kb,tabname FROM sysmaster:sysptnhdr pt INNER JOIN sysmaster:systabnames tn ON tn.partnum = pt.partnum where (tn.dbsname in ( %s )) order by tabname desc Limit 40;";
    public static final String TABLESPACE_USED_SQL = "SELECT(pt.npused  * pt.pagesize)  * 1024 AS used_kb,tabname FROM sysmaster:sysptnhdr pt INNER JOIN sysmaster:systabnames tn ON tn.partnum = pt.partnum where (tn.dbsname in ('instana')) order by tabname desc Limit 40;";
    public static final String TABLESPACE_UTILIZATION_SQL = "select case WHEN (pt.nptotal > 0) THEN ((pt.npused) /pt.nptotal) * 100 ELSE 0 END AS table_utilization, tabname FROM sysmaster:sysptnhdr pt INNER JOIN sysmaster:systabnames tn ON tn.partnum = pt.partnum where (tn.dbsname in ('instana')) order by tabname desc Limit 40;";
    public static final String TABLESPACE_MAX_SQL = "SELECT(pt.nptotal * pt.pagesize)  * 1024 AS total_kb, tabname FROM sysmaster:sysptnhdr pt INNER JOIN sysmaster:systabnames tn ON tn.partnum = pt.partnum where (tn.dbsname in ('instana')) order by tabname desc Limit 40;";

    public static String decodePassword(String encodedPwd) {
        return new String(Base64.getDecoder().decode(encodedPwd));
    }
}
