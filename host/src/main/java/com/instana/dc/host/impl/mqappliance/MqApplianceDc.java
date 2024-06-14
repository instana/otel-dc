/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.host.impl.mqappliance;

import com.instana.dc.host.AbstractHostDc;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.instana.dc.DcUtil.mergeResourceAttributesFromEnv;
import static com.instana.dc.host.HostDcUtil.*;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

public class MqApplianceDc extends AbstractHostDc {
    private static final Logger logger = Logger.getLogger(MqApplianceDc.class.getName());

    protected String applianceHost;
    private final String applianceUser;
    private final String appliancePassword;

    private Process process;
    protected BufferedReader bufferedReader;

    private long cpuTime = 0;
    private long cpuTimeIdle = 0;

    public MqApplianceDc(Map<String, Object> properties, String applianceSystem) {
        super(properties, applianceSystem);
        applianceHost = (String) properties.get(APPLIANCE_HOST);
        applianceUser = (String) properties.get(APPLIANCE_USER);
        appliancePassword = (String) properties.get(APPLIANCE_PASSWORD);
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::closeProcess));
    }

    private void closeProcess() {
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        } catch (IOException e) {
            logger.severe("Cannot close the bufferedReader: " + e.getMessage());
        }
        if (process != null) {
            process.destroy();
        }
        ScheduledExecutorService exec = getExec();
        if (exec != null && !exec.isShutdown()) {
            exec.shutdownNow();
        }
    }

    @Override
    public void start() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("expect", "scripts/getMqApplianceData.exp", applianceHost,
                    applianceUser, appliancePassword, String.valueOf(getPollInterval()));
            process = processBuilder.start();
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            addShutdownHook();
            getExec().scheduleWithFixedDelay(this::collectData, 1, getPollInterval(), TimeUnit.SECONDS);
        } catch (IOException e) {
            logger.severe("Cannot start the data collector: " + e.getMessage());
        }
    }

    public String getHostName() {
        return applianceHost;
    }

    @Override
    public Resource getResourceAttributes() {
        String applianceName = getHostName();

        Resource resource = Resource.getDefault().merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, getServiceName(), ResourceAttributes.SERVICE_INSTANCE_ID, applianceName)));

        resource = resource.merge(Resource.create(Attributes.of(ResourceAttributes.HOST_NAME, applianceName, ResourceAttributes.OS_TYPE, "MQ Appliance", ResourceAttributes.HOST_ID, applianceName)))
                .merge(Resource.create(Attributes.of(stringKey("INSTANA_PLUGIN"), "host")));

        return mergeResourceAttributesFromEnv(resource);
    }

    @Override
    public void collectData() {
        logger.info("Start to collect metrics");
        try {
            String line = bufferedReader.readLine();
            if (line != null) {
                logger.info("Data collected: " + line);
                String[] tokens = line.split(";");
                if (tokens.length >= 7) {
                    String[] systemMetricsTokens = tokens[0].split(":");
                    if (systemMetricsTokens.length == 6) {
                        long cpuUsage = Long.parseLong(systemMetricsTokens[0]);
                        if (cpuUsage >= 0) {
                            long cpuTime1 = cpuTime + cpuUsage;
                            long cpuTimeIdle1 = cpuTimeIdle + (100 - cpuUsage);
                            if (cpuTime1 >= 0 && cpuTimeIdle1 >= 0) {
                                cpuTime = cpuTime1;
                                cpuTimeIdle = cpuTimeIdle1;
                            } else { //Reset
                                cpuTime = 0;
                                cpuTimeIdle = 0;
                            }
                            getRawMetric(SYSTEM_CPU_TIME_NAME).setValue(MqApplianceUtil.getApplianceCpuUsageResults(cpuTime, cpuTimeIdle));
                        }
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
                } else {
                    logger.severe("Incorrect data format, cannot parse it.");
                }
            } else {
                logger.severe("No data returned. Please check if the appliance is running, or ensure that you are using the correct username and password.");
            }
        } catch (IOException e) {
            logger.severe("Cannot record loads: " + e.getMessage());
        }
    }
}
