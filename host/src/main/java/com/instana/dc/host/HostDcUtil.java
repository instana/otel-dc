/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.host;

import java.io.*;
import java.util.logging.Logger;


public class HostDcUtil {
    private static final Logger logger = Logger.getLogger(HostDcUtil.class.getName());

    public static class MeterName{
        public static final String CPU = "cpu";
        public static final String MEMORY = "memory";
        public static final String NETWORK = "network";
        public static final String LOAD = "load";
        public static final String DISK = "disk";
        public static final String FILESYSTEM = "filesystem";
        public static final String PROCESSES = "processes";
        public static final String PAGING = "paging";
    }

    /* Configurations for Metrics:
     */
    public static final String UNIT_S = "s";
    public static final String UNIT_BY = "By";
    public static final String UNIT_1 = "1";

    public static final String SYSTEM_CPU_TIME_NAME = "system.cpu.time";
    public static final String SYSTEM_CPU_TIME_DESC = "Seconds each logical CPU spent on each mode";
    public static final String SYSTEM_CPU_TIME_UNIT = UNIT_S;

    public static final String SYSTEM_MEMORY_USAGE_NAME = "system.memory.usage";
    public static final String SYSTEM_MEMORY_USAGE_DESC = "Bytes of memory in use";
    public static final String SYSTEM_MEMORY_USAGE_UNIT = UNIT_BY;

    public static final String SYSTEM_CPU_LOAD1_NAME = "system.cpu.load_average.1m";
    public static final String SYSTEM_CPU_LOAD1_DESC = "Average CPU Load over 1 minutes";
    public static final String SYSTEM_CPU_LOAD1_UNIT = UNIT_1;

    public static final String SYSTEM_CPU_LOAD5_NAME = "system.cpu.load_average.5m";
    public static final String SYSTEM_CPU_LOAD5_DESC = "Average CPU Load over 5 minutes";
    public static final String SYSTEM_CPU_LOAD5_UNIT = UNIT_1;

    public static final String SYSTEM_CPU_LOAD15_NAME = "system.cpu.load_average.15m";
    public static final String SYSTEM_CPU_LOAD15_DESC = "Average CPU Load over 15 minutes";
    public static final String SYSTEM_CPU_LOAD15_UNIT = UNIT_1;

    public static String readFileText(String filePath) throws IOException {
        try (InputStream is = new FileInputStream(filePath)) {
            int n = is.available();
            byte[] ba = new byte[n];
            is.read(ba);
            return new String(ba).trim();
        }
    }

    public static String readFileText(String filePath, int max) throws IOException {
        try (InputStream is = new FileInputStream(filePath)) {
            byte[] ba = new byte[max];
            int n = is.read(ba);
            return new String(ba, 0, n).trim();
        }
    }

    public static String readFileTextLine(String filePath) throws IOException {
        try (Reader rd = new FileReader(filePath)) {
            BufferedReader reader = new BufferedReader(rd);
            String line = reader.readLine();
            reader.close();
            return line;
        }
    }
}