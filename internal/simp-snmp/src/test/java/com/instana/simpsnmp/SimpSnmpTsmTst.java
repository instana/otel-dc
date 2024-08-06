package com.instana.simpsnmp;

import org.snmp4j.smi.OID;

import java.util.Map;

public class SimpSnmpTsmTst {

    public static void main(String[] args) {
        try {
            // Set java keystore manually
            System.setProperty("javax.net.ssl.keyStore", "/tmp/lr/manager.keystore");
            System.setProperty("javax.net.ssl.keyStorePassword", "password");
            System.setProperty("javax.net.ssl.trustStore", "/tmp/lr/snmpd.keystore");
            System.setProperty("javax.net.ssl.trustStorePassword", "password");
            System.setProperty("java.net.preferIPv4Stack", "true");

            SnmpOption option = new SnmpOption();
            option.setVersion(3);
            option.setSecurityLevel(3);
            option.setTsmEnabled(true);
            option.setSecurityName("liurui");
            SimpSnmp simpSnmp = new SimpSnmp("dtls:dk-liurui1.fyre.ibm.com/10161", option);
            Map<OID, SnmpValue> result = simpSnmp.queryScalarOids(Oid.HOST_NAME, Oid.OS_TYPE);
            String hostName0 = SnmpValue.getString(result, Oid.HOST_NAME, null);
            String uname0 = SnmpValue.getString(result, Oid.OS_TYPE, null);
            System.out.println(hostName0 + ": " + uname0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
