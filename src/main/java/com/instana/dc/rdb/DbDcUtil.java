/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb;

import com.instana.dc.SimpleQueryResult;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.instana.agent.sensorsdk.semconv.SemanticAttributes.*;

public class DbDcUtil {
    private static final Logger logger = Logger.getLogger(DbDcUtil.class.getName());

    /* Configurations for the Data Collector:
     */
    public static final String DB_SYSTEM = "db.system";
    public static final String DB_DRIVER = "db.driver";
    public static final String DB_ADDRESS = "db.address";
    public static final String DB_PORT = "db.port";
    public static final String DB_USERNAME = "db.username";
    public static final String DB_PASSWORD = "db.password";
    public static final String DB_CONN_URL = "db.connection.url";
    public static final String DB_ENTITY_TYPE = "db.entity.type";
    public static final String DEFAULT_DB_ENTITY_TYPE = "DATABASE";
    public static final String DB_NAME = "db.name";
    public static final String DB_VERSION = "db.version";

    public static final String DEFAULT_INSTRUMENTATION_SCOPE = "instana.sensor-sdk.dc.db";
    public static final String DEFAULT_INSTRUMENTATION_SCOPE_VER = "1.0.0";

    /* Configurations for Metrics:
     */
    public static final String UNIT_S = "s";
    public static final String UNIT_BY = "By";
    public static final String UNIT_1 = "1";

    public static final String DB_STATUS_NAME = DB_STATUS.getKey();
    public static final String DB_STATUS_DESC = "The status of the database";
    public static final String DB_STATUS_UNIT = "{status}";

    public static final String DB_INSTANCE_COUNT_NAME = DB_INSTANCE_COUNT.getKey();
    public static final String DB_INSTANCE_COUNT_DESC = "The total number of db instances";
    public static final String DB_INSTANCE_COUNT_UNIT = "{instance}";

    public static final String DB_INSTANCE_ACTIVE_COUNT_NAME = DB_INSTANCE_ACTIVE_COUNT.getKey();
    public static final String DB_INSTANCE_ACTIVE_COUNT_DESC = "The total number of active db instances";
    public static final String DB_INSTANCE_ACTIVE_COUNT_UNIT = "{instance}";

    public static final String DB_SESSION_COUNT_NAME = DB_SESSION_COUNT.getKey();
    public static final String DB_SESSION_COUNT_DESC = "Number of sessions";
    public static final String DB_SESSION_COUNT_UNIT = "{session}";

    public static final String DB_SESSION_ACTIVE_COUNT_NAME = DB_SESSION_ACTIVE_COUNT.getKey();
    public static final String DB_SESSION_ACTIVE_COUNT_DESC = "The number of active database sessions";
    public static final String DB_SESSION_ACTIVE_COUNT_UNIT = "{session}";

    public static final String DB_TRANSACTION_COUNT_NAME = DB_TRANSACTION_COUNT.getKey();
    public static final String DB_TRANSACTIONS_COUNT_DESC = "The number of completed transactions";
    public static final String DB_TRANSACTION_COUNT_UNIT = "{transaction}";

    public static final String DB_TRANSACTION_RATE_NAME = DB_TRANSACTION_RATE.getKey();
    public static final String DB_TRANSACTION_RATE_DESC = "The number of transactions per second";
    public static final String DB_TRANSACTION_RATE_UNIT = "{transaction}";

    public static final String DB_TRANSACTION_LATENCY_NAME = DB_TRANSACTION_LATENCY.getKey();
    public static final String DB_TRANSACTION_LATENCY_DESC = "The average transaction latency";

    public static final String DB_SQL_COUNT_NAME = DB_SQL_COUNT.getKey();
    public static final String DB_SQL_COUNT_DESC = "The number of SQLs";
    public static final String DB_SQL_COUNT_UNIT = "{sql}";

    public static final String DB_SQL_RATE_NAME = DB_SQL_RATE.getKey();
    public static final String DB_SQL_RATE_DESC = "The number of SQL per second";
    public static final String DB_SQL_RATE_UNIT = "{sql}";

    public static final String DB_SQL_LATENCY_NAME = DB_SQL_LATENCY.getKey();
    public static final String DB_SQL_LATENCY_DESC = "The average SQL latency";

    public static final String DB_IO_READ_RATE_NAME = DB_IO_READ_RATE.getKey();
    public static final String DB_IO_READ_RATE_DESC = "The physical read per second";

    public static final String DB_IO_WRITE_RATE_NAME = DB_IO_WRITE_RATE.getKey();
    public static final String DB_IO_WRITE_RATE_DESC = "The physical write per second";

    public static final String DB_TASK_WAIT_COUNT_NAME = DB_TASK_WAIT_COUNT.getKey();
    public static final String DB_TASK_WAIT_COUNT_DESC = "The number of waiting task";
    public static final String DB_TASK_WAIT_COUNT_UNIT = "{tasks}";

    public static final String DB_TASK_AVG_WAIT_TIME_NAME = DB_TASK_AVG_WAIT_TIME.getKey();
    public static final String DB_TASK_AVG_WAIT_TIME_DESC = "Average task wait time";

    public static final String DB_CACHE_HIT_NAME = DB_CACHE_HIT.getKey();
    public static final String DB_CACHE_HIT_DESC = "The cache hit ratio/percentage";
    public static final String DB_CACHE_HIT_KEY = TYPE.getKey();

    public static final String DB_SQL_ELAPSED_TIME_NAME = DB_SQL_ELAPSED_TIME.getKey();
    public static final String DB_SQL_ELAPSED_TIME_DESC = "The elapsed time in second of the query";
    public static final String DB_SQL_ELAPSED_TIME_KEY = SQL_ID.getKey();

    public static final String DB_LOCK_TIME_NAME = DB_LOCK_TIME.getKey();
    public static final String DB_LOCK_TIME_DESC = "The lock elapsed time";
    public static final String DB_LOCK_TIME_KEY = LOCK_ID.getKey();

    public static final String DB_LOCK_COUNT_NAME = DB_LOCK_COUNT.getKey();
    public static final String DB_LOCK_COUNT_DESC = "The number of database locks";
    public static final String DB_LOCK_COUNT_UNIT = "{lock}";
    public static final String DB_LOCK_COUNT_KEY = TYPE.getKey();

    public static final String DB_TABLESPACE_SIZE_NAME = DB_TABLESPACE_SIZE.getKey();
    public static final String DB_TABLESPACE_SIZE_DESC = "The size (in bytes) of the tablespace";
    public static final String DB_TABLESPACE_SIZE_KEY = TABLESPACE_NAME.getKey();

    public static final String DB_TABLESPACE_USED_NAME = DB_TABLESPACE_USED.getKey();
    public static final String DB_TABLESPACE_USED_DESC = "The used size (in bytes) of the tablespace";
    public static final String DB_TABLESPACE_USED_KEY = TABLESPACE_NAME.getKey();

    public static final String DB_TABLESPACE_UTILIZATION_NAME = DB_TABLESPACE_UTILIZATION.getKey();
    public static final String DB_TABLESPACE_UTILIZATION_DESC = "The used percentage of the tablespace";
    public static final String DB_TABLESPACE_UTILIZATION_KEY = TABLESPACE_NAME.getKey();

    public static final String DB_TABLESPACE_MAX_NAME = DB_TABLESPACE_MAX.getKey();
    public static final String DB_TABLESPACE_MAX_DESC = "The max size (in bytes) of the tablespace";
    public static final String DB_TABLESPACE_MAX_KEY = TABLESPACE_NAME.getKey();

    public static final String DB_CPU_UTILIZATION_NAME = DB_CPU_UTILIZATION.getKey();
    public static final String DB_CPU_UTILIZATION_DESC = "The percentage of used CPU";

    public static final String DB_MEM_UTILIZATION_NAME = DB_MEM_UTILIZATION.getKey();
    public static final String DB_MEM_UTILIZATION_DESC = "The percentage of used memory on the file system";

    public static final String DB_DISK_UTILIZATION_NAME = DB_DISK_UTILIZATION.getKey();
    public static final String DB_DISK_UTILIZATION_DESC = "The percentage of used disk space on the file system";
    public static final String DB_DISK_UTILIZATION_KEY = PATH.getKey();

    public static final String DB_DISK_USAGE_NAME = DB_DISK_USAGE.getKey();
    public static final String DB_DISK_USAGE_DESC = "The size (in bytes) of the used disk space on the file system";
    public static final String DB_DISK_USAGE_KEY = PATH.getKey();

    public static final String DB_BACKUP_CYCLE_NAME = DB_BACKUP_CYCLE.getKey();
    public static final String DB_BACKUP_CYCLE_DESC = "Backup cycle";


    /* Utilities:
     **/
    public static ResultSet executeQuery(Connection connection, String query) throws SQLException {
        Statement statement = connection.createStatement();
        return statement.executeQuery(query);
    }

    public static Number getSimpleMetricWithSql(Connection connection, String queryStr) {
        try {
            ResultSet rs = executeQuery(connection, queryStr);
            if (rs.isClosed()) {
                logger.severe("getSimpleMetricWithSql: ResultSet is closed");
                return null;
            }
            if (rs.next()) {
                return (Number) rs.getObject(1);
            } else {
                logger.log(Level.WARNING, "getSimpleMetricWithSql: No result");
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "getSimpleMetricWithSql: Error occurred", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getSimpleListWithSql(Connection connection, String queryStr) {
        try {
            ResultSet rs = executeQuery(connection, queryStr);
            if (rs.isClosed()) {
                logger.severe("getSimpleObjectWithSql: ResultSet is closed");
                return null;
            }
            List<T> list = new ArrayList<>();
            int nColumn = rs.getMetaData().getColumnCount();
            if (rs.next()) {
                for (int i = 1; i <= nColumn; i++) {
                    list.add((T) rs.getObject(i));
                }
                return list;
            } else {
                logger.log(Level.WARNING, "getSimpleObjectWithSql: No result");
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "getSimpleObjectWithSql: Error occurred", e);
            return null;
        }
    }

    public static List<SimpleQueryResult> getMetricWithSql(Connection connection, String queryStr, String... attrs) {
        List<SimpleQueryResult> results = new ArrayList<>();
        try {
            ResultSet rs = executeQuery(connection, queryStr);
            if (rs.isClosed()) {
                logger.severe("getMetricWithSql: ResultSet is closed");
                return null;
            }
            while (rs.next()) {
                int n = 1;
                SimpleQueryResult result = new SimpleQueryResult((Number) rs.getObject(n));
                for (String attr : attrs) {
                    n++;
                    Object obj = rs.getObject(n);
                    if (obj == null) {
                        obj = "null";
                    }
                    result.getAttributes().put(attr, obj);
                    if (n == 2) {
                        result.setKey(obj.toString());
                    }
                }
                results.add(result);
            }
            return results;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "getMetricWithSql: Error occurred", e);
            return null;
        }
    }

}