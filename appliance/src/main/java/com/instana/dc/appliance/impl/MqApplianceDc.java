/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.appliance.impl;

import com.instana.dc.appliance.AbstractApplianceDc;
import com.instana.dc.appliance.ApplianceDcUtil;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.mergeResourceAttributesFromEnv;
import static com.instana.dc.appliance.ApplianceDcUtil.*;

public class MqApplianceDc extends AbstractApplianceDc {
    private static final Logger logger = Logger.getLogger(MqApplianceDc.class.getName());

    public MqApplianceDc(Map<String, String> properties, String applianceSystem) {
        super(properties, applianceSystem);
    }

    public String getHostName() {
        return applianceHost;
    }

    public String getApplianceId() {
        try {
            return ApplianceDcUtil.readFileText("/etc/machine-id");
        } catch (IOException e) {
            return "UnknownID";
        }
    }

    @Override
    public Resource getResourceAttributes() {
        String applianceName = getHostName();

        Resource resource = Resource.getDefault().merge(
                Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, getServiceName(),
                        ResourceAttributes.SERVICE_INSTANCE_ID, applianceName
                ))
        );

        resource = resource.merge(
                Resource.create(Attributes.of(ResourceAttributes.HOST_NAME, applianceName,
                        ResourceAttributes.OS_TYPE, "MQ Appliance",
                        ResourceAttributes.HOST_ID, getApplianceId()
                ))
        );

        return mergeResourceAttributesFromEnv(resource);
    }

    @Override
    public void collectData() {
        logger.info("Start to collect metrics");
        //getRawMetric(SYSTEM_CPU_TIME_NAME).setValue(MqApplianceUtil.getCpuTimeResults())
        try {
            String line = bufferedReader.readLine();
            if (line != null) {
                logger.info("Data collected: " + line);
                String[] tokens = line.split(";");
                if (tokens.length >= 7) {
                    String[] systemMetricsTokens = tokens[0].split(":");
                    if (systemMetricsTokens.length == 6) {
                        getRawMetric(SYSTEM_CPU_TIME_NAME).setValue(MqApplianceUtil.getApplianceCpuUsageResults(Double.parseDouble(systemMetricsTokens[0])));
                        getRawMetric(SYSTEM_CPU_LOAD1_NAME).setValue(Double.parseDouble(systemMetricsTokens[1]));
                        getRawMetric(SYSTEM_CPU_LOAD5_NAME).setValue(Double.parseDouble(systemMetricsTokens[2]));
                        getRawMetric(SYSTEM_CPU_LOAD15_NAME).setValue(Double.parseDouble(systemMetricsTokens[3]));
                        getRawMetric(SYSTEM_MEMORY_USAGE_NAME).setValue(MqApplianceUtil.getApplianceMemUsageResults(Long.parseLong(systemMetricsTokens[4]), Long.parseLong(systemMetricsTokens[5])));
                    }
                    getRawMetric(SYSTEM_NETWORK_CONNECTIONS_NAME).setValue(MqApplianceUtil.getApplianceNetworkConnectionsResults(tokens[1]));
                    getRawMetric(SYSTEM_NETWORK_IO_NAME).setValue(MqApplianceUtil.getApplianceNetworkInterfaceResults(tokens[2]));
                    getRawMetric(SYSTEM_NETWORK_PACKETS_NAME).setValue(MqApplianceUtil.getApplianceNetworkInterfaceResults(tokens[3]));
                    getRawMetric(SYSTEM_NETWORK_ERRORS_NAME).setValue(MqApplianceUtil.getApplianceNetworkInterfaceResults(tokens[4]));
                    getRawMetric(SYSTEM_NETWORK_DROPPED_NAME).setValue(MqApplianceUtil.getApplianceNetworkInterfaceResults(tokens[5]));
                    getRawMetric(SYSTEM_IBMQMGR_STATUS_NAME).setValue(MqApplianceUtil.getQmgrStatusResults(tokens[6]));
                }
            }
        } catch (IOException e) {
            logger.severe("Cannot record loads: " + e.getMessage());
        }
    }
}
