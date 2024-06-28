package com.instana.dc.rdb.impl.informix.metric.collection.strategy;

import com.instana.dc.SimpleQueryResult;
import com.instana.dc.rdb.DbDcUtil;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricCollectionMode;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricDataConfig;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

public class SqlExecutorStrategyTest {

    private static final String QUERY = "query";
    private static BasicDataSource dataSource;
    private static SqlExecutorStrategy sqlExecutorStrategy;

    @BeforeAll
    public static void init() {
        dataSource = mock(BasicDataSource.class);
        sqlExecutorStrategy = new SqlExecutorStrategy(dataSource);
    }

    @Test
    public void shouldCollectSimpleMetrics() throws SQLException {
        Connection connection = mock(Connection.class);
        given(dataSource.getConnection()).willReturn(connection);
        MetricDataConfig metricDataConfig = new MetricDataConfig(QUERY, MetricCollectionMode.SQL, Number.class);
        try (MockedStatic<DbDcUtil> utilities = mockStatic(DbDcUtil.class)) {
            utilities.when(() -> DbDcUtil.getSimpleMetricWithSql(connection, QUERY))
                    .thenReturn(1);
            assertEquals((Number) 1, sqlExecutorStrategy.collectMetrics(metricDataConfig));
        }
    }

    @Test
    public void shouldCollectListMetrics() throws SQLException {
        Connection connection = mock(Connection.class);
        given(dataSource.getConnection()).willReturn(connection);
        MetricDataConfig metricDataConfig = new MetricDataConfig(QUERY, MetricCollectionMode.SQL, List.class);
        SimpleQueryResult simpleQueryResult = new SimpleQueryResult(1);
        try (MockedStatic<DbDcUtil> utilities = mockStatic(DbDcUtil.class)) {
            utilities.when(() -> DbDcUtil.getMetricWithSql(connection, QUERY))
                    .thenReturn(Collections.singletonList(simpleQueryResult));
            assertEquals(Collections.singletonList(simpleQueryResult), sqlExecutorStrategy.collectMetrics(metricDataConfig));
        }
    }
}
