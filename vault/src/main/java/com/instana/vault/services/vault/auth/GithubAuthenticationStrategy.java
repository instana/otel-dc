package com.instana.vault.services.vault.auth;

import java.util.Optional;

public class GithubAuthenticationStrategy implements VaultAuthenticationStrategy {
    private final String github;

    public GithubAuthenticationStrategy(final String github) {
        this.github = github;
    }

    @Override
    public Optional<String> github() {
        return Optional.ofNullable(github);
    }


}
