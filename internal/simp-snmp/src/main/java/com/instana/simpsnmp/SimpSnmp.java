/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */
package com.instana.simpsnmp;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple SNMP agent
 */
public class SimpSnmp implements Closeable {
    private final String endpoint;
    private final String community;
    private final int retries;
    private final int timeout;
    private final int version;

    private final CommunityTarget<Address> myTarget;
    private final TransportMapping<UdpAddress> transport;
    private final Snmp protocol;

    public SimpSnmp(String endpoint, String community, Integer retries, Integer timeout, Integer version) throws IOException {
        this.endpoint = endpoint;
        this.community = community != null ? community : "public";
        this.retries = retries != null ? retries : 3;
        this.timeout = timeout != null ? timeout : 600;
        this.version = version != null ? version : SnmpConstants.version2c;

        myTarget = new CommunityTarget<>();
        Address deviceAdd = GenericAddress.parse(this.endpoint);
        myTarget.setAddress(deviceAdd);
        myTarget.setCommunity(new OctetString(this.community));
        myTarget.setRetries(this.retries);
        myTarget.setTimeout(this.timeout);
        myTarget.setVersion(this.version);

        transport = new DefaultUdpTransportMapping();
        transport.listen();
        protocol = new Snmp(transport);
    }

    public SimpSnmp(String endpoint, String community) throws IOException {
        this(endpoint, community, null, null, null);
    }

    public SimpSnmp(String endpoint) throws IOException {
        this(endpoint, null, null, null, null);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getCommunity() {
        return community;
    }

    public int getRetries() {
        return retries;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getVersion() {
        return version;
    }

    public TransportMapping<UdpAddress> getTransport() {
        return transport;
    }

    public Snmp getProtocol() {
        return protocol;
    }

    public void close() throws IOException {
        transport.close();
    }

    public Map<OID, SnmpValue> queryScalarOids(List<OID> oids) throws IOException {
        PDU request = new PDU();
        request.setType(PDU.GET);
        for (OID oid : oids) {
            request.add(new VariableBinding(oid));
        }
        ResponseEvent<Address> responseEvent = protocol.send(request, myTarget);
        PDU response = responseEvent.getResponse();
        Map<OID, SnmpValue> result = new HashMap<>();
        if (response != null) {
            for (int i = 0; i < response.size(); i++) {
                VariableBinding vb = response.get(i);
                SnmpValue snmpValue = SnmpValue.getValue(vb);
                if (snmpValue != null) {
                    result.put(snmpValue.getOid(), snmpValue);
                }
            }
        }
        return result;
    }

    public Map<OID, SnmpValue> queryScalarOids(OID... oids) throws IOException {
        return queryScalarOids(Arrays.stream(oids).collect(Collectors.toList()));
    }

    private static OID findParentOid(List<OID> oids, OID oid1) {
        for (OID oid : oids) {
            if (oid1.startsWith(oid)) {
                return oid;
            }
        }
        return oid1;
    }

    public List<Map<OID, SnmpValue>> queryColumnOids(List<OID> oids) throws IOException {
        TableUtils tableUtils = new TableUtils(protocol, new DefaultPDUFactory(PDU.GETBULK));

        List<TableEvent> events = tableUtils.getTable(myTarget, oids.toArray(OID[]::new), null, null);

        List<Map<OID, SnmpValue>> result = new ArrayList<>();
        for (TableEvent event : events) {
            if (event.isError()) {
                System.err.println("SNMP Error: " + event.getErrorMessage());
            } else {
                VariableBinding[] vbs = event.getColumns();
                if (vbs != null) {
                    Map<OID, SnmpValue> result1 = new HashMap<>();
                    for (VariableBinding vb : vbs) {
                        SnmpValue snmpValue = SnmpValue.getValue(vb);
                        if (snmpValue != null) {
                            result1.put(findParentOid(oids, snmpValue.getOid()), snmpValue);
                        }
                    }
                    result.add(result1);
                }
            }
        }
        return result;
    }

    public List<Map<OID, SnmpValue>> queryColumnOids(OID... oids) throws IOException {
        return queryColumnOids(Arrays.stream(oids).collect(Collectors.toList()));
    }

}