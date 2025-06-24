package com.instana.dc.genai.base;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.instana.dc.genai.service.MetricsCollectorService;
import com.instana.dc.genai.llm.metrics.LLMOtelMetric;
import com.instana.dc.genai.vectordb.metrics.VectordbOtelMetric;

public abstract class AbstractMetricCollector {
    private static final Logger logger = Logger.getLogger(AbstractMetricCollector.class.getName());

    protected final MetricsCollectorService metricsCollectorService;
    protected final Boolean otelAgentlessMode;
    protected final Integer otelPollInterval;
    protected final int listenPort;
    protected final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private boolean started = false;

    protected AbstractMetricCollector(Boolean otelAgentlessMode, Integer otelPollInterval, int listenPort) {
        this.metricsCollectorService = MetricsCollectorService.getInstance();
        this.otelAgentlessMode = otelAgentlessMode;
        this.otelPollInterval = otelPollInterval;
        this.listenPort = listenPort;
    }

    public void start() {
        synchronized (this) {
            if (started) return;
            started = true;
        }
        exec.scheduleWithFixedDelay(() -> {
            try {
                collectMetrics();
            } catch (Exception e) {
                logger.severe("Error in metric collection: " + e.getMessage());
            }
        }, 1, otelPollInterval, TimeUnit.SECONDS);
    }

    protected abstract void collectMetrics();
    protected abstract void processLLMMetric(LLMOtelMetric metric);
    protected abstract void processVectordbMetric(VectordbOtelMetric metric);
    protected abstract void processMetrics(int divisor);
}
