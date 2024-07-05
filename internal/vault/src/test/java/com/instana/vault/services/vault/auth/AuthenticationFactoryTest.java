package com.instana.vault.services.vault.auth;

import com.instana.vault.VaultServiceConfig;
import com.instana.vault.VaultServiceConfigTest;
import com.instana.vault.services.vault.auth.strategy.TokenAuthenticationStrategy;
import com.instana.vault.services.vault.auth.strategy.VaultAuthenticationStrategy;
import com.instana.vault.services.vault.util.Constant;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticationFactoryTest {

    @Mock
    private VaultServiceConfig vaultServiceConfig = mock(VaultServiceConfig.class);

    @Test
    public void testGetVaultAuthStrategyFromConfig() {
        Map<String, Object> authConfig = new HashMap<>();
        authConfig.put(Constant.TOKEN, "my-token");
        when(vaultServiceConfig.getAuthConfig()).thenReturn(authConfig);

        VaultAuthenticationStrategy strategy = AuthenticationFactory.getVaultAuthStrategyFromConfig(vaultServiceConfig);
        assertInstanceOf(TokenAuthenticationStrategy.class, strategy);
    }
}
