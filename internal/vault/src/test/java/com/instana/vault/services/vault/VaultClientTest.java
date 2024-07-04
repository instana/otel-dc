package com.instana.vault.services.vault;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.instana.vault.VaultServiceConfig;
import com.instana.vault.services.vault.VaultClient;
import com.instana.vault.services.vault.auth.AuthenticationFactory;
import com.instana.vault.services.vault.auth.strategy.VaultAuthenticationStrategy;
import com.instana.vault.services.vault.util.Constant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

public class VaultClientTest {

    @Mock
    private VaultServiceConfig vaultServiceConfig = mock(VaultServiceConfig.class);

    @Mock
    private VaultConfig vaultConfig = mock(VaultConfig.class);

    @Mock
    private VaultAuthenticationStrategy vaultAuthenticationStrategy = mock(VaultAuthenticationStrategy.class);

    @Test
    public void testCreateVaultClient() throws Exception {
        // Arrange
        String mockURL = "http://mockurl.com";
        when(vaultServiceConfig.getConnectionURL()).thenReturn(mockURL);
        when(vaultServiceConfig.getKvVersion()).thenReturn(2);

        VaultConfig mockVaultConfig = mock(VaultConfig.class);
        when(mockVaultConfig.address(mockURL)).thenReturn(mockVaultConfig);
        when(mockVaultConfig.sslConfig(any(SslConfig.class))).thenReturn(mockVaultConfig);
        when(mockVaultConfig.token(anyString())).thenReturn(mockVaultConfig);
        when(mockVaultConfig.build()).thenReturn(mockVaultConfig);

        when(vaultServiceConfig.isPathToPEMFilePresent()).thenReturn(true);
        when(vaultServiceConfig.getPathToPEMFile()).thenReturn(Optional.of("path/to/pem"));

        try (MockedStatic<AuthenticationFactory> mockedFactory = mockStatic(AuthenticationFactory.class)) {
            mockedFactory.when(() -> AuthenticationFactory.getVaultAuthStrategyFromConfig(vaultServiceConfig))
                    .thenReturn(vaultAuthenticationStrategy);
            when(vaultAuthenticationStrategy.token()).thenReturn(Optional.of("mockToken"));

            // Act
            Vault result = VaultClient.createVaultClient(vaultServiceConfig);

            // Assert
            assertNotNull(result);
        }
    }
}
