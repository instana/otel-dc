/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.instana.vault.VaultService;
import com.instana.vault.VaultServiceConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DcConfig implements VaultService {

    @JsonProperty("db.system")
    private String dbSystem;

    @JsonProperty("db.driver")
    private String dbDriver;

    private final List<ConcurrentHashMap<String, Object>> instances = new ArrayList<>();

    @JsonProperty(value = "vault", required = false)
    private VaultServiceConfig vaultConfig;

    @Override
    @JsonIgnore
    public boolean isVaultServiceConfigPresent() {
        return Optional.ofNullable(vaultConfig).isPresent();
    }

    @Override
    public VaultServiceConfig getVaultServiceConfig() {
        return vaultConfig;
    }

    public String getDbSystem() {
        return dbSystem;
    }

    public String getDbDriver() {
        return dbDriver;
    }

    public List<ConcurrentHashMap<String, Object>> getInstances() {
        return instances;
    }
}
