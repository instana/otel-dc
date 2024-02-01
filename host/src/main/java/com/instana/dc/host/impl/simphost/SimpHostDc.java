/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.host.impl.simphost;

import com.instana.dc.host.AbstractHostDc;
import com.instana.dc.host.HostDcUtil;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.mergeResourceAttributesFromEnv;
import static com.instana.dc.host.HostDcUtil.*;

public class SimpHostDc extends AbstractHostDc {
    private static final Logger logger = Logger.getLogger(SimpHostDc.class.getName());

    public SimpHostDc(Map<String, Object> properties, String hostSystem) {
        super(properties, hostSystem);
    }

    public String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "UnknownName";
        }
    }

    public String getHostId() {
        try {
            return HostDcUtil.readFileText("/etc/machine-id");
        } catch (IOException e) {
            return "UnknownID";
        }
    }

    @Override
    public Resource getResourceAttributes() {
        String hostName = getHostName();

        Resource resource = Resource.getDefault().merge(
                Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, getServiceName(),
                        ResourceAttributes.SERVICE_INSTANCE_ID, hostName
                ))
        );

        resource = resource.merge(
                Resource.create(Attributes.of(ResourceAttributes.HOST_NAME, hostName,
                        ResourceAttributes.OS_TYPE, "linux",
                        ResourceAttributes.HOST_ID, getHostId()
                ))
        );

        return mergeResourceAttributesFromEnv(resource);
    }

    @Override
    public void collectData() {
        logger.info("Start to collect metrics");
        getRawMetric(SYSTEM_CPU_TIME_NAME).setValue(SimpHostUtil.getCpuTimeResults());
        getRawMetric(SYSTEM_MEMORY_USAGE_NAME).setValue(SimpHostUtil.getMemUsageResults());

        List<Double> loads;
        try {
            loads = SimpHostUtil.getLoadAvgInfo();
            getRawMetric(SYSTEM_CPU_LOAD1_NAME).setValue(loads.get(0));
            getRawMetric(SYSTEM_CPU_LOAD5_NAME).setValue(loads.get(1));
            getRawMetric(SYSTEM_CPU_LOAD15_NAME).setValue(loads.get(2));
        } catch (Exception e) {
            logger.severe("Cannot record loads: " + e.getMessage());
        }
    }
}
