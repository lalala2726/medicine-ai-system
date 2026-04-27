package com.zhangyichuang.medicine.admin.common.storage.service;

import io.minio.messages.Bucket;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * MinIO存储服务接口
 *
 * @author Chuang
 * created on 2025/9/25
 */
public interface MinioStorageService {

    /**
     * 检查存储桶是否存在，不存在则创建
     *
     * @param bucketName 存储桶名称
     */
    void checkBucketExists(String bucketName);

    /**
     * 上传文件
     *
     * @param bucketName    存储桶名称
     * @param objectName    对象名称（包含路径）
     * @param inputStream   文件输入流
     * @param contentLength 文件大小（字节）
     * @param contentType   文件类型
     * @return 文件访问地址
     */
    String uploadFile(String bucketName, String objectName, InputStream inputStream, long contentLength, String contentType);

    /**
     * 上传文件（MultipartFile）
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（包含路径）
     * @param file       文件
     * @return 文件访问地址
     */
    String uploadFile(String bucketName, String objectName, MultipartFile file);

    /**
     * 删除文件
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 是否删除成功
     */
    boolean deleteFile(String bucketName, String objectName);

    /**
     * 获取文件访问地址
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 文件访问地址；配置了公网前缀时返回 CDN 地址，否则返回 MinIO 原始地址
     */
    String getFileUrl(String bucketName, String objectName);

    /**
     * 获取所有存储桶
     *
     * @return 存储桶列表
     */
    List<Bucket> listBuckets();

    /**
     * 检查文件是否存在
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 是否存在
     */
    boolean fileExists(String bucketName, String objectName);

}
