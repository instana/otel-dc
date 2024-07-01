package com.instana.vault.services.vault;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.api.Logical;
import com.bettercloud.vault.response.LogicalResponse;
import com.instana.vault.VaultService;
import com.instana.vault.VaultServiceConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;

public class VaultConfigTest {

    @Mock
    private VaultServiceConfig vaultServiceConfig = mock(VaultServiceConfig.class);

    @Mock
    private VaultService vaultService = mock(VaultService.class);

    @Mock
    private Vault vault = mock(Vault.class);

    @Mock
    private VaultClient vaultClient = mock(VaultClient.class);

    @Mock
    private Logical logical = mock(Logical.class);

    @Mock
    private LogicalResponse logicalResponse = mock(LogicalResponse.class);

    @Test
    void testExecuteStep() throws VaultException {
        // Arrange
        when(vaultService.getVaultServiceConfig()).thenReturn(vaultServiceConfig);
        try (MockedStatic<VaultClient> mockedVaultClient = mockStatic(VaultClient.class)) {
            mockedVaultClient.when(() -> VaultClient.createVaultClient(vaultServiceConfig)).thenReturn(vault);

            // Act
            VaultService result = VaultConfig.executeStep(vaultService);

            // Assert
            assertNotNull(result);
            verify(vaultService).getVaultServiceConfig();
            mockedVaultClient.verify(() -> VaultClient.createVaultClient(vaultServiceConfig));
        }
    }

    @Test
    void testRead_Success() throws VaultException {
        // Arrange
        String path = "some/path";
        String keyName = "someKey";
        String secret = "secret";

        Map<String, String> data = new HashMap<>();
        data.put(keyName, secret);

        when(vault.logical()).thenReturn(logical);
        when(logical.read(path)).thenReturn(logicalResponse);
        when(logicalResponse.getData()).thenReturn(data);

        // Act
        String result = vault.logical().read(path).getData().get(keyName);

        // Assert
        assertNotNull(result);
        assertEquals(secret, result);
    }
}

