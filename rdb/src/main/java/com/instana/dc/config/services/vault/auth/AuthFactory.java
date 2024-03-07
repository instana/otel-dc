package com.instana.dc.config.services.vault.auth;

import com.instana.dc.config.VaultServiceConfig;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

public class AuthFactory {
    private static final Logger logger = Logger.getLogger(AuthFactory.class.getName());

    public static VaultAuthenticationStrategy getVaultAuthStrategyFromConfig(VaultServiceConfig vaultServiceConfig){
        Map<String,Object> authConfig = vaultServiceConfig.getAuthConfig();
        String authType = new ArrayList<>(authConfig.keySet()).get(0);
        return getVaultAuthStrategy(authType, authConfig);
    }
    public static VaultAuthenticationStrategy getVaultAuthStrategy(String authType,Map<String,Object> authConfig){
        logger.info("auth type: "+authType);
        VaultAuthenticationStrategy vaultAuthenticationStrategy = null;
        switch (authType){
            case "token":
                vaultAuthenticationStrategy = new TokenAuthenticationStrategy((String) authConfig.get(authType));
                break;
            case "github":
                vaultAuthenticationStrategy = new GithubAuthenticationStrategy();
                break;
            case "approle":
                vaultAuthenticationStrategy = new AppRoleAuthenticationStrategy();
                break;
            default:

        }
        return vaultAuthenticationStrategy;
    }

}
