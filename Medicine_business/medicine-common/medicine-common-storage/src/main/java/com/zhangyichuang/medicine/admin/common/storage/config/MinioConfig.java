package com.zhangyichuang.medicine.admin.common.storage.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO配置类
 *
 * @author Chuang
 * created on 2025/9/25
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    /**
     * 默认上传路径前缀，防止对象直接落在桶根目录。
     */
    private static final String DEFAULT_UPLOAD_PATH = "resources";

    /**
     * MinIO 服务端点地址。
     */
    private String endpoint;

    /**
     * MinIO 访问账号。
     */
    private String accessKey;

    /**
     * MinIO 访问密钥。
     */
    private String secretKey;

    /**
     * MinIO 默认桶名称。
     */
    private String bucketName;

    /**
     * 文件公网访问前缀，优先用于生成对外 fileUrl。
     */
    private String publicUrlPrefix;

    /**
     * 文件上传路径前缀，最终会拼接到对象路径最前面。
     */
    private String uploadPath;

    /**
     * 功能描述：获取标准化后的 MinIO 服务端点地址，移除末尾多余的斜杠。
     * <p>
     * 参数说明：无。
     * 返回值：{@code String}，标准化后的端点地址；未配置时返回原始空值。
     * 异常说明：无。
     */
    public String getNormalizedEndpoint() {
        return normalizeBaseUrl(endpoint);
    }

    /**
     * 功能描述：获取标准化后的文件公网访问前缀，移除末尾多余的斜杠。
     * <p>
     * 参数说明：无。
     * 返回值：{@code String}，标准化后的公网访问前缀；未配置时返回原始空值。
     * 异常说明：无。
     */
    public String getNormalizedPublicUrlPrefix() {
        return normalizeBaseUrl(publicUrlPrefix);
    }

    /**
     * 功能描述：获取标准化后的上传路径前缀，未配置时默认回退为 resources。
     * <p>
     * 参数说明：无。
     * 返回值：{@code String}，标准化后的上传路径前缀，且一定不带首尾斜杠。
     * 异常说明：无。
     */
    public String getNormalizedUploadPath() {
        return normalizePath(uploadPath);
    }

    /**
     * 功能描述：标准化基础地址，统一去掉结尾的斜杠，避免后续 URL 拼接出现双斜杠。
     * <p>
     * 参数说明：
     *
     * @param baseUrl String 待标准化的基础地址。
     *                返回值：{@code String}，标准化后的地址；当输入为空白时原样返回。
     *                异常说明：无。
     */
    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return baseUrl;
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * 功能描述：标准化路径片段，移除首尾斜杠并在空白场景下回退到默认上传路径。
     * <p>
     * 参数说明：
     *
     * @param rawPath String 待标准化的路径片段。
     *                返回值：{@code String}，标准化后的路径片段。
     *                异常说明：无。
     */
    private String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return DEFAULT_UPLOAD_PATH;
        }
        String normalizedPath = rawPath.trim();
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        while (normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        return normalizedPath.isBlank() ? DEFAULT_UPLOAD_PATH : normalizedPath;
    }

    /**
     * 功能描述：创建 MinIO 客户端，供文件上传、删除和下载时复用。
     * <p>
     * 参数说明：无。
     * 返回值：{@link MinioClient}，已注入账号和密钥的 MinIO 客户端实例。
     * 异常说明：当 endpoint 或凭证非法时，MinIO SDK 可能抛出运行时异常。
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
