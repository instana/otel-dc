package com.instana.vault;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

public class VaultServiceConfigTest {

    private VaultServiceConfig config;

    @BeforeEach
    public void setUp() {
        config = new VaultServiceConfig();
    }

    @Test
    public void testGetConnectionURL() throws Exception {
        String json = "{\"connection_url\": \"http://localhost:8200\"}";
        ObjectMapper mapper = new ObjectMapper();
        config = mapper.readValue(json, VaultServiceConfig.class);
        assertEquals("http://localhost:8200", config.getConnectionURL());
    }
    @Test
    public void testGetAuthConfig() throws Exception {
        String json = "{\"auth_method\": \"token\", \"token\": \"s.1234567890\"}";
        ObjectMapper mapper = new ObjectMapper();
        config = mapper.readValue(json, VaultServiceConfig.class);
        Map<String, Object> expectedAuthConfig = new HashMap<>();
        expectedAuthConfig.put("auth_method", "token");
        expectedAuthConfig.put("token", "s.1234567890");

        assertEquals(expectedAuthConfig, config.getAuthConfig());
    }

    @Test
    public void testGetSecretRefreshRate() throws Exception {
        String json = "{\"secret_refresh_rate\": 3600}";
        ObjectMapper mapper = new ObjectMapper();
        config = mapper.readValue(json, VaultServiceConfig.class);

        assertEquals(3600, config.getSecretRefreshRate());
    }

    @Test
    public void testGetKvVersion() throws Exception {
        String json = "{\"kv_version\": 2}";
        ObjectMapper mapper = new ObjectMapper();
        config = mapper.readValue(json, VaultServiceConfig.class);

        assertEquals(2, config.getKvVersion());
    }

    @Test
    public void testGetPathToPEMFile() throws Exception {
        String json = "{\"path_to_pem_file\": \"/path/to/pemfile.pem\"}";
        ObjectMapper mapper = new ObjectMapper();
        config = mapper.readValue(json, VaultServiceConfig.class);

        assertTrue(config.getPathToPEMFile().isPresent());
        assertEquals("/path/to/pemfile.pem", config.getPathToPEMFile().get());
    }

    @Test
    public void testIsPathToPEMFilePresent() throws Exception {
        String json = "{\"path_to_pem_file\": \"/path/to/pemfile.pem\"}";
        ObjectMapper mapper = new ObjectMapper();
        config = mapper.readValue(json, VaultServiceConfig.class);

        assertTrue(config.isPathToPEMFilePresent());

        json = "{}";
        config = mapper.readValue(json, VaultServiceConfig.class);
        assertFalse(config.isPathToPEMFilePresent());
    }
}
