package com.instana.vault.services.vault;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.api.Logical;
import com.bettercloud.vault.response.LogicalResponse;
import com.instana.vault.VaultService;
import com.instana.vault.VaultServiceConfig;
import com.instana.vault.services.vault.util.Constant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class VaultConfigTest {

    @Mock
    private VaultServiceConfig vaultServiceConfig = mock(VaultServiceConfig.class);

    @Mock
    private VaultService vaultService = mock(VaultService.class);

    @Mock
    private Vault vault = mock(Vault.class);

    @Mock
    private Logical logical = mock(Logical.class);

    @Mock
    private LogicalResponse logicalResponse = mock(LogicalResponse.class);

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() throws Exception {
        Field vaultClientField = VaultConfig.class.getDeclaredField("vaultClient");
        vaultClientField.setAccessible(true);
        vaultClientField.set(null, vault);
    }

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
    void testFetchSecret() throws Exception {
        // Arrange
        Map<String, Object> vaultSecrets = new HashMap<>();
        vaultSecrets.put(Constant.VAULT_SECRET_KEY, "someKey");
        vaultSecrets.put(Constant.VAULT_SECRET_PATH, "some/path");
        String secret = "secret";

        when(vault.logical()).thenReturn(logical);
        when(logical.read("some/path")).thenReturn(logicalResponse);
        Map<String, String> responseData = new HashMap<>();
        responseData.put("someKey", secret);
        when(logicalResponse.getData()).thenReturn(responseData);

        Method fetchSecretMethod = VaultConfig.class.getDeclaredMethod("fetchSecret", Map.class, VaultServiceConfig.class);
        fetchSecretMethod.setAccessible(true);

        // Act
        String result = (String) fetchSecretMethod.invoke(null, vaultSecrets, vaultServiceConfig);

        // Assert
        assertNotNull(result);
        assertEquals(Base64.getEncoder().encodeToString(secret.getBytes()), result);
    }

    @Test
    void testUpdateVaultSecretConfig() throws Exception {
        // Arrange
        Map<String, Object> vaultSecrets = new HashMap<>();
        vaultSecrets.put(Constant.VAULT_SECRET_KEY, "someKey");
        vaultSecrets.put(Constant.VAULT_SECRET_PATH, "some/path");

        ConcurrentHashMap<String, Object> instance = new ConcurrentHashMap<>();
        instance.put("someProperty", vaultSecrets);
        instance.put("db.address", "testAddress");

        List<ConcurrentHashMap<String, Object>> instances = Arrays.asList(instance);

        when(vaultService.getInstances()).thenReturn(instances);
        when(vaultService.getVaultServiceConfig()).thenReturn(vaultServiceConfig);

        when(vault.logical()).thenReturn(logical);
        when(logical.read("some/path")).thenReturn(logicalResponse);
        Map<String, String> responseData = new HashMap<>();
        responseData.put("someKey", "secret");
        when(logicalResponse.getData()).thenReturn(responseData);

        Method updateVaultSecretConfigMethod = VaultConfig.class.getDeclaredMethod("updateVaultSecretConfig", VaultService.class);
        updateVaultSecretConfigMethod.setAccessible(true);

        // Act
        VaultService result = (VaultService) updateVaultSecretConfigMethod.invoke(null, vaultService);

        // Assert
        assertNotNull(result);
        assertEquals(Base64.getEncoder().encodeToString("secret".getBytes()), instance.get("someProperty"));
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
        String result = VaultConfig.read(path, keyName);

        // Assert
        assertNotNull(result);
        assertEquals(secret, result);
    }

    @Test
    public void testRead_Failure() throws VaultException {
        String path = "invalid/path";
        String keyName = "invalidKey";

        when(vault.logical()).thenReturn(logical);
        when(logical.read(path)).thenThrow(new VaultException("Mocked VaultException"));

        String result = VaultConfig.read(path, keyName);

        assertNull(result);
    }
}


