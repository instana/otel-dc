/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb.impl.mssql;

import com.instana.dc.CalculationMode;
import com.instana.dc.DcUtil;
import com.instana.dc.rdb.AbstractDbDc;
import com.instana.dc.rdb.DbDcUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.instana.agent.sensorsdk.semconv.SemanticAttributes.*;
import static com.instana.dc.rdb.DbDcUtil.*;
import static com.instana.dc.rdb.impl.mssql.MssqlUtil.*;

public class MssqlDc extends AbstractDbDc {
    private static final Logger logger = Logger.getLogger(MssqlDc.class.getName());

    public MssqlDc(Map<String, Object> properties, String dbSystem, String dbDriver) throws SQLException {
        super(properties, dbSystem, dbDriver);
        setDbPassword(DcUtil.base64Decode(getDbPassword()));
        findDbNameAndVersion();
        if (getServiceInstanceId() == null) {
            setServiceInstanceId(getDbAddress() + ":" + getDbPort() + "@" + getDbName());
        }
    }

    private void findDbNameAndVersion() throws SQLException {
        try (Connection connection = getConnection()) {
            ResultSet rs = DbDcUtil.executeQuery(connection, DB_NAME_VERSION_SQL);
            rs.next();
            if (getDbName() == null)
                setDbName(rs.getString(1));
            setDbVersion(rs.getString(2));
        }
    }

    @Override
    public void registerMetrics() {
        super.registerMetrics();
//        getRawMetric(DB_TRANSACTION_RATE_NAME).setCalculationMode(CalculationMode.RATE);
//        getRawMetric(DB_SQL_RATE_NAME).setCalculationMode(CalculationMode.RATE);
//        getRawMetric(DB_IO_READ_RATE_NAME).setCalculationMode(CalculationMode.RATE);
//        getRawMetric(DB_IO_WRITE_RATE_NAME).setCalculationMode(CalculationMode.RATE);
    }

    @Override
    public void collectData() {
        logger.info("Start to collect metrics");
        try (Connection conn = getConnection()) {

            //TODO


           getRawMetric(DB_STATUS_NAME).setValue(1);
            getRawMetric(DB_INSTANCE_COUNT_NAME).setValue(getSimpleMetricWithSql(conn, INSTANCE_COUNT_SQL));
            getRawMetric(DB_INSTANCE_ACTIVE_COUNT_NAME).setValue(getSimpleMetricWithSql(conn, INSTANCE_ACTIVE_COUNT_SQL));


        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update metric with exception", e);
            getRawMetric(DB_STATUS_NAME).setValue(0);
        }
    }
}
