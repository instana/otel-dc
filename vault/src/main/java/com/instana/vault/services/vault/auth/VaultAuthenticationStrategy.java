package com.instana.vault.services.vault.auth;

import com.bettercloud.vault.Vault;

import java.util.Optional;

public interface VaultAuthenticationStrategy {
    default Optional<String> authenticate(Vault vault) {
        return Optional.empty();
    }

    default Optional<String> token() {
        return Optional.empty();
    }

    default Optional<String> github() {
        return Optional.empty();
    }

    default Optional<String> roleID() {
        return Optional.empty();
    }

    default Optional<String> secretID() {
        return Optional.empty();
    }

}
