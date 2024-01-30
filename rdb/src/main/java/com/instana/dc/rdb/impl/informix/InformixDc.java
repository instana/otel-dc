/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb.impl.informix;

import com.instana.dc.CalculationMode;
import com.instana.dc.rdb.AbstractDbDc;
import com.instana.dc.rdb.DbDcUtil;
import com.instana.dc.rdb.impl.Constants;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
import static com.instana.dc.rdb.impl.informix.InformixUtil.DB_HOST_AND_VERSION_SQL;


public class InformixDc extends AbstractDbDc {
    private static final Logger LOGGER = Logger.getLogger(InformixDc.class.getName());
    private String tableSpaceSizeQuery;
    private String tableSpaceUsedQuery;
    private String tableSpaceUtilizationQuery;
    private String tableSpaceMaxQuery;
    private boolean customPollRateEnabled = true;
    private ScheduledExecutorService executorService;
    final  BasicDataSource ds;


    public InformixDc(Map<String, Object> properties, String dbSystem, String dbDriver) throws ClassNotFoundException, SQLException {
        super(properties, dbSystem, dbDriver);
        parseCustomAttributes(properties);
//        Class.forName("com.informix.jdbc.IfxDriver");
//        setDbPassword(InformixUtil.decodePassword(getDbPassword()));
//        setDbConnUrl();
//        getDbNameAndVersion();
        ds = new BasicDataSource();
        ds.setDriverClassName("com.informix.jdbc.IfxDriver");
        ds.setUsername("informix");
        ds.setPassword("password");
        ds.setUrl("jdbc:informix-sqli://9.46.251.113:9088/sysmaster:INFORMIXSERVER=ol_informix1410");
        if (getServiceInstanceId() == null) {
            setServiceInstanceId(getDbAddress() + ":" + getDbPort() + "@" + getDbName());
        }
        parseCustomPollRate(properties);
    }

    private void parseCustomPollRate(Map<String, Object> properties) {
        Map<String, Object> customInput = (Map<String, Object>) properties.get("custom.poll.interval");
        if (null == customInput || customInput.isEmpty()) {
            customPollRateEnabled = false;
            return;
        }

        executorService = Executors.newScheduledThreadPool(3);

        for (Map.Entry<String, Object> entry : customInput.entrySet()) {
            PollingInterval type = getPollingInterval(entry.getKey());
            int pollInterval = (int) entry.getValue();
            scheduleCustomPollRate(pollInterval, type);
        }
    }

    private PollingInterval getPollingInterval(String pollingInterval) {
        for (PollingInterval interval : PollingInterval.values()) {
            if (pollingInterval.equalsIgnoreCase(interval.name())) {
                return interval;
            }
        }
        LOGGER.log(Level.SEVERE, "Invalid Polling Interval : {}", pollingInterval);
        return null;
    }


    private void scheduleCustomPollRate(int pollInterval, PollingInterval pollingInterval) {
        switch (pollingInterval) {
            case HIGH:
                LOGGER.info("Starting Long Polling Scheduler");
                executorService.scheduleWithFixedDelay(this::longPollingInterval, 1, pollInterval, TimeUnit.SECONDS);
                break;
            case MEDIUM:
                LOGGER.info("Starting Medium Polling Scheduler");
                executorService.scheduleWithFixedDelay(this::mediumPollingInterval, 1, pollInterval, TimeUnit.SECONDS);
                break;
            case LOW:
                LOGGER.info("Starting Low Polling Scheduler");
                executorService.scheduleWithFixedDelay(this::shortPollingInterval, 1, pollInterval, TimeUnit.SECONDS);
                break;
        }
    }




    /**
     * Util method to parse the config and get the custom Attributes from the Config
     *
     * @param properties : Config data
     */
    private void parseCustomAttributes(Map<String, Object> properties) {
        Map<String, Object> customInput = (Map<String, Object>) properties.get("custom.input");
        String[] dbNames = ((String) customInput.get("db.names")).split(",");
        StringBuilder sb = new StringBuilder(Constants.SINGLE_QUOTES + dbNames[0] + Constants.SINGLE_QUOTES);
        for (int i = 1; i < dbNames.length; i++) {
            sb.append(Constants.COMMA).append(Constants.SINGLE_QUOTES).append(dbNames[i].trim()).append(Constants.SINGLE_QUOTES);
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

        String url = String.format("jdbc:informix-sqli://%s:%s/sysmaster:informixserver=%s",
                        //";user=%s;Password=%s",

                host,
                port,
                serverName
//                user,
//                password
        );
        setDbConnUrl(url);
    }

    private void getDbNameAndVersion() throws SQLException {
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
        try (Connection connection = ds.getConnection()) {
            getRawMetric(DbDcUtil.DB_SQL_COUNT_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.SQL_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_SQL_RATE_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.SQL_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_TRANSACTION_COUNT_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.TRANSACTION_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_TRANSACTION_RATE_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.TRANSACTION_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_SQL_ELAPSED_TIME_NAME).setValue(getMetricWithSql(connection, InformixUtil.SQL_ELAPSED_TIME_SQL, DbDcUtil.DB_SQL_ELAPSED_TIME_KEY, SQL_TEXT.getKey()));
            getRawMetric(DbDcUtil.DB_STATUS_NAME).setValue(1);
            getRawMetric(DbDcUtil.DB_INSTANCE_COUNT_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.INSTANCE_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_INSTANCE_ACTIVE_COUNT_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.INSTANCE_ACTIVE_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_SESSION_COUNT_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.SESSION_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_SESSION_ACTIVE_COUNT_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.ACTIVE_SESSION));
            getRawMetric(DbDcUtil.DB_IO_READ_RATE_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.IO_READ_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_IO_WRITE_RATE_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.IO_WRITE_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_MEM_UTILIZATION_NAME).setValue(getMetricWithSql(connection, InformixUtil.MEMORY_UTILIZATION_SQL));
            getRawMetric(DbDcUtil.DB_TABLESPACE_SIZE_NAME).setValue(getMetricWithSql(connection, tableSpaceSizeQuery, DB_TABLESPACE_SIZE_KEY));
            getRawMetric(DbDcUtil.DB_TABLESPACE_USED_NAME).setValue(getMetricWithSql(connection, tableSpaceUsedQuery, DB_TABLESPACE_USED_KEY));
            getRawMetric(DbDcUtil.DB_TABLESPACE_UTILIZATION_NAME).setValue(getMetricWithSql(connection, tableSpaceUtilizationQuery, DB_TABLESPACE_UTILIZATION_KEY));
            getRawMetric(DbDcUtil.DB_TABLESPACE_MAX_NAME).setValue(getMetricWithSql(connection, tableSpaceMaxQuery, DB_TABLESPACE_MAX_KEY));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while retrieving the Informix data for Host: {} ", getServerName());
        }
    }



    private void mediumPollingInterval() {
        try (Connection connection = ds.getConnection()) {
            getRawMetric(DbDcUtil.DB_SQL_COUNT_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.SQL_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_SQL_RATE_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.SQL_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_TRANSACTION_COUNT_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.TRANSACTION_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_TRANSACTION_RATE_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.TRANSACTION_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_SQL_ELAPSED_TIME_NAME).setValue(getMetricWithSql(connection, InformixUtil.SQL_ELAPSED_TIME_SQL, DbDcUtil.DB_SQL_ELAPSED_TIME_KEY, SQL_TEXT.getKey()));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while retrieving the Informix data for Host: {} ", getServerName());
        }
    }

    private void shortPollingInterval() {
       try (Connection connection = ds.getConnection()) {
            getRawMetric(DbDcUtil.DB_STATUS_NAME).setValue(1);
            getRawMetric(DbDcUtil.DB_INSTANCE_COUNT_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.INSTANCE_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_INSTANCE_ACTIVE_COUNT_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.INSTANCE_ACTIVE_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_SESSION_COUNT_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.SESSION_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_SESSION_ACTIVE_COUNT_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.ACTIVE_SESSION));
            getRawMetric(DbDcUtil.DB_IO_READ_RATE_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.IO_READ_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_IO_WRITE_RATE_NAME).setValue(getSimpleMetricWithSql(connection, InformixUtil.IO_WRITE_COUNT_SQL));
            getRawMetric(DbDcUtil.DB_MEM_UTILIZATION_NAME).setValue(getMetricWithSql(connection, InformixUtil.MEMORY_UTILIZATION_SQL));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while retrieving the data : ", e);
        }
    }

    private void longPollingInterval( ) {
        try (Connection connection = ds.getConnection()) {
            getRawMetric(DbDcUtil.DB_TABLESPACE_SIZE_NAME).setValue(getMetricWithSql(connection, tableSpaceSizeQuery, DB_TABLESPACE_SIZE_KEY));
            getRawMetric(DbDcUtil.DB_TABLESPACE_USED_NAME).setValue(getMetricWithSql(connection, tableSpaceUsedQuery, DB_TABLESPACE_USED_KEY));
            getRawMetric(DbDcUtil.DB_TABLESPACE_UTILIZATION_NAME).setValue(getMetricWithSql(connection, tableSpaceUtilizationQuery, DB_TABLESPACE_UTILIZATION_KEY));
            getRawMetric(DbDcUtil.DB_TABLESPACE_MAX_NAME).setValue(getMetricWithSql(connection, tableSpaceMaxQuery, DB_TABLESPACE_MAX_KEY));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while retrieving the data : ", e);
        }
    }

    @Override
    public void start() {
        if (customPollRateEnabled) {
            LOGGER.info("Custom Poll Rate is not Enabled for InformixDC");
            return;
        }
        super.start();
    }

    private enum PollingInterval {
        HIGH,
        MEDIUM,
        LOW;
    }
}
