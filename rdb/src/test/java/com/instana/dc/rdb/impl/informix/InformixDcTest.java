package com.instana.dc.rdb.impl.informix;

import com.instana.dc.rdb.impl.informix.metric.collection.strategy.MetricsCollector;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleGaugeBuilder;
import io.opentelemetry.api.metrics.DoubleUpDownCounterBuilder;
import io.opentelemetry.api.metrics.LongGaugeBuilder;
import io.opentelemetry.api.metrics.LongUpDownCounterBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterBuilder;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static com.instana.dc.CalculationMode.RATE;
import static com.instana.dc.rdb.DbDcUtil.DB_PASSWORD;
import static com.instana.dc.rdb.DbDcUtil.DB_TRANSACTION_RATE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class InformixDcTest {

    private static InformixDc informixDc;
    private static final MetricsCollector metricsCollector = mock(MetricsCollector.class);

    @BeforeAll
    public static void init() throws SQLException {
        try (MockedConstruction<BasicDataSource> mockedDataSource = Mockito.mockConstruction(BasicDataSource.class,
                (mock, context) -> {
                    Connection connection = mock(Connection.class);
                    Statement statement = mock(Statement.class);
                    given(statement.executeQuery(anyString())).willReturn(mock(ResultSet.class));
                    given(connection.createStatement()).willReturn(statement);
                    given(mock.getConnection()).willReturn(connection);
                })) {
            informixDc = new InformixDc(buildProperties(), "informix", "com.informix.jdbc.IfxDriver");
            Field metricCollector = informixDc.getClass().getDeclaredField("metricCollector");
            metricCollector.setAccessible(true);
            metricCollector.set(informixDc, metricsCollector);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> buildProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(DB_PASSWORD, "password");
        Map<String, Object> customInput = new HashMap<>();
        customInput.put("db.names", "instana, test");
        customInput.put("db.sequential.scan.count", 1);
        properties.put("custom.input", customInput);
        Map<String, Integer> customPollInterval = new HashMap<>();
        customPollInterval.put("HIGH", 10);
        properties.put("custom.poll.interval", customPollInterval);
        return properties;
    }

    @Test
    public void shouldRegisterMetricsMetadata() {
        OpenTelemetry openTelemetry = mock(OpenTelemetry.class);
        MeterBuilder meterBuilder = buildMeterBuilder();
        given(openTelemetry.meterBuilder(any())).willReturn(meterBuilder);
        informixDc.initMeters(openTelemetry);
        informixDc.registerMetrics();
        assertEquals(RATE, informixDc.getRawMetric(DB_TRANSACTION_RATE_NAME).getCalculationMode());
    }

    @Test
    public void shouldCollectData() {
        informixDc.collectData();
        verify(metricsCollector, times(32)).collectMetrics(anyString());
    }

    private static MeterBuilder buildMeterBuilder() {
        MeterBuilder meterBuilder = mock(MeterBuilder.class);
        given(meterBuilder.setInstrumentationVersion(any())).willReturn(meterBuilder);
        Meter meter = buildMeter();
        given(meterBuilder.build()).willReturn(meter);
        return meterBuilder;
    }

    private static Meter buildMeter() {
        Meter meter = mock(Meter.class);
        LongUpDownCounterBuilder longUpDownCounterBuilder = buildLongUpDownCounterBuilder();
        given(meter.upDownCounterBuilder(any())).willReturn(longUpDownCounterBuilder);
        DoubleGaugeBuilder doubleGaugeBuilder = buildDoubleGaugeBuilder();
        given(meter.gaugeBuilder(any())).willReturn(doubleGaugeBuilder);
        return meter;
    }

    private static DoubleGaugeBuilder buildDoubleGaugeBuilder() {
        DoubleGaugeBuilder doubleGaugeBuilder = mock(DoubleGaugeBuilder.class);
        given(doubleGaugeBuilder.setUnit(any())).willReturn(doubleGaugeBuilder);
        given(doubleGaugeBuilder.setDescription(any())).willReturn(doubleGaugeBuilder);
        LongGaugeBuilder longGaugeBuilder = buildLongGaugeBuilder();
        given(doubleGaugeBuilder.ofLongs()).willReturn(longGaugeBuilder);
        return doubleGaugeBuilder;
    }

    private static LongGaugeBuilder buildLongGaugeBuilder() {
        LongGaugeBuilder longGaugeBuilder = mock(LongGaugeBuilder.class);
        given(longGaugeBuilder.setUnit(any())).willReturn(longGaugeBuilder);
        given(longGaugeBuilder.setDescription(any())).willReturn(longGaugeBuilder);
        return longGaugeBuilder;
    }

    private static LongUpDownCounterBuilder buildLongUpDownCounterBuilder() {
        LongUpDownCounterBuilder longUpDownCounterBuilder = mock(LongUpDownCounterBuilder.class);
        given(longUpDownCounterBuilder.setUnit(any())).willReturn(longUpDownCounterBuilder);
        given(longUpDownCounterBuilder.setDescription(any())).willReturn(longUpDownCounterBuilder);
        DoubleUpDownCounterBuilder doubleUpDownCounterBuilder = buildDoubleUpDownCounterBuilder();
        given(longUpDownCounterBuilder.ofDoubles()).willReturn(doubleUpDownCounterBuilder);
        return longUpDownCounterBuilder;
    }

    private static DoubleUpDownCounterBuilder buildDoubleUpDownCounterBuilder() {
        DoubleUpDownCounterBuilder doubleUpDownCounterBuilder = mock(DoubleUpDownCounterBuilder.class);
        given(doubleUpDownCounterBuilder.setUnit(any())).willReturn(doubleUpDownCounterBuilder);
        given(doubleUpDownCounterBuilder.setDescription(any())).willReturn(doubleUpDownCounterBuilder);
        return doubleUpDownCounterBuilder;
    }
}
