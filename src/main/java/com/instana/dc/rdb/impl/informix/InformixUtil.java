/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb.impl.informix;

import java.util.Base64;

public class InformixUtil {

    public static final String DB_HOST_AND_VERSION_SQL = "SELECT  FIRST 1 DBINFO('version','full') as Version,  DBINFO('dbhostname') as HostName FROM systables;";
    public static final String AVAILABLE_DATA_BASES = "select name, owner, partnum  from sysdatabases";
    public static final String AVAILABLE_SERVERS = "SELECT count(distinct DBSERVERNAME) as SERVER FROM systables;";
    public static final String ACTIVE_SESSION = "select count(1) from syssessions;";

    //For number of Reads/Wirtes PER CHUNK
    public static final String IO_READ_COUNT_SQL = "SELECT  SUM(reads) FROM SYSCHKIO";
    public static final String IO_WRITE_COUNT_SQL = "SELECT  SUM(writes) FROM SYSCHKIO";

    public static final String MEMORY_UTILIZATION_SQL = "SELECT (sum(seg_blkused) * 100) / (sum(seg_blkused) + sum(seg_blkfree)) FROM SYSSEGLST;";


    public static String decodePassword(String encodedPwd) {
        return new String(Base64.getDecoder().decode(encodedPwd));
    }
}