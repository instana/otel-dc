package com.instana.vault.services.vault.auth;

import java.util.Optional;

public class TokenAuthenticationStrategy implements VaultAuthenticationStrategy {
    private final String token;

    public TokenAuthenticationStrategy(final String token) {
        this.token = token;
    }

    @Override
    public Optional<String> token() {
        return Optional.ofNullable(token);
    }


}
