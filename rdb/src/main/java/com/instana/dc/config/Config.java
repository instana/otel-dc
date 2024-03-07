package com.instana.dc.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Config {
    @JsonProperty("db.system")
    private String dbSystem;
    @JsonProperty("db.driver")
    private String dbDriver;
    private final List<ConcurrentHashMap<String, Object>> instances = new ArrayList<>();
    @JsonProperty(value="vault",required=false)
    private VaultServiceConfig vaultConfig;

    @JsonIgnore
    public boolean isVaultServiceConfigPresent(){
        return Optional.ofNullable(vaultConfig).isPresent();
    }
    public VaultServiceConfig getVaultServiceConfig(){
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
