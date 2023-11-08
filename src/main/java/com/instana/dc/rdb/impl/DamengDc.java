/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb.impl;

import com.instana.dc.CalculationMode;
import com.instana.dc.rdb.AbstractDbDc;
import com.instana.dc.rdb.DbDcUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.instana.agent.sensorsdk.semconv.SemanticAttributes.*;
import static com.instana.dc.rdb.DbDcUtil.*;
import static com.instana.dc.rdb.impl.DamengUtil.*;

public class DamengDc extends AbstractDbDc {
    private static final Logger logger = Logger.getLogger(DamengDc.class.getName());

    public DamengDc(Map<String, String> properties, String dbSystem, String dbDriver) throws SQLException {
        super(properties, dbSystem, dbDriver);
        setDbPassword(DamengUtil.decodePassword(getDbPassword()));
        getDbNameAndVersion();
        if (getServiceInstanceId() == null) {
            setServiceInstanceId(getDbAddress() + ":" + getDbPort() + "@" + getDbName());
        }
    }

    private void getDbNameAndVersion() throws SQLException {
        try (Connection connection = getConnection()) {
            ResultSet rs = DbDcUtil.executeQuery(connection, DB_NAME_VERSION_SQL);
            rs.next();
            if (getDbName() == null)
                setDbName(rs.getString(1));
            setDbVersion(rs.getString(2));
        }
    }

    @Override
    public void initOnce() throws ClassNotFoundException {
        Class.forName(getDbDriver());
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getDbConnUrl(), getDbUserName(), getDbPassword());
    }

    @Override
    public void registerMetrics() {
        super.registerMetrics();
        getRawMetric(DB_TRANSACTION_RATE_NAME).setCalculationMode(CalculationMode.RATE);
        getRawMetric(DB_SQL_RATE_NAME).setCalculationMode(CalculationMode.RATE);
        getRawMetric(DB_IO_READ_RATE_NAME).setCalculationMode(CalculationMode.RATE);
        getRawMetric(DB_IO_WRITE_RATE_NAME).setCalculationMode(CalculationMode.RATE);
    }

    @Override
    public void collectData() {
        logger.info("Start to collect metrics");
        try (Connection con = getConnection()) {
            getRawMetric(DB_STATUS_NAME).setValue(1);
            getRawMetric(DB_INSTANCE_COUNT_NAME).setValue(getSimpleMetricWithSql(con, INSTANCE_COUNT_SQL));
            getRawMetric(DB_INSTANCE_ACTIVE_COUNT_NAME).setValue(getSimpleMetricWithSql(con, INSTANCE_ACTIVE_COUNT_SQL));

            getRawMetric(DB_SESSION_COUNT_NAME).setValue(getSimpleMetricWithSql(con, SESSION_COUNT_SQL));
            getRawMetric(DB_SESSION_ACTIVE_COUNT_NAME).setValue(getSimpleMetricWithSql(con, SESSION_ACTIVE_COUNT_SQL));
            getRawMetric(DB_TRANSACTION_COUNT_NAME).setValue(getSimpleMetricWithSql(con, TRANSACTION_COUNT_SQL));
            getRawMetric(DB_TRANSACTION_RATE_NAME).setValue(getSimpleMetricWithSql(con, TRANSACTION_COUNT_SQL));
            getRawMetric(DB_TRANSACTION_LATENCY_NAME).setValue(getSimpleMetricWithSql(con, TRANSACTION_LATENCY_SQL));
            getRawMetric(DB_SQL_COUNT_NAME).setValue(getSimpleMetricWithSql(con, SQL_COUNT_SQL));
            getRawMetric(DB_SQL_RATE_NAME).setValue(getSimpleMetricWithSql(con, SQL_COUNT_SQL));
            getRawMetric(DB_IO_READ_RATE_NAME).setValue(getSimpleMetricWithSql(con, IO_READ_COUNT_SQL));
            getRawMetric(DB_IO_WRITE_RATE_NAME).setValue(getSimpleMetricWithSql(con, IO_WRITE_COUNT_SQL));
            getRawMetric(DB_TASK_WAIT_COUNT_NAME).setValue(getSimpleMetricWithSql(con, TASK_WAIT_COUNT_SQL));
            getRawMetric(DB_TASK_AVG_WAIT_TIME_NAME).setValue(getSimpleMetricWithSql(con, TASK_AVG_WAIT_TIME_SQL));

            getRawMetric(DB_CACHE_HIT_NAME).setValue(getMetricWithSql(con, CACHE_HIT_SQL, DB_CACHE_HIT_KEY));
            getRawMetric(DB_SQL_ELAPSED_TIME_NAME).setValue(getMetricWithSql(con, SQL_ELAPSED_TIME_SQL, DB_SQL_ELAPSED_TIME_KEY, SQL_TEXT.getKey()));
            getRawMetric(DB_LOCK_COUNT_NAME).setValue(getMetricWithSql(con, LOCK_COUNT_SQL, DB_LOCK_COUNT_KEY));
            getRawMetric(DB_LOCK_TIME_NAME).setValue(getMetricWithSql(con, LOCK_TIME_SQL, DB_LOCK_TIME_KEY, BLOCKING_SESS_ID.getKey(), BLOCKER_SESS_ID.getKey(), LOCKED_OBJ_NAME.getKey()));

            getRawMetric(DB_TABLESPACE_SIZE_NAME).setValue(getMetricWithSql(con, TABLESPACE_SIZE_SQL, DB_TABLESPACE_SIZE_KEY));
            getRawMetric(DB_TABLESPACE_USED_NAME).setValue(getMetricWithSql(con, TABLESPACE_USED_SQL, DB_TABLESPACE_USED_KEY));
            getRawMetric(DB_TABLESPACE_UTILIZATION_NAME).setValue(getMetricWithSql(con, TABLESPACE_UTILIZATION_SQL, DB_TABLESPACE_UTILIZATION_KEY));
            getRawMetric(DB_TABLESPACE_MAX_NAME).setValue(getMetricWithSql(con, TABLESPACE_MAX_SQL, DB_TABLESPACE_MAX_KEY));
            getRawMetric(DB_CPU_UTILIZATION_NAME).setValue(getSimpleMetricWithSql(con, CPU_UTILIZATION_SQL));
            List<Long> listMemData = getSimpleListWithSql(con, MEM_UTILIZATION_SQL);
            if (listMemData != null) {
                getRawMetric(DB_MEM_UTILIZATION_NAME).setValue((double) listMemData.get(0) / listMemData.get(1));
            }
            List<Long> listDiskData = getSimpleListWithSql(con, DISK_USAGE_SQL);
            if (listDiskData != null) {
                long free = listDiskData.get(0);
                long total = listDiskData.get(1);
                getRawMetric(DB_DISK_UTILIZATION_NAME).getDataPoint("default").setValue((double) free / total, Collections.singletonMap("path", "default"));
                getRawMetric(DB_DISK_USAGE_NAME).getDataPoint("default").setValue(total - free, Collections.singletonMap("path", "default"));
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update metric with exception", e);
            getRawMetric(DB_STATUS_NAME).setValue(0);
        }
    }
}
