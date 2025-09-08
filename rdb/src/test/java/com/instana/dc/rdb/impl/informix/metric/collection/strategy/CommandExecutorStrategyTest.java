package com.instana.dc.rdb.impl.informix.metric.collection.strategy;

import com.instana.dc.rdb.impl.informix.OnstatCommandExecutor;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricCollectionMode;
import com.instana.dc.rdb.impl.informix.metric.collection.MetricDataConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class CommandExecutorStrategyTest {

    private static CommandExecutorStrategy commandExecutorStrategy;
    private static OnstatCommandExecutor onstatCommandExecutor;

    @BeforeAll
    public static void init() {
        onstatCommandExecutor = mock(OnstatCommandExecutor.class);
        commandExecutorStrategy = new CommandExecutorStrategy(onstatCommandExecutor);
    }

    @Test
    public void shouldCollectNumberMetrics() {
        MetricDataConfig metricDataConfig = new MetricDataConfig("query", MetricCollectionMode.CMD, Number.class);
        given(onstatCommandExecutor.executeCommand(any())).willReturn(Optional.of(new String[]{"1"}));
        assertEquals((Number) 1L, commandExecutorStrategy.collectMetrics(metricDataConfig));
    }

    @Test
    public void shouldCollectDoubleMetrics() {
        MetricDataConfig metricDataConfig = new MetricDataConfig("query", MetricCollectionMode.CMD, Double.class);
        given(onstatCommandExecutor.executeCommand(any())).willReturn(Optional.of(new String[]{"1"}));
        assertEquals((Number) 1.0, commandExecutorStrategy.collectMetrics(metricDataConfig));
    }
}
