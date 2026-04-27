package com.zhangyichuang.medicine.admin.common.storage.service.impl;

import com.zhangyichuang.medicine.admin.common.storage.config.MinioConfig;
import com.zhangyichuang.medicine.admin.common.storage.service.MinioStorageService;
import io.minio.*;
import io.minio.messages.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * MinIO存储服务实现类
 *
 * @author Chuang
 * created on 2025/9/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageServiceImpl implements MinioStorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    public void checkBucketExists(String bucketName) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket '{}' created successfully", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to check/create bucket: {}", bucketName, e);
            throw new RuntimeException("Failed to check/create bucket: " + bucketName, e);
        }
    }

    @Override
    public String uploadFile(String bucketName, String objectName, InputStream inputStream, long contentLength, String contentType) {
        try {
            checkBucketExists(bucketName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, contentLength, -1)
                            .contentType(contentType)
                            .build()
            );

            return getFileUrl(bucketName, objectName);
        } catch (Exception e) {
            log.error("Failed to upload file: {}", objectName, e);
            throw new RuntimeException("Failed to upload file: " + objectName, e);
        }
    }

    @Override
    public String uploadFile(String bucketName, String objectName, MultipartFile file) {
        try {
            checkBucketExists(bucketName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return getFileUrl(bucketName, objectName);
        } catch (Exception e) {
            log.error("Failed to upload file: {}", objectName, e);
            throw new RuntimeException("Failed to upload file: " + objectName, e);
        }
    }

    @Override
    public boolean deleteFile(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file: {}", objectName, e);
            return false;
        }
    }

    @Override
    public String getFileUrl(String bucketName, String objectName) {
        try {
            String normalizedObjectName = normalizeObjectName(objectName);
            String publicUrlPrefix = minioConfig.getNormalizedPublicUrlPrefix();
            if (StringUtils.hasText(publicUrlPrefix)) {
                return String.format("%s/%s", publicUrlPrefix, normalizedObjectName);
            }

            // 构建直接的文件访问URL，不使用预签名URL
            return String.format("%s/%s/%s", requireNormalizedEndpoint(), bucketName, normalizedObjectName);
        } catch (Exception e) {
            log.error("Failed to get file URL: {}", objectName, e);
            throw new RuntimeException("Failed to get file URL: " + objectName, e);
        }
    }

    @Override
    public List<Bucket> listBuckets() {
        try {
            return minioClient.listBuckets();
        } catch (Exception e) {
            log.error("Failed to list buckets", e);
            throw new RuntimeException("Failed to list buckets", e);
        }
    }

    @Override
    public boolean fileExists(String bucketName, String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 功能描述：获取必填的标准化 MinIO 端点地址，缺失时直接抛出非法状态异常。
     * <p>
     * 参数说明：无。
     * 返回值：{@code String}，标准化后的 MinIO 端点地址。
     * 异常说明：当 minio.endpoint 未配置时抛出 {@link IllegalStateException}。
     */
    private String requireNormalizedEndpoint() {
        String endpoint = minioConfig.getNormalizedEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalStateException("minio.endpoint 未配置");
        }
        return endpoint;
    }

    /**
     * 功能描述：将对象名标准化为不带前导斜杠的路径，避免 URL 拼接出现重复分隔符。
     * <p>
     * 参数说明：
     *
     * @param objectName String MinIO 对象路径。
     *                   返回值：{@code String}，移除前导斜杠后的对象路径。
     *                   异常说明：当对象路径为空白时抛出 {@link IllegalArgumentException}。
     */
    private String normalizeObjectName(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            throw new IllegalArgumentException("objectName 不能为空");
        }
        return objectName.startsWith("/") ? objectName.substring(1) : objectName;
    }
}
