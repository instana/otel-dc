package com.instana.vault.services.vault.auth;

import java.util.Optional;

public class AppRoleAuthenticationStrategy implements VaultAuthenticationStrategy {
    private final String roleId;
    private final String secretId;

    public AppRoleAuthenticationStrategy(final String roleId, final String secretId) {
        this.roleId = roleId;
        this.secretId = secretId;
    }

    @Override
    public Optional<String> roleID() {
        return Optional.ofNullable(roleId);
    }

    @Override
    public Optional<String> secretID() {
        return Optional.ofNullable(secretId);
    }
}
