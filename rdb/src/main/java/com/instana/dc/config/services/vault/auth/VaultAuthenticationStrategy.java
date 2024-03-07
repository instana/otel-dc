package com.instana.dc.config.services.vault.auth;

import com.bettercloud.vault.Vault;

import java.util.Optional;

public interface VaultAuthenticationStrategy {
    default Optional<String> authenticate(Vault vault) {
        return Optional.empty();
    }

    default Optional<String> token() {
        return Optional.empty();
    }

}
