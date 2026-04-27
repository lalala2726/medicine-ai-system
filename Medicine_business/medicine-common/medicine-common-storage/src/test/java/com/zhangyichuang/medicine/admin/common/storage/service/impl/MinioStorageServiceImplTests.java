package com.zhangyichuang.medicine.admin.common.storage.service.impl;

import com.zhangyichuang.medicine.admin.common.storage.config.MinioConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MinIO 存储服务 URL 规则测试。
 */
class MinioStorageServiceImplTests {

    /**
     * 默认 MinIO 端点地址。
     */
    private static final String MINIO_ENDPOINT = "http://localhost:9000/";

    /**
     * 默认文件桶名称。
     */
    private static final String BUCKET_NAME = "medicine";

    /**
     * CDN 公网访问前缀。
     */
    private static final String CDN_PUBLIC_URL_PREFIX = "http://localhost:9000/medicine/";

    /**
     * 测试用对象路径。
     */
    private static final String OBJECT_NAME = "/resources/2026/03/test-file.pdf";

    @Test
    void getFileUrlUsesConfiguredPublicUrlPrefix() {
        MinioStorageServiceImpl storageService = buildStorageService(MINIO_ENDPOINT, CDN_PUBLIC_URL_PREFIX, BUCKET_NAME);

        String actualFileUrl = storageService.getFileUrl(BUCKET_NAME, OBJECT_NAME);

        assertEquals("http://localhost:9000/medicine/resources/2026/03/test-file.pdf", actualFileUrl);
    }

    @Test
    void getFileUrlFallsBackToMinioEndpointWhenPublicPrefixMissing() {
        MinioStorageServiceImpl storageService = buildStorageService(MINIO_ENDPOINT, "", BUCKET_NAME);

        String actualFileUrl = storageService.getFileUrl(BUCKET_NAME, OBJECT_NAME);

        assertEquals("http://localhost:9000/medicine/resources/2026/03/test-file.pdf", actualFileUrl);
    }

    @Test
    void getNormalizedUploadPathDefaultsToResourcesWhenBlank() {
        MinioConfig minioConfig = buildMinioConfig(MINIO_ENDPOINT, CDN_PUBLIC_URL_PREFIX, BUCKET_NAME);
        minioConfig.setUploadPath("   ");

        String actualUploadPath = minioConfig.getNormalizedUploadPath();

        assertEquals("resources", actualUploadPath);
    }

    @Test
    void getNormalizedUploadPathTrimsSlashCharacters() {
        MinioConfig minioConfig = buildMinioConfig(MINIO_ENDPOINT, CDN_PUBLIC_URL_PREFIX, BUCKET_NAME);
        minioConfig.setUploadPath("/resources/");

        String actualUploadPath = minioConfig.getNormalizedUploadPath();

        assertEquals("resources", actualUploadPath);
    }

    /**
     * 功能描述：构建 MinIO 存储服务测试实例，仅注入配置对象用于验证 URL 生成逻辑。
     * <p>
     * 参数说明：
     *
     * @param endpoint        String MinIO 端点地址。
     * @param publicUrlPrefix String CDN 公网访问前缀。
     * @param bucketName      String 默认桶名称。
     *                        返回值：{@link MinioStorageServiceImpl}，用于当前测试的存储服务实例。
     *                        异常说明：无。
     */
    private MinioStorageServiceImpl buildStorageService(String endpoint, String publicUrlPrefix, String bucketName) {
        return new MinioStorageServiceImpl(null, buildMinioConfig(endpoint, publicUrlPrefix, bucketName));
    }

    /**
     * 功能描述：构建 MinIO 配置对象，供存储服务测试复用。
     * <p>
     * 参数说明：
     *
     * @param endpoint        String MinIO 端点地址。
     * @param publicUrlPrefix String CDN 公网访问前缀。
     * @param bucketName      String 默认桶名称。
     *                        返回值：{@link MinioConfig}，已填充基础字段的配置对象。
     *                        异常说明：无。
     */
    private MinioConfig buildMinioConfig(String endpoint, String publicUrlPrefix, String bucketName) {
        MinioConfig minioConfig = new MinioConfig();
        minioConfig.setEndpoint(endpoint);
        minioConfig.setPublicUrlPrefix(publicUrlPrefix);
        minioConfig.setBucketName(bucketName);
        return minioConfig;
    }
}
