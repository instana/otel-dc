package com.instana.dc.cdc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.instana.dc.cdc.ApmDcUtil.*;

@Component
@PropertySource("classpath:application.properties")
public class ApmDc extends AbstractApmDc {
    private final Queue<ApmEvent> evtQueue = new ConcurrentLinkedQueue<>();

    public Queue<ApmEvent> getEvtQueue() {
        return evtQueue;
    }

    public ApmDc(@Autowired Environment env) {
        super(env);
    }

    @Override
    public void collectData() {
        ApmEvent event = evtQueue.poll();
        while (event != null) {
            Map<String, Object> map = new HashMap<>();
            map.put(EVENT_TIMESTAMP, event.getTimestamp());
            map.put(EVENT_OBSERVED_TIMESTAMP, event.getObservedTimestamp());
            map.put(EVENT_TRACE_ID, event.getTraceId());
            map.put(EVENT_SPAN_ID, event.getSpanId());
            map.put(EVENT_TRACE_FLAGS, event.getTraceFlags());
            map.put(EVENT_SEVERITY_TEXT, event.getSeverityText());
            map.put(EVENT_SEVERITY_NUMBER, event.getSeverityNumber());
            map.put(EVENT_BODY, event.getBody());
            map.put(EVENT_RESOURCE, event.getResource());
            map.put(EVENT_INSTRUMENTATION_SCOPE, event.getInstrumentationScope());
            map.put(EVENT_ATTRIBUTES, event.getAttributes());

            getRawMetric(M_EVENT_NAME).getDataPoint(event.getId()).setValue(0, map);
            event = evtQueue.poll();
        }
    }
}
