/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.host.impl.snmphost;

import com.instana.dc.SimpleQueryResult;
import com.instana.dc.host.HostDcUtil;
import com.instana.simpsnmp.SnmpValue;
import org.snmp4j.smi.OID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SnmpHostUtil {
    private static final Logger logger = Logger.getLogger(SnmpHostUtil.class.getName());

    public static class Oid {
        //scalar_oids:
        public static final OID CPU_TIME__USER = new OID(".1.3.6.1.4.1.2021.11.50.0");
        public static final OID CPU_TIME__SYSTEM = new OID(".1.3.6.1.4.1.2021.11.52.0");
        public static final OID CPU_TIME__IDLE = new OID(".1.3.6.1.4.1.2021.11.53.0");
        public static final OID CPU_TIME__NICE = new OID(".1.3.6.1.4.1.2021.11.51.0");

        public static final OID CPU_LOAD_1M = new OID(".1.3.6.1.4.1.2021.10.1.6.1");
        public static final OID CPU_LOAD_5M = new OID(".1.3.6.1.4.1.2021.10.1.6.2");
        public static final OID CPU_LOAD_15M = new OID(".1.3.6.1.4.1.2021.10.1.6.3");

        public static final OID MEMORY_USAGE__TOTAL = new OID(".1.3.6.1.4.1.2021.4.5.0");
        public static final OID MEMORY_USAGE__FREE = new OID(".1.3.6.1.4.1.2021.4.6.0");
        public static final OID MEMORY_USAGE__BUFFERED = new OID(".1.3.6.1.4.1.2021.4.14.0");
        public static final OID MEMORY_USAGE__CACHED = new OID(".1.3.6.1.4.1.2021.4.15.0");

        public static final OID HOST_NAME = new OID("1.3.6.1.2.1.1.5.0");
        public static final OID OS_TYPE = new OID("1.3.6.1.2.1.1.1.0");


        //column_oids:
        public static final OID DISK_IO__READ = new OID(".1.3.6.1.4.1.2021.13.15.1.1.3");
        public static final OID DISK_IO__WRITE = new OID(".1.3.6.1.4.1.2021.13.15.1.1.4");
        public static final OID DISKDEVICE = new OID(".1.3.6.1.4.1.2021.13.15.1.1.2");

        public static final OID FILESYSTEMDEVICE = new OID(".1.3.6.1.2.1.25.2.3.1.3");
        public static final OID FILESYSTEM_USAGE__USED = new OID(".1.3.6.1.2.1.25.2.3.1.6");
        public static final OID FILESYSTEM_USAGE__ALL = new OID(".1.3.6.1.2.1.25.2.3.1.5");
        public static final OID FILESYSTEM_USAGE__UNIT = new OID(".1.3.6.1.2.1.25.2.3.1.4");

        public static final OID NETWORKDEVICE = new OID(".1.3.6.1.2.1.2.2.1.2");
        public static final OID NETWORK_DROPPED_RECEIVE = new OID(".1.3.6.1.2.1.2.2.1.13");
        public static final OID NETWORK_DROPPED_TRANSMIT = new OID(".1.3.6.1.2.1.2.2.1.19");
        public static final OID NETWORK_ERRORS_RECEIVE = new OID(".1.3.6.1.2.1.2.2.1.14");
        public static final OID NETWORK_ERRORS_TRANSMIT = new OID(".1.3.6.1.2.1.2.2.1.20");
        public static final OID NETWORK_IO_RECEIVE = new OID(".1.3.6.1.2.1.2.2.1.10");
        public static final OID NETWORK_IO_TRANSMIT = new OID(".1.3.6.1.2.1.2.2.1.16");
        public static final OID NETWORK_PACKAGES_RECEIVE = new OID(".1.3.6.1.2.1.2.2.1.11");
        public static final OID NETWORK_PACKAGES_TRANSMIT = new OID(".1.3.6.1.2.1.2.2.1.17");
    }

    //Parameters:
    public static final String SNMP_HOST = "snmp.host";
    public static final String HOST_NAME = "host.name";
    public static final String OS_TYPE = "os.type";


    public static List<SimpleQueryResult> getCpuTimeResults(Map<OID, SnmpValue> result) {
        SimpleQueryResult user = null;
        SimpleQueryResult system = null;
        SimpleQueryResult idle = null;
        SimpleQueryResult nice = null;
        List<SimpleQueryResult> output = new ArrayList<SimpleQueryResult>(4);

        if (result.containsKey(Oid.CPU_TIME__USER)) {
            user = new SimpleQueryResult(result.get(Oid.CPU_TIME__USER).toDouble());
            user.setKey("user");
            user.setAttribute("cpu", "cpu0").setAttribute("state", "user");
            output.add(user);
        }
        if (result.containsKey(Oid.CPU_TIME__SYSTEM)) {
            system = new SimpleQueryResult(result.get(Oid.CPU_TIME__SYSTEM).toDouble());
            system.setKey("system");
            system.setAttribute("cpu", "cpu0").setAttribute("state", "system");
            output.add(system);
        }
        if (result.containsKey(Oid.CPU_TIME__IDLE)) {
            idle = new SimpleQueryResult(result.get(Oid.CPU_TIME__IDLE).toDouble());
            idle.setKey("idle");
            idle.setAttribute("cpu", "cpu0").setAttribute("state", "idle");
            output.add(idle);
        }
        if (result.containsKey(Oid.CPU_TIME__NICE)) {
            nice = new SimpleQueryResult(result.get(Oid.CPU_TIME__NICE).toDouble());
            nice.setKey("nice");
            nice.setAttribute("cpu", "cpu0").setAttribute("state", "nice");
            output.add(nice);
        }

        if (output.isEmpty()) {
            return null;
        }
        return output;
    }


    public static List<SimpleQueryResult> getMemUsageResults(Map<OID, SnmpValue> result) {
        List<SimpleQueryResult> output = new ArrayList<SimpleQueryResult>(4);
        if (!result.containsKey(Oid.MEMORY_USAGE__TOTAL)) {
            return null;
        }
        long total = SnmpValue.getLong(result, Oid.MEMORY_USAGE__TOTAL, 0L);
        if (total <= 0) {
            logger.log(Level.SEVERE, "Invalid value of total memory: " + total);
            return null;
        }
        long free = SnmpValue.getLong(result, Oid.MEMORY_USAGE__FREE, 0L);
        long cached = SnmpValue.getLong(result, Oid.MEMORY_USAGE__CACHED, 0L);
        long buffered = SnmpValue.getLong(result, Oid.MEMORY_USAGE__BUFFERED, 0L);
        long used = total - free - cached - buffered;

        output.add( new SimpleQueryResult(used * 1024).setKey("used").setAttribute("state", "used") );
        output.add( new SimpleQueryResult(free * 1024).setKey("free").setAttribute("state", "free") );
        output.add( new SimpleQueryResult(buffered * 1024).setKey("buffered").setAttribute("state", "buffered") );
        output.add( new SimpleQueryResult(cached * 1024).setKey("cached").setAttribute("state", "cached") );

        return output;
    }

}