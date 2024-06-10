package com.instana.vault.services.vault.auth;

import com.instana.vault.VaultServiceConfig;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

public class AuthFactory {
    private static final Logger logger = Logger.getLogger(AuthFactory.class.getName());

    public static VaultAuthenticationStrategy getVaultAuthStrategyFromConfig(VaultServiceConfig vaultServiceConfig) {
        Map<String, Object> authConfig = vaultServiceConfig.getAuthConfig();
        String authType = new ArrayList<>(authConfig.keySet()).get(0);
        return getVaultAuthStrategy(authType, authConfig);
    }

    public static VaultAuthenticationStrategy getVaultAuthStrategy(String authType, Map<String, Object> authConfig) {
        logger.info("auth type: " + authType);
        VaultAuthenticationStrategy vaultAuthenticationStrategy = null;
        switch (authType) {
            case "token":
                vaultAuthenticationStrategy = new TokenAuthenticationStrategy((String) authConfig.get(authType));
                break;
            case "github":
                vaultAuthenticationStrategy = new GithubAuthenticationStrategy((String) authConfig.get(authType));
                break;
            case "approle":
                Map<String, String> approleConfig = (Map<String, String>) authConfig.get(authType);
                String roleId = approleConfig.get("roleID");
                String secretId = approleConfig.get("secretID");
                vaultAuthenticationStrategy = new AppRoleAuthenticationStrategy(roleId, secretId);
                break;
            default:

        }
        return vaultAuthenticationStrategy;
    }

}
