package com.zhangyichuang.medicine.admin.support;

import java.util.List;

/**
 * Agent 提示词业务键目录。
 */
public final class AgentPromptKeyCatalog {

    /**
     * 预置提示词键列表。
     */
    private static final List<PromptKeyMeta> DEFAULT_KEYS = List.of(
            new PromptKeyMeta(
                    "admin_assistant_system_prompt",
                    "管理端助手主系统提示词"
            ),
            new PromptKeyMeta(
                    "client_service_node_system_prompt",
                    "客户端服务节点系统提示词"
            ),
            new PromptKeyMeta(
                    "client_medical_node_system_prompt",
                    "客户端医疗节点系统提示词"
            ),
            new PromptKeyMeta(
                    "client_base_prompt",
                    "客户端基础提示词"
            ),
            new PromptKeyMeta(
                    "system_base_prompt",
                    "系统通用基础提示词"
            )
    );

    private AgentPromptKeyCatalog() {
    }

    /**
     * 读取预置提示词键列表。
     *
     * @return 预置提示词键列表
     */
    public static List<PromptKeyMeta> listDefaultKeys() {
        return DEFAULT_KEYS;
    }

    /**
     * 判断是否为受支持的提示词键。
     *
     * @param promptKey 提示词业务键
     * @return 是否受支持
     */
    public static boolean containsPromptKey(String promptKey) {
        if (promptKey == null || promptKey.isBlank()) {
            return false;
        }
        for (PromptKeyMeta promptKeyMeta : DEFAULT_KEYS) {
            if (promptKeyMeta.promptKey().equals(promptKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 预置提示词键元数据。
     *
     * @param promptKey   提示词业务键
     * @param description 提示词用途说明
     */
    public record PromptKeyMeta(
            String promptKey,
            String description
    ) {
    }
}
