package com.zhangyichuang.medicine.admin.common.storage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件上传相关配置属性，仅负责基础校验配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {

    /**
     * 允许的文件类型列表，支持 MIME 类型与文件扩展名（需包含 "." 前缀）。
     */
    private List<String> allowedTypes = List.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/pdf",
            "text/plain",
            ".txt",
            ".md",
            ".pdf",
            ".docx",
            ".doc",
            ".pptx",
            ".ppt",
            ".xlsx",
            ".xls"
    );

    /**
     * 功能描述：获取标准化后的允许文件类型集合（去空格、去重并统一为小写）。
     * <p>
     * 参数说明：无。
     * 返回值：{@code Set<String>}，允许的类型集合；当配置为空时返回空集合。
     * 异常说明：无。
     */
    public Set<String> getAllowedTypeSet() {
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            return Collections.emptySet();
        }
        return allowedTypes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(type -> type.toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
