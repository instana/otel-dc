/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.appliance.impl;

import com.instana.dc.SimpleQueryResult;
import com.instana.dc.appliance.ApplianceDcUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MqApplianceUtil {
    private static final Logger logger = Logger.getLogger(MqApplianceUtil.class.getName());

    public static List<SimpleQueryResult> getApplianceCpuUsageResults(Double cpuUsage) {
        List<SimpleQueryResult> results = new ArrayList<SimpleQueryResult>(1);
        if (cpuUsage != null) {
            SimpleQueryResult result = new SimpleQueryResult(cpuUsage/100);
            result.setKey("user");
            result.setAttribute("cpu", "cpu");
            result.setAttribute("state", "user");
            results.add(result);
            return results;
        }
        return null;
    }

    public static List<SimpleQueryResult> getApplianceMemUsageResults(Long usedMem, Long freeMem) {
        List<SimpleQueryResult> results = new ArrayList<SimpleQueryResult>(2);
        if (freeMem != null && usedMem != null) {
            SimpleQueryResult resultUsed = new SimpleQueryResult(usedMem * 1024L * 1024L);
            resultUsed.setKey("used");
            resultUsed.setAttribute("state", "used");
            results.add(resultUsed);
            SimpleQueryResult resultFree = new SimpleQueryResult(freeMem * 1024L * 1024L);
            resultFree.setKey("free");
            resultFree.setAttribute("state", "free");
            results.add(resultFree);
            return results;
        }
        return null;
    }

    public static List<SimpleQueryResult> getApplianceNetworkConnectionsResults(String networkConnections) {
        if (networkConnections != null && networkConnections.length() > 0) {
            String[] tokens = networkConnections.split(":");
            if (tokens.length == 11) {
                List<SimpleQueryResult> results = new ArrayList<SimpleQueryResult>(11);
                
                SimpleQueryResult established = new SimpleQueryResult(Integer.parseInt(tokens[0]));
                SimpleQueryResult syn_sent = new SimpleQueryResult(Integer.parseInt(tokens[1]));
                SimpleQueryResult syn_received = new SimpleQueryResult(Integer.parseInt(tokens[2]));
                SimpleQueryResult fin_wait_1 = new SimpleQueryResult(Integer.parseInt(tokens[3]));
                SimpleQueryResult fin_wait_2 = new SimpleQueryResult(Integer.parseInt(tokens[4]));
                SimpleQueryResult time_wait = new SimpleQueryResult(Integer.parseInt(tokens[5]));
                SimpleQueryResult closed = new SimpleQueryResult(Integer.parseInt(tokens[6]));
                SimpleQueryResult closed_wait = new SimpleQueryResult(Integer.parseInt(tokens[7]));
                SimpleQueryResult last_ack = new SimpleQueryResult(Integer.parseInt(tokens[8]));
                SimpleQueryResult listen = new SimpleQueryResult(Integer.parseInt(tokens[9]));
                SimpleQueryResult closing = new SimpleQueryResult(Integer.parseInt(tokens[10]));

                established.setKey("ESTABLISHED");
                syn_sent.setKey("SYN_SENT");
                syn_received.setKey("SYN_RECV");
                fin_wait_1.setKey("FIN_WAIT_1");
                fin_wait_2.setKey("FIN_WAIT_2");
                time_wait.setKey("TIME_WAIT");
                closed.setKey("CLOSE");
                closed_wait.setKey("CLOSE_WAIT");
                last_ack.setKey("LAST_ACK");
                listen.setKey("LISTEN");
                closing.setKey("CLOSING");

                established.setAttribute("protocol", "tcp");
                syn_sent.setAttribute("protocol", "tcp");
                syn_received.setAttribute("protocol", "tcp");
                fin_wait_1.setAttribute("protocol", "tcp");
                fin_wait_2.setAttribute("protocol", "tcp");
                time_wait.setAttribute("protocol", "tcp");
                closed.setAttribute("protocol", "tcp");
                closed_wait.setAttribute("protocol", "tcp");
                last_ack.setAttribute("protocol", "tcp");
                listen.setAttribute("protocol", "tcp");
                closing.setAttribute("protocol", "tcp");

                established.setAttribute("state", "ESTABLISHED");
                syn_sent.setAttribute("state", "SYN_SENT");
                syn_received.setAttribute("state", "SYN_RECV");
                fin_wait_1.setAttribute("state", "FIN_WAIT_1");
                fin_wait_2.setAttribute("state", "FIN_WAIT_2");
                time_wait.setAttribute("state", "TIME_WAIT");
                closed.setAttribute("state", "CLOSE");
                closed_wait.setAttribute("state", "CLOSE_WAIT");
                last_ack.setAttribute("state", "LAST_ACK");
                listen.setAttribute("state", "LISTEN");
                closing.setAttribute("state", "CLOSING");

                results.add(established);
                results.add(syn_sent);
                results.add(syn_received);
                results.add(fin_wait_1);
                results.add(fin_wait_2);
                results.add(time_wait);
                results.add(closed);
                results.add(closed_wait);
                results.add(last_ack);
                results.add(listen);
                results.add(closing);

                return results;
            }
        }
        return null;
    }

    public static List<SimpleQueryResult> getApplianceNetworkInterfaceResults(String networkInterfaceData) {
        if (networkInterfaceData != null && networkInterfaceData.length() > 0) {
            String[] tokens = networkInterfaceData.split(":");
            List<SimpleQueryResult> results = new ArrayList<SimpleQueryResult>(tokens.length * 2);
            for (int i = 0; i < tokens.length; i++) {
                String[] tokenss = tokens[i].split("\\|");
                if (tokenss.length == 3) {
                    SimpleQueryResult resultR = new SimpleQueryResult(Long.parseLong(tokenss[1]));
                    resultR.setKey(tokenss[0] + ":receive");
                    resultR.setAttribute("device", tokenss[0]);
                    resultR.setAttribute("direction", "receive");
                    results.add(resultR);

                    SimpleQueryResult resultT = new SimpleQueryResult(Long.parseLong(tokenss[2]));
                    resultT.setKey(tokenss[0] + ":transmit");
                    resultT.setAttribute("device", tokenss[0]);
                    resultT.setAttribute("direction", "transmit");
                    results.add(resultT);
                }
            }
            return results;
        }
        return null;
    }

    public static int convertQmgrStatusToInt(String status) {
        switch (status.toLowerCase()) {
            case "starting":
                return 1;
            case "running":
                return 2;
            case "quiescing":
                return 3;
            case "running as standby":
                return 4;
            case "running elsewhere":
                return 5;
            case "ending immediately":
                return 6;
            case "ending pre-emptively":
                return 7;
            case "ended normally":
                return 8;
            case "ended immediately":
                return 9;
            case "ended unexpectedly":
                return 10;
            case "ended pre-emptively":
                return 11;
            case "status not available":
                return 12;
            default:
                return 0;
        }
    }

    public static List<SimpleQueryResult> getQmgrStatusResults(String qmgrStatusData) {
        if (qmgrStatusData != null && qmgrStatusData.length() > 0) {
            String[] tokens = qmgrStatusData.split(":");
            List<SimpleQueryResult> results = new ArrayList<SimpleQueryResult>(tokens.length);
            for (int i = 0; i < tokens.length; i++) {
                String[] tokenss = tokens[i].split("\\|");
                if (tokenss.length == 2) {
                    SimpleQueryResult result = new SimpleQueryResult(convertQmgrStatusToInt(tokenss[1]));
                    result.setKey(tokenss[0]);
                    result.setAttribute("qmgr", tokenss[0]);
                    results.add(result);
                }
            }
            return results;
        }
        return null;
    }

    public static List<Double> getLoadAvgInfo() throws IOException {
        final int AVG_NUM = 3;
        String line = ApplianceDcUtil.readFileTextLine("/proc/loadavg");
        String[] words = line.split("\\s+", AVG_NUM + 1);
        if (words.length < AVG_NUM)
            return null;

        List<Double> loads = new ArrayList<>(AVG_NUM);
        for (int i = 0; i < AVG_NUM; i++) {
            loads.add(Double.parseDouble(words[i]));
        }
        return loads;
    }

}