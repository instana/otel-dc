/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */
package com.instana.simpsnmp;

import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;

public class SnmpOption {
    private String community;
    private int retries;
    private int timeout;
    private int version;
    private int securityLevel;
    private String authPassword;
    private String privacyPassword;

    public SnmpOption() {
        this.community = "public";
        this.retries = 3;
        this.timeout = 450;
        this.version = SnmpConstants.version2c; //1
        this.securityLevel = SecurityLevel.NOAUTH_NOPRIV; //1
    }

    public String getCommunity() {
        return community;
    }

    public void setCommunity(String community) {
        this.community = community;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(int securityLevel) {
        this.securityLevel = securityLevel;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }

    public String getPrivacyPassword() {
        return privacyPassword;
    }

    public void setPrivacyPassword(String privacyPassword) {
        this.privacyPassword = privacyPassword;
    }
}
