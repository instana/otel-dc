package com.instana.vault;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import com.bettercloud.vault.VaultException;
import com.instana.vault.services.vault.VaultConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;


public class VaultUtilTest {

    @Mock
    private final VaultService vaultService = mock(VaultService.class);

    @Test
    void testIsVaultConfigured_ConfigPresent() throws VaultException {
        // Arrange
        when(vaultService.isVaultServiceConfigPresent()).thenReturn(true);

        try (MockedStatic<VaultConfig> mockedVaultConfig = mockStatic(VaultConfig.class)) {
            mockedVaultConfig.when(() -> VaultConfig.executeStep(vaultService)).thenReturn(vaultService);

            // Act
            VaultService result = VaultUtil.isVaultConfigured(vaultService);

            // Assert
            assertNotNull(result);
            assertSame(vaultService, result);
            verify(vaultService).isVaultServiceConfigPresent();
            mockedVaultConfig.verify(() -> VaultConfig.executeStep(vaultService));
        }
    }

    @Test
    void testIsVaultConfigured_ConfigNotPresent() throws VaultException {
        // Arrange
        when(vaultService.isVaultServiceConfigPresent()).thenReturn(false);

        try (MockedStatic<VaultConfig> mockedVaultConfig = mockStatic(VaultConfig.class)) {

            // Act
            VaultService result = VaultUtil.isVaultConfigured(vaultService);

            // Assert
            assertNotNull(result);
            assertSame(vaultService, result);
            verify(vaultService).isVaultServiceConfigPresent();
            mockedVaultConfig.verify(() -> VaultConfig.executeStep(vaultService), never());
        }
    }
}
