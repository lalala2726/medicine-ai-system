package com.zhangyichuang.medicine.common.systemauth.core;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import com.zhangyichuang.medicine.common.systemauth.config.SystemAuthProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.*;

/**
 * 系统客户端注册表（读取并缓存 SYSTEM_AUTH_CLIENTS_JSON）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemAuthClientRegistry {

    /**
     * 客户端注册 JSON 数组反序列化类型。
     */
    private static final Type CLIENT_LIST_TYPE = new TypeToken<List<SystemAuthClient>>() {
    }.getType();

    private final SystemAuthProperties properties;

    /**
     * 上一次成功解析的原始 JSON。
     */
    private volatile String cachedRawJson = "[]";

    /**
     * 当前客户端快照缓存。
     */
    private volatile Map<String, SystemAuthClient> cachedClients = Map.of();

    /**
     * 按 appId 查询已启用客户端。
     *
     * @param appId 调用方 app_id
     * @return 已启用客户端信息；不存在或被禁用时返回空
     */
    public Optional<SystemAuthClient> findEnabledClient(String appId) {
        if (StringUtils.isBlank(appId)) {
            return Optional.empty();
        }
        SystemAuthClient client = getClientsSnapshot().get(appId);
        if (client == null || !Boolean.TRUE.equals(client.getEnabled())) {
            return Optional.empty();
        }
        return Optional.of(client);
    }

    /**
     * 读取并按需刷新客户端快照。
     */
    private Map<String, SystemAuthClient> getClientsSnapshot() {
        String rawJson = StringUtils.defaultIfBlank(properties.getClientsJson(), "[]");
        if (Objects.equals(rawJson, cachedRawJson)) {
            return cachedClients;
        }
        synchronized (this) {
            if (Objects.equals(rawJson, cachedRawJson)) {
                return cachedClients;
            }
            cachedClients = parseClients(rawJson);
            cachedRawJson = rawJson;
            return cachedClients;
        }
    }

    /**
     * 将配置中的客户端 JSON 解析为按 appId 建立的索引。
     */
    private Map<String, SystemAuthClient> parseClients(String rawJson) {
        try {
            List<SystemAuthClient> clientList = JSONUtils.fromJson(rawJson, CLIENT_LIST_TYPE);
            if (clientList == null || clientList.isEmpty()) {
                return Map.of();
            }
            Map<String, SystemAuthClient> result = new LinkedHashMap<>();
            for (SystemAuthClient client : clientList) {
                if (client == null || StringUtils.isBlank(client.getAppId()) || StringUtils.isBlank(client.getSecret())) {
                    continue;
                }
                String appId = client.getAppId().trim();
                if (!result.containsKey(appId)) {
                    SystemAuthClient normalized = new SystemAuthClient();
                    normalized.setAppId(appId);
                    normalized.setSecret(client.getSecret().trim());
                    normalized.setEnabled(client.getEnabled() == null || client.getEnabled());
                    result.put(appId, normalized);
                }
            }
            return result;
        } catch (RuntimeException ex) {
            log.error("Parse system auth clients json failed: {}", ex.getMessage());
            return Map.of();
        }
    }

    /**
     * 单个系统客户端配置。
     */
    @Data
    public static class SystemAuthClient {
        /**
         * 客户端 app_id。
         */
        @SerializedName("app_id")
        private String appId;

        /**
         * 客户端密钥。
         */
        private String secret;

        /**
         * 是否启用。
         */
        private Boolean enabled = Boolean.TRUE;

    }
}
