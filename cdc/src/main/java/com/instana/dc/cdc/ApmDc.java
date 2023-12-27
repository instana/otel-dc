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
            map.put(EVENT_KEY, event.getKey());
            map.put(EVENT_VENDOR, event.getVendor());
            map.put(EVENT_ID, event.getEventId());
            map.put(EVENT_TITLE, event.getTitle());
            map.put(EVENT_DESCRIPTION, event.getDescription());
            map.put(EVENT_SEVERITY, event.getSeverity());
            map.put(EVENT_TIMESTAMP, event.getTimestamp());
            map.put(EVENT_ETC, event.getEtc());
            getRawMetric(M_EVENT_NAME).getDataPoint(event.getKey()).setValue(0, map);
            event = evtQueue.poll();
        }
    }
}
