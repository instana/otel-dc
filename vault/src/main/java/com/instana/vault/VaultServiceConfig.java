package com.instana.vault;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Optional;

public class VaultServiceConfig {
    @JsonProperty("connection_url")
    private String connectionURL;
    @JsonAnySetter
    private Map<String, Object> authConfig;
    @JsonProperty("secret_refresh_rate")
    private Integer secretRefreshRate;
    @JsonProperty("kv_version")
    private Integer kvVersion;
    @JsonProperty(value = "path_to_pem_file", required = false)
    private Optional<String> path_to_pem_file;

    public String getConnectionURL() {
        return connectionURL;
    }

    public Map<String, Object> getAuthConfig() {
        return authConfig;
    }

    public Integer getKvVersion() {
        return kvVersion;
    }

    public Integer getSecretRefreshRate() {
        return secretRefreshRate;
    }

    public String getPathToPEMFile() {
        return String.valueOf(path_to_pem_file.get());
    }

    public boolean isPathToPEMFilePresent() {
        return path_to_pem_file.isPresent();
    }
}
