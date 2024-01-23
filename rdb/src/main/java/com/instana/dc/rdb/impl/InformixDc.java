/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb.impl;

import com.instana.dc.CalculationMode;
import com.instana.dc.rdb.AbstractDbDc;
import com.instana.dc.rdb.DbDcUtil;
import com.instana.dc.rdb.util.InformixUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.instana.agent.sensorsdk.semconv.SemanticAttributes.SQL_TEXT;
import static com.instana.dc.rdb.DbDcUtil.DB_TABLESPACE_MAX_KEY;
import static com.instana.dc.rdb.DbDcUtil.DB_TABLESPACE_SIZE_KEY;
import static com.instana.dc.rdb.DbDcUtil.DB_TABLESPACE_USED_KEY;
import static com.instana.dc.rdb.DbDcUtil.DB_TABLESPACE_UTILIZATION_KEY;
import static com.instana.dc.rdb.DbDcUtil.getMetricWithSql;
import static com.instana.dc.rdb.DbDcUtil.getSimpleMetricWithSql;
import static com.instana.dc.rdb.util.InformixUtil.DB_HOST_AND_VERSION_SQL;


public class InformixDc extends AbstractDbDc {
    private static final Logger LOGGER = Logger.getLogger(InformixDc.class.getName());
    private String tableSpaceSizeQuery;
    private String tableSpaceUsedQuery;
    private String tableSpaceUtilizationQuery;
    private String tableSpaceMaxQuery;

    private boolean customPollRateEnabled = true;

    public InformixDc(Map<String, Object> properties, String dbSystem, String dbDriver) throws ClassNotFoundException, SQLException {
        super(properties, dbSystem, dbDriver);
        parseCustomAttributes(properties);
        parseCustomPollRate(properties);
        Class.forName("com.informix.jdbc.IfxDriver");
        setDbPassword(InformixUtil.decodePassword(getDbPassword()));
        setDbConnUrl();
        getDbNameAndVersion();
        if (getServiceInstanceId() == null) {
            setServiceInstanceId(getDbAddress() + ":" + getDbPort() + "@" + getDbName());
        }
    }

    private void parseCustomPollRate(Map<String, Object> properties) {
        Map<Integer, Object> customInput = (Map<Integer, Object>) properties.get("custom.poll.interval");
        if (customInput.isEmpty()) {
            customPollRateEnabled = false;
            return;
        }

        for (Map.Entry<Integer, Object> entry : customInput.entrySet()) {
            int pollRate = entry.getKey();
            String[] metrics = ((String) entry.getValue()).split(",");
            List<String> queryList = new ArrayList<>();
            for (String metricKey : metrics) {
                queryList.add(InformixUtil.getQueryMap().get(metricKey.trim()));
            }

            schedule(pollRate, queryList);
        }

    }

    private void schedule(int pollRate, List<String> queryList) {
        
    }


    private void parseCustomAttributes(Map<String, Object> properties) {
        Map<String, Object> customInput = (Map<String, Object>) properties.get("custom.input");
        String[] dbNames = ((String) customInput.get("db.names")).split(",");
        StringBuilder sb = new StringBuilder("'" + dbNames[0] + "'");
        for (int i = 1; i < dbNames.length; i++) {
            sb.append(" , ").append("'").append(dbNames[i].trim()).append("'");
        }
        tableSpaceSizeQuery = String.format(InformixUtil.TABLESPACE_SIZE_SQL, sb);
        tableSpaceUsedQuery = String.format(InformixUtil.TABLESPACE_USED_SQL, sb);
        tableSpaceUtilizationQuery = String.format(InformixUtil.TABLESPACE_UTILIZATION_SQL, sb);
        tableSpaceMaxQuery = String.format(InformixUtil.TABLESPACE_MAX_SQL, sb);
    }

    public void setDbConnUrl() {
        String host = getDbAddress();
        long port = getDbPort();
        String serverName = getServerName();
        String user = getDbUserName();
        String password = getDbPassword();

        String url = String.format("jdbc:informix-sqli://%s:%s/sysmaster:informixserver=%s;user=%s;Password=%s",
                host,
                port,
                serverName,
                user,
                password
        );

        setDbConnUrl(url);
    }

    private void getDbNameAndVersion() throws SQLException, ClassNotFoundException {
        try (Connection connection = getConnection()) {
            ResultSet rs = DbDcUtil.executeQuery(connection, DB_HOST_AND_VERSION_SQL);
            rs.next();
            setDbVersion(rs.getString("Version"));
        }
    }


    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getDbConnUrl());
    }

    @Override
    public void registerMetrics() {
        super.registerMetrics();
        getRawMetric(DbDcUtil.DB_TRANSACTION_RATE_NAME).setCalculationMode(CalculationMode.RATE);
        getRawMetric(DbDcUtil.DB_SQL_RATE_NAME).setCalculationMode(CalculationMode.RATE);
        getRawMetric(DbDcUtil.DB_IO_READ_RATE_NAME).setCalculationMode(CalculationMode.RATE);
        getRawMetric(DbDcUtil.DB_IO_WRITE_RATE_NAME).setCalculationMode(CalculationMode.RATE);
    }

    @Override
    public void collectData() {
        LOGGER.info("Start to collect metrics for Informix DB");
        try (Connection con = getConnection()) {

            getRawMetric(DbDcUtil.DB_STATUS_NAME).setValue(1);
            getRawMetric(DbDcUtil.DB_INSTANCE_COUNT_NAME).setValue(getSimpleMetricWithSql(con, InformixUtil.INSTANCE_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_INSTANCE_ACTIVE_COUNT_NAME).setValue(getSimpleMetricWithSql(con, InformixUtil.INSTANCE_ACTIVE_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_SESSION_COUNT_NAME).setValue(getSimpleMetricWithSql(con, InformixUtil.SESSION_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_SESSION_ACTIVE_COUNT_NAME).setValue(getSimpleMetricWithSql(con, InformixUtil.ACTIVE_SESSION));
            getRawMetric(DbDcUtil.DB_IO_READ_RATE_NAME).setValue(getSimpleMetricWithSql(con, InformixUtil.IO_READ_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_IO_WRITE_RATE_NAME).setValue(getSimpleMetricWithSql(con, InformixUtil.IO_WRITE_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_MEM_UTILIZATION_NAME).setValue(getMetricWithSql(con, InformixUtil.MEMORY_UTILIZATION_SQL));

            getRawMetric(DbDcUtil.DB_TABLESPACE_SIZE_NAME).setValue(getMetricWithSql(con, tableSpaceSizeQuery, DB_TABLESPACE_SIZE_KEY));
            getRawMetric(DbDcUtil.DB_TABLESPACE_USED_NAME).setValue(getMetricWithSql(con, tableSpaceUsedQuery, DB_TABLESPACE_USED_KEY));
            getRawMetric(DbDcUtil.DB_TABLESPACE_UTILIZATION_NAME).setValue(getMetricWithSql(con, tableSpaceUtilizationQuery, DB_TABLESPACE_UTILIZATION_KEY));
            getRawMetric(DbDcUtil.DB_TABLESPACE_MAX_NAME).setValue(getMetricWithSql(con, tableSpaceMaxQuery, DB_TABLESPACE_MAX_KEY));

            getRawMetric(DbDcUtil.DB_SQL_COUNT_NAME).setValue(getSimpleMetricWithSql(con, InformixUtil.SQL_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_SQL_RATE_NAME).setValue(getSimpleMetricWithSql(con, InformixUtil.SQL_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_TRANSACTION_COUNT_NAME).setValue(getSimpleMetricWithSql(con, InformixUtil.TRANSACTION_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_TRANSACTION_RATE_NAME).setValue(getSimpleMetricWithSql(con, InformixUtil.TRANSACTION_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_SQL_ELAPSED_TIME_NAME).setValue(getMetricWithSql(con, InformixUtil.SQL_ELAPSED_TIME_SQL, DbDcUtil.DB_SQL_ELAPSED_TIME_KEY, SQL_TEXT.getKey()));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update metric with exception", e);
            getRawMetric(DbDcUtil.DB_STATUS_NAME).setValue(0);
        }
    }

    @Override
    public void start() {
        LOGGER.info("Starting Informix Scheduler");
        //exec.scheduleWithFixedDelay(this::collectData, 1, pollInterval, TimeUnit.SECONDS);
        if (customPollRateEnabled) {
            return;
        }
        super.start();
    }
}
