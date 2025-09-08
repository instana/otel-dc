package com.instana.dc.rdb.impl.informix.metric.collection.strategy;

import com.instana.dc.rdb.DbDcUtil;
import com.instana.dc.rdb.impl.informix.OnstatCommandExecutor;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricCollectionMode;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricDataConfig;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricsDataConfigRegister;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

public class MetricsCollectorTest {

    private static BasicDataSource dataSource;
    private static OnstatCommandExecutor onstatCommandExecutor;
    private static MetricsCollector metricsCollector;

    @BeforeAll
    public static void init() {
        dataSource = mock(BasicDataSource.class);
        onstatCommandExecutor = mock(OnstatCommandExecutor.class);
        metricsCollector = new MetricsCollector(dataSource, onstatCommandExecutor);
    }

    @Test
    public void shouldCollectMetricsWithSQL() throws SQLException {
        MetricsDataConfigRegister.subscribeMetricDataConfig("metric",
                new MetricDataConfig("query", MetricCollectionMode.SQL, Number.class));
        Connection connection = mock(Connection.class);
        given(dataSource.getConnection()).willReturn(connection);
        try (MockedStatic<DbDcUtil> utilities = mockStatic(DbDcUtil.class)) {
            utilities.when(() -> DbDcUtil.getSimpleMetricWithSql(connection, "query"))
                    .thenReturn(1);
            assertEquals((Number) 1, metricsCollector.collectMetrics("metric"));
        }

    }

    @Test
    public void shouldCollectMetricsWithCMD() {
        MetricsDataConfigRegister.subscribeMetricDataConfig("metric",
                new MetricDataConfig("query", MetricCollectionMode.CMD, Number.class));
        given(onstatCommandExecutor.executeCommand(any())).willReturn(Optional.of(new String[]{"1"}));
        assertEquals((Number) 1L, metricsCollector.collectMetrics("metric"));
    }
}
