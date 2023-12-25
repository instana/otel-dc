/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb.impl;

import com.instana.dc.CalculationMode;
import com.instana.dc.DcException;
import com.instana.dc.DcUtil;
import com.instana.dc.rdb.AbstractDbDc;
import io.opentelemetry.api.OpenTelemetry;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.instana.agent.sensorsdk.semconv.SemanticAttributes.SQL_TEXT;
import static com.instana.dc.rdb.DbDcUtil.*;
import static com.instana.dc.rdb.impl.Oceanbase4Util.*;

public class Oceanbase4Dc extends AbstractDbDc {
    private static final Logger logger = Logger.getLogger(Oceanbase4Dc.class.getName());

    boolean isCluster = false;
    boolean isTenant = false;

    public Oceanbase4Dc(Map<String, String> properties, String dbSystem, String dbDriver) throws SQLException, DcException {
        super(properties, dbSystem, dbDriver);
        setDbPassword(DcUtil.base64Decode(getDbPassword()));
        if (getServiceInstanceId() == null) {
            setServiceInstanceId(getDbAddress() + ":" + getDbPort() + "@" + getDbName());
        }

        try (Connection conn = getConnection()) {
            setDbVersion(getSimpleStringWithSql(conn, DB_VERSION_SQL));

            if (this.getDbEntityType().equals(TYPE_CLUSTER)) {
                isCluster = true;
                this.setDbTenantId("1");
                this.setDbTenantName("sys");

            } else if (getDbEntityType().equals(TYPE_TENANT)) {
                isTenant = true;
                String tId = getDbTenantId();
                String tName = getDbTenantName();
                if (tId == null) {
                    if (tName == null) {
                        throw new DcException(DB_TENANT_ID + " or " + DB_TENANT_NAME + " must be provided!");
                    }
                    setDbTenantId(getSimpleStringWithSql(conn, DB_TENANT_NAME2ID_SQL.replace(TENANT_HOLDER, tName)));
                } else if (tName == null) {
                    setDbTenantName(getSimpleStringWithSql(conn, DB_TENANT_ID2NAME_SQL.replace(TENANT_HOLDER, tId)));
                }
            } else {
                throw new DcException("Unsupported entity type of Oceanbase");
            }
        }

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
        try (Connection conn = getConnection()) {
            getRawMetric(DB_STATUS_NAME).setValue(1);
            getRawMetric(DB_INSTANCE_COUNT_NAME).setValue(getSimpleMetricWithSql(conn, INSTANCE_COUNT_SQL));
            getRawMetric(DB_INSTANCE_ACTIVE_COUNT_NAME).setValue(getSimpleMetricWithSql(conn, INSTANCE_ACTIVE_COUNT_SQL));

            if (isCluster) {
                getRawMetric(DB_SESSION_COUNT_NAME).setValue(getSimpleMetricWithSql(conn, SESSION_COUNT_SQL0));
                getRawMetric(DB_SESSION_ACTIVE_COUNT_NAME).setValue(getSimpleMetricWithSql(conn, SESSION_ACTIVE_COUNT_SQL0));
                getRawMetric(DB_TRANSACTION_COUNT_NAME).setValue(getSimpleMetricWithSql(conn, TRANSACTION_COUNT_SQL0));
                getRawMetric(DB_TRANSACTION_RATE_NAME).setValue(getSimpleMetricWithSql(conn, TRANSACTION_COUNT_SQL0));
                getRawMetric(DB_TRANSACTION_LATENCY_NAME).setValue(getSimpleMetricWithSql(conn, TRANSACTION_LATENCY_SQL0));
                getRawMetric(DB_SQL_COUNT_NAME).setValue(getSimpleMetricWithSql(conn, SQL_COUNT_SQL0));
                getRawMetric(DB_SQL_RATE_NAME).setValue(getSimpleMetricWithSql(conn, SQL_COUNT_SQL0));
                getRawMetric(DB_IO_READ_RATE_NAME).setValue(getSimpleMetricWithSql(conn, IO_READ_COUNT_SQL0));
                getRawMetric(DB_IO_WRITE_RATE_NAME).setValue(getSimpleMetricWithSql(conn, IO_WRITE_COUNT_SQL0));
                getRawMetric(DB_TASK_WAIT_COUNT_NAME).setValue(getSimpleMetricWithSql(conn, TASK_WAIT_COUNT_SQL0));
                getRawMetric(DB_TASK_AVG_WAIT_TIME_NAME).setValue(getSimpleMetricWithSql(conn, TASK_AVG_WAIT_TIME_SQL0));

                getRawMetric(DB_CACHE_HIT_NAME).setValue(getMetricWithSql(conn, CACHE_HIT_SQL0, DB_CACHE_HIT_KEY));
                getRawMetric(DB_SQL_ELAPSED_TIME_NAME).setValue(getMetricWithSql(conn, SQL_ELAPSED_TIME_SQL0, DB_SQL_ELAPSED_TIME_KEY, SQL_TEXT.getKey()));
            } else if (isTenant) {
                getRawMetric(DB_SESSION_COUNT_NAME).setValue(getSimpleMetricWithSql(conn, withTenant(SESSION_COUNT_SQL1)));
                getRawMetric(DB_SESSION_ACTIVE_COUNT_NAME).setValue(getSimpleMetricWithSql(conn, withTenant(SESSION_ACTIVE_COUNT_SQL1)));
                getRawMetric(DB_TRANSACTION_COUNT_NAME).setValue(getSimpleMetricWithSql(conn, withTenant(TRANSACTION_COUNT_SQL1)));
                getRawMetric(DB_TRANSACTION_RATE_NAME).setValue(getSimpleMetricWithSql(conn, withTenant(TRANSACTION_COUNT_SQL1)));
                getRawMetric(DB_TRANSACTION_LATENCY_NAME).setValue(getSimpleMetricWithSql(conn, withTenant(TRANSACTION_LATENCY_SQL1)));
                getRawMetric(DB_SQL_COUNT_NAME).setValue(getSimpleMetricWithSql(conn, withTenant(SQL_COUNT_SQL1)));
                getRawMetric(DB_SQL_RATE_NAME).setValue(getSimpleMetricWithSql(conn, withTenant(SQL_COUNT_SQL1)));
                getRawMetric(DB_IO_READ_RATE_NAME).setValue(getSimpleMetricWithSql(conn, withTenant(IO_READ_COUNT_SQL1)));
                getRawMetric(DB_IO_WRITE_RATE_NAME).setValue(getSimpleMetricWithSql(conn, withTenant(IO_WRITE_COUNT_SQL1)));
                getRawMetric(DB_TASK_WAIT_COUNT_NAME).setValue(getSimpleMetricWithSql(conn, withTenant(TASK_WAIT_COUNT_SQL1)));
                getRawMetric(DB_TASK_AVG_WAIT_TIME_NAME).setValue(getSimpleMetricWithSql(conn, withTenant(TASK_AVG_WAIT_TIME_SQL1)));

                getRawMetric(DB_CACHE_HIT_NAME).setValue(getMetricWithSql(conn, withTenant(CACHE_HIT_SQL1), DB_CACHE_HIT_KEY));
                getRawMetric(DB_SQL_ELAPSED_TIME_NAME).setValue(getMetricWithSql(conn, withTenant(SQL_ELAPSED_TIME_SQL1), DB_SQL_ELAPSED_TIME_KEY, SQL_TEXT.getKey()));
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update metric with exception", e);
            getRawMetric(DB_STATUS_NAME).setValue(0);
        }
    }

    private String withTenant(String rawStr) {
        return rawStr.replace(TENANT_HOLDER, getDbTenantId());
    }
}
