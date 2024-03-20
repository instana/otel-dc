/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb.impl.informix;

import com.instana.agent.sensorsdk.semconv.SemanticAttributes;
import com.instana.dc.CalculationMode;
import com.instana.dc.SimpleQueryResult;
import com.instana.dc.rdb.AbstractDbDc;
import com.instana.dc.rdb.DbDcUtil;
import com.instana.dc.rdb.impl.Constants;
import com.instana.dc.rdb.impl.informix.metric.collection.*;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.instana.dc.rdb.DbDcUtil.*;
import static com.instana.dc.rdb.impl.informix.InformixUtil.DB_HOST_AND_VERSION_SQL;


public class InformixDc extends AbstractDbDc {
    private static final Logger LOGGER = Logger.getLogger(InformixDc.class.getName());
    private String tableSpaceSizeQuery;
    private String tableSpaceUsedQuery;
    private String tableSpaceUtilizationQuery;
    private String tableSpaceMaxQuery;
    private boolean customPollRateEnabled = true;
    private ScheduledExecutorService executorService;
    private final BasicDataSource ds;
    private final OnstatCommandExecutor onstatCommandExecutor;

    private final MetricsCollector metricCollector;

    public InformixDc(Map<String, Object> properties, String dbSystem, String dbDriver) throws SQLException {
        super(properties, dbSystem, dbDriver);
        parseCustomAttributes(properties);
        onstatCommandExecutor = new OnstatCommandExecutor(getDbPath(), getServerName());
        setDbPassword(InformixUtil.decodePassword(getDbPassword()));
        setDbConnUrl();

        ds = new BasicDataSource();
        ds.setDriverClassName(getDbDriver());
        ds.setUsername(getDbUserName());
        ds.setPassword(getDbPassword());
        ds.setUrl(getDbConnUrl());
        ds.setInitialSize(3);
        ds.setMaxIdle(1);
        if (getServiceInstanceId() == null) {
            setServiceInstanceId(getDbAddress() + ":" + getDbPort() + "@" + getDbName());
        }
        getDbNameAndVersion();
        parseCustomPollRate(properties);
        registerMetricsForOperations();
        metricCollector = new MetricsCollector(ds, onstatCommandExecutor);
    }

    private void registerMetricsForOperations() {

        MetricsDataConfigMapping.subscribeMetricDataConfig(DB_TABLESPACE_SIZE_NAME, new MetricDataConfig(tableSpaceSizeQuery, List.class, DB_TABLESPACE_SIZE_KEY));
        MetricsDataConfigMapping.subscribeMetricDataConfig(DB_TABLESPACE_USED_NAME, new MetricDataConfig(tableSpaceUsedQuery, List.class, DB_TABLESPACE_USED_KEY));
        MetricsDataConfigMapping.subscribeMetricDataConfig(DB_TABLESPACE_UTILIZATION_NAME, new MetricDataConfig(tableSpaceUtilizationQuery, List.class, DB_TABLESPACE_UTILIZATION_KEY));
        MetricsDataConfigMapping.subscribeMetricDataConfig(DB_TABLESPACE_MAX_NAME, new MetricDataConfig(tableSpaceMaxQuery, List.class, DB_TABLESPACE_MAX_KEY));
        MetricsDataConfigMapping.subscribeMetricDataConfig(DB_SQL_COUNT_NAME, new MetricDataConfig(InformixUtil.SQL_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_SQL_COUNT_NAME), MetricCollectionMode.CMD, Number.class));
        MetricsDataConfigMapping.subscribeMetricDataConfig(DB_SQL_RATE_NAME, new MetricDataConfig(InformixUtil.SQL_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_SQL_RATE_NAME), MetricCollectionMode.CMD, Number.class));
        MetricsDataConfigMapping.subscribeMetricDataConfig(DB_TRANSACTION_COUNT_NAME, new MetricDataConfig(InformixUtil.TRANSACTION_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_TRANSACTION_COUNT_NAME), MetricCollectionMode.CMD, Number.class));
        MetricsDataConfigMapping.subscribeMetricDataConfig(DB_TRANSACTION_RATE_NAME, new MetricDataConfig(InformixUtil.TRANSACTION_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_TRANSACTION_RATE_NAME), MetricCollectionMode.CMD, Number.class));
        MetricsDataConfigMapping.subscribeMetricDataConfig(DB_SQL_ELAPSED_TIME_NAME, new MetricDataConfig(InformixUtil.SQL_ELAPSED_TIME_SQL, List.class, DB_SQL_ELAPSED_TIME_KEY, SemanticAttributes.SQL_TEXT.getKey()));
        MetricsDataConfigMapping.subscribeMetricDataConfig(DB_INSTANCE_COUNT_NAME, new MetricDataConfig(InformixUtil.INSTANCE_COUNT_SQL, Number.class));
        MetricsDataConfigMapping.subscribeMetricDataConfig(DB_INSTANCE_ACTIVE_COUNT_NAME, new MetricDataConfig(InformixUtil.INSTANCE_ACTIVE_COUNT_SQL, Number.class));
        MetricsDataConfigMapping.subscribeMetricDataConfig(DB_SESSION_COUNT_NAME, new MetricDataConfig(InformixUtil.SESSION_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_SESSION_COUNT_NAME), MetricCollectionMode.CMD, Number.class));
        MetricsDataConfigMapping.subscribeMetricDataConfig(DB_SESSION_ACTIVE_COUNT_NAME, new MetricDataConfig(InformixUtil.ACTIVE_SESSION, CommandLineConstants.getMetricScriptMapping(DB_SESSION_ACTIVE_COUNT_NAME), MetricCollectionMode.CMD, Number.class));
        MetricsDataConfigMapping.subscribeMetricDataConfig(DB_IO_READ_RATE_NAME, new MetricDataConfig(InformixUtil.IO_READ_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_IO_READ_RATE_NAME), MetricCollectionMode.CMD, Number.class));
        MetricsDataConfigMapping.subscribeMetricDataConfig(DB_IO_WRITE_RATE_NAME, new MetricDataConfig(InformixUtil.IO_WRITE_COUNT_SQL, CommandLineConstants.getMetricScriptMapping(DB_IO_WRITE_RATE_NAME), MetricCollectionMode.CMD, Number.class));
        MetricsDataConfigMapping.subscribeMetricDataConfig(DB_MEM_UTILIZATION_NAME, new MetricDataConfig(InformixUtil.MEMORY_UTILIZATION_SQL, CommandLineConstants.getMetricScriptMapping(DB_MEM_UTILIZATION_NAME), MetricCollectionMode.CMD, Number.class));
    }

    /**
     * Util method to parse the user input
     *
     * @param properties : user inputs
     */
    private void parseCustomPollRate(Map<String, Object> properties) {
        Map<String, Object> customInput = (Map<String, Object>) properties.get("custom.poll.interval");
        if (null == customInput || customInput.isEmpty()) {
            customPollRateEnabled = false;
            LOGGER.info("No custom polling interval fallback to default");
            return;
        }

        executorService = Executors.newScheduledThreadPool(3);

        for (Map.Entry<String, Object> entry : customInput.entrySet()) {
            IntervalType type = getPollingInterval(entry.getKey());
            int pollInterval = (int) entry.getValue();
            assert type != null;
            scheduleCustomPollRate(pollInterval, type);
        }
    }


    /**
     * Util method to schedule custom Poll Rate based on the user Input
     *
     * @param pollInterval : Polling value
     * @param intervalType : Type of the Interval
     */
    private void scheduleCustomPollRate(int pollInterval, IntervalType intervalType) {
        switch (intervalType) {
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

    private void setDbConnUrl() {
        String url = String.format("jdbc:informix-sqli://%s:%s/sysmaster:informixserver=%s;user=%s;Password=%s",
                getDbAddress(),
                getDbPort(),
                getServerName(),
                getDbUserName(),
                getDbPassword()
        );
        setDbConnUrl(url);
    }

    private void getDbNameAndVersion() throws SQLException {
        try (Connection connection = ds.getConnection()) {
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
        getRawMetric(DB_TRANSACTION_RATE_NAME).setCalculationMode(CalculationMode.RATE);
        getRawMetric(DB_SQL_RATE_NAME).setCalculationMode(CalculationMode.RATE);
        getRawMetric(DB_IO_READ_RATE_NAME).setCalculationMode(CalculationMode.RATE);
        getRawMetric(DB_IO_WRITE_RATE_NAME).setCalculationMode(CalculationMode.RATE);
    }

    @Override
    public void collectData() {
        LOGGER.info("Start to collect metrics for Informix DB");
        getallMetrics();
    }

    private void getallMetrics() {
        longPollingInterval();
        mediumPollingInterval();
        shortPollingInterval();
    }

    private void mediumPollingInterval() {
        getRawMetric(DB_SQL_COUNT_NAME).setValue((Number) metricCollector.collectMetrics(DB_SQL_COUNT_NAME));
        getRawMetric(DB_SQL_RATE_NAME).setValue((Number) metricCollector.collectMetrics(DB_SQL_RATE_NAME));
        getRawMetric(DB_TRANSACTION_COUNT_NAME).setValue((Number) metricCollector.collectMetrics(DB_TRANSACTION_COUNT_NAME));
        getRawMetric(DB_TRANSACTION_RATE_NAME).setValue((Number) metricCollector.collectMetrics(DB_TRANSACTION_COUNT_NAME));
        getRawMetric(DB_SQL_ELAPSED_TIME_NAME).setValue((List<SimpleQueryResult>) metricCollector.collectMetrics(DB_SQL_ELAPSED_TIME_NAME));

    }

    private void shortPollingInterval() {
        getRawMetric(DbDcUtil.DB_STATUS_NAME).setValue(1);
        getRawMetric(DB_INSTANCE_COUNT_NAME).setValue((Number) metricCollector.collectMetrics(DB_INSTANCE_COUNT_NAME));
        getRawMetric(DB_INSTANCE_ACTIVE_COUNT_NAME).setValue((Number) metricCollector.collectMetrics(DB_INSTANCE_ACTIVE_COUNT_NAME));
        getRawMetric(DB_SESSION_COUNT_NAME).setValue((Number) metricCollector.collectMetrics(DB_SESSION_COUNT_NAME));
        getRawMetric(DB_SESSION_ACTIVE_COUNT_NAME).setValue((Number) metricCollector.collectMetrics(DB_SESSION_ACTIVE_COUNT_NAME));
        getRawMetric(DB_IO_READ_RATE_NAME).setValue((Number) metricCollector.collectMetrics(DB_IO_READ_RATE_NAME));
        getRawMetric(DB_IO_WRITE_RATE_NAME).setValue((Number) metricCollector.collectMetrics(DB_IO_WRITE_RATE_NAME));
        getRawMetric(DB_MEM_UTILIZATION_NAME).setValue((Number) metricCollector.collectMetrics(DB_MEM_UTILIZATION_NAME));
    }

    private void longPollingInterval() {
        getRawMetric(DB_TABLESPACE_SIZE_NAME).setValue((List<SimpleQueryResult>) metricCollector.collectMetrics(DB_TABLESPACE_SIZE_NAME));
        getRawMetric(DB_TABLESPACE_USED_NAME).setValue((List<SimpleQueryResult>) metricCollector.collectMetrics(DB_TABLESPACE_USED_NAME));
        getRawMetric(DB_TABLESPACE_UTILIZATION_NAME).setValue((List<SimpleQueryResult>) metricCollector.collectMetrics(DB_TABLESPACE_UTILIZATION_NAME));
        getRawMetric(DB_TABLESPACE_MAX_NAME).setValue((List<SimpleQueryResult>) metricCollector.collectMetrics(DB_TABLESPACE_MAX_NAME));

    }

    @Override
    public void start() {
        if (customPollRateEnabled) {
            LOGGER.info("Custom Poll Rate is Enabled for InformixDC, not starting default executors");
            return;
        }
        super.start();
    }

    private enum IntervalType {
        HIGH,
        MEDIUM,
        LOW;
    }

    /**
     * Util method to get the Polling Interval
     *
     * @param pollingInterval : User input of the Interval
     * @return : Mapped Type of the Interval
     */
    private IntervalType getPollingInterval(String pollingInterval) {
        for (IntervalType interval : IntervalType.values()) {
            if (pollingInterval.equalsIgnoreCase(interval.name())) {
                return interval;
            }
        }
        LOGGER.log(Level.SEVERE, "Invalid Polling Interval : {}", pollingInterval);
        return null;
    }
}
