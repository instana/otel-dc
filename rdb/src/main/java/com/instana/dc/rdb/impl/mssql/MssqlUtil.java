/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb.impl.mssql;

public class MssqlUtil {


    //TODO



    public static final String DB_NAME_VERSION_SQL = "select db_name(),@@VERSION;";

    public static final String INSTANCE_COUNT_SQL = "SELECT count(1) FROM sys.databases";
    public static final String INSTANCE_ACTIVE_COUNT_SQL = "SELECT count(1) FROM sys.databases WHERE state_desc = 'ONLINE';";


}
