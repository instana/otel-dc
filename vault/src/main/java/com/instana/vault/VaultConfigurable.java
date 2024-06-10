package com.instana.vault;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public interface VaultConfigurable {
    boolean isVaultServiceConfigPresent();

    VaultServiceConfig getVaultServiceConfig();

    List<ConcurrentHashMap<String, Object>> getInstances();
}
