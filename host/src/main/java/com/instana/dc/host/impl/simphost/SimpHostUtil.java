/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.host.impl.simphost;

import com.instana.dc.SimpleQueryResult;
import com.instana.dc.host.HostDcUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpHostUtil {
    private static final Logger logger = Logger.getLogger(SimpHostUtil.class.getName());

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
        String line = HostDcUtil.readFileTextLine("/proc/stat");
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
        String txt = HostDcUtil.readFileText("/proc/meminfo", 5000);
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

    public static List<Double> getLoadAvgInfo() throws IOException {
        final int AVG_NUM = 3;
        String line = HostDcUtil.readFileTextLine("/proc/loadavg");
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