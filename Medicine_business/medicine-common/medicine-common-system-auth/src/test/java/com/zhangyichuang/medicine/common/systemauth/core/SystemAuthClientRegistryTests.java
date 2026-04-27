package com.zhangyichuang.medicine.common.systemauth.core;

import com.zhangyichuang.medicine.common.systemauth.config.SystemAuthProperties;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemAuthClientRegistryTests {

    @Test
    void findEnabledClient_ShouldReturnMatchedClient() {
        SystemAuthProperties properties = new SystemAuthProperties();
        properties.setClientsJson("[{\"app_id\":\"medicine-admin\",\"secret\":\"s1\",\"enabled\":true}]");
        SystemAuthClientRegistry registry = new SystemAuthClientRegistry(properties);

        Optional<SystemAuthClientRegistry.SystemAuthClient> client = registry.findEnabledClient("medicine-admin");

        assertTrue(client.isPresent());
        assertEquals("medicine-admin", client.get().getAppId());
        assertEquals("s1", client.get().getSecret());
    }

    @Test
    void findEnabledClient_WhenClientDisabled_ShouldReturnEmpty() {
        SystemAuthProperties properties = new SystemAuthProperties();
        properties.setClientsJson("[{\"app_id\":\"medicine-admin\",\"secret\":\"s1\",\"enabled\":false}]");
        SystemAuthClientRegistry registry = new SystemAuthClientRegistry(properties);

        Optional<SystemAuthClientRegistry.SystemAuthClient> client = registry.findEnabledClient("medicine-admin");

        assertTrue(client.isEmpty());
    }

    @Test
    void findEnabledClient_WhenJsonInvalid_ShouldReturnEmpty() {
        SystemAuthProperties properties = new SystemAuthProperties();
        properties.setClientsJson("{not-json}");
        SystemAuthClientRegistry registry = new SystemAuthClientRegistry(properties);

        Optional<SystemAuthClientRegistry.SystemAuthClient> client = registry.findEnabledClient("medicine-admin");

        assertTrue(client.isEmpty());
    }
}
