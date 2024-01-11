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

    public static class CpuTime {
        private long user, nice, system, idle, iowait, irq, softirq;

        public long getUser() {
            return user;
        }

        public void setUser(long user) {
            this.user = user;
        }

        public long getNice() {
            return nice;
        }

        public void setNice(long nice) {
            this.nice = nice;
        }

        public long getSystem() {
            return system;
        }

        public void setSystem(long system) {
            this.system = system;
        }

        public long getIdle() {
            return idle;
        }

        public void setIdle(long idle) {
            this.idle = idle;
        }

        public long getIowait() {
            return iowait;
        }

        public void setIowait(long iowait) {
            this.iowait = iowait;
        }

        public long getIrq() {
            return irq;
        }

        public void setIrq(long irq) {
            this.irq = irq;
        }

        public long getSoftirq() {
            return softirq;
        }

        public void setSoftirq(long softirq) {
            this.softirq = softirq;
        }

        @Override
        public String toString() {
            return "CpuTime{" +
                    "user=" + user +
                    ", nice=" + nice +
                    ", system=" + system +
                    ", idle=" + idle +
                    ", iowait=" + iowait +
                    ", irq=" + irq +
                    ", softirq=" + softirq +
                    '}';
        }
    }

    public static CpuTime getCpuTime() throws IOException {
        String line = ApplianceDcUtil.readFileTextLine("/proc/stat");
        Pattern pattern = Pattern.compile("\\D+(\\d+)\\D+(\\d+)\\D+(\\d+)\\D+(\\d+)\\D+(\\d+)\\D+(\\d+)\\D+(\\d+)");
        Matcher m = pattern.matcher(line);

        CpuTime cpuTime = new CpuTime();
        if (m.find()) {
            cpuTime.setUser(Long.parseLong(m.group(1)));
            cpuTime.setNice(Long.parseLong(m.group(2)));
            cpuTime.setSystem(Long.parseLong(m.group(3)));
            cpuTime.setIdle(Long.parseLong(m.group(4)));
            cpuTime.setIowait(Long.parseLong(m.group(5)));
            cpuTime.setIrq(Long.parseLong(m.group(6)));
            cpuTime.setSoftirq(Long.parseLong(m.group(7)));
        }
        return cpuTime;
    }

    public static List<SimpleQueryResult> getCpuTimeResults() {
        CpuTime cputime = null;
        try {
            cputime = getCpuTime();
        } catch (IOException e) {
            logger.severe("Cannot getCpuTimeResults: " + e.getMessage());
            return null;
        }

        SimpleQueryResult user = new SimpleQueryResult(cputime.getUser());
        SimpleQueryResult nice = new SimpleQueryResult(cputime.getNice());
        SimpleQueryResult system = new SimpleQueryResult(cputime.getSystem());
        SimpleQueryResult idle = new SimpleQueryResult(cputime.getIdle());
        SimpleQueryResult wait = new SimpleQueryResult(cputime.getIowait());
        SimpleQueryResult interrupt = new SimpleQueryResult(cputime.getIrq());
        SimpleQueryResult softirq = new SimpleQueryResult(cputime.getSoftirq());

        user.setKey("user");
        nice.setKey("nice");
        system.setKey("system");
        idle.setKey("idle");
        wait.setKey("wait");
        interrupt.setKey("interrupt");
        softirq.setKey("softirq");

        user.setAttribute("cpu", "cpu");
        nice.setAttribute("cpu", "cpu");
        system.setAttribute("cpu", "cpu");
        idle.setAttribute("cpu", "cpu");
        wait.setAttribute("cpu", "cpu");
        interrupt.setAttribute("cpu", "cpu");
        softirq.setAttribute("cpu", "cpu");

        user.setAttribute("state", "user");
        nice.setAttribute("state", "nice");
        system.setAttribute("state", "system");
        idle.setAttribute("state", "idle");
        wait.setAttribute("state", "wait");
        interrupt.setAttribute("state", "interrupt");
        softirq.setAttribute("state", "softirq");

        List<SimpleQueryResult> results = new ArrayList<SimpleQueryResult>(7);
        results.add(user);
        results.add(nice);
        results.add(system);
        results.add(idle);
        results.add(wait);
        results.add(interrupt);
        results.add(softirq);
        return results;
    }

    public static Map<String, Long> getMemInfo() throws IOException {
        Map<String, Long> map = new HashMap<>();
        String txt = ApplianceDcUtil.readFileText("/proc/meminfo", 5000);
        String[] lines = txt.split("\n");
        for (String line : lines) {
            String[] words = line.split("\\s+", 3);
            if (words.length >= 3) {
                if (words[0].endsWith(":")) {
                    String key = words[0].substring(0, words[0].length() - 1);
                    map.put(key, Long.parseLong(words[1]));
                }
            }
        }
        return map;
    }

    private static void addMemUsageResult(Map<String, Long> map, List<SimpleQueryResult> results, String oldKey, String newKey) {
        Long value = map.get(oldKey);
        if (value != null) {
            SimpleQueryResult result = new SimpleQueryResult(value * 1000L);
            result.setKey(newKey);
            result.setAttribute("state", newKey);
            results.add(result);
        }
    }

    public static List<SimpleQueryResult> getMemUsageResults() {
        Map<String, Long> map = null;
        try {
            map = getMemInfo();
        } catch (IOException e) {
            logger.severe("Cannot getMemUsageResults: " + e.getMessage());
            return null;
        }

        List<SimpleQueryResult> results = new ArrayList<SimpleQueryResult>(7);

        addMemUsageResult(map, results, "MemFree", "free");
        addMemUsageResult(map, results, "Buffers", "buffered");
        addMemUsageResult(map, results, "Cached", "cached");
        addMemUsageResult(map, results, "Inactive", "inactive");
        addMemUsageResult(map, results, "SReclaimable", "slab_reclaimable");
        addMemUsageResult(map, results, "SUnreclaim", "slab_unreclaimable");

        Long free = map.get("MemFree");
        Long total = map.get("MemTotal");
        if (free != null && total != null) {
            SimpleQueryResult result = new SimpleQueryResult((total - free) * 1000L);
            result.setKey("used");
            result.setAttribute("state", "used");
            results.add(result);
        }

        return results;
    }

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