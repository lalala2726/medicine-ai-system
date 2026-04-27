package com.zhangyichuang.medicine.admin.common.storage.service.impl;

import com.zhangyichuang.medicine.admin.common.storage.config.FileUploadProperties;
import com.zhangyichuang.medicine.admin.common.storage.config.MinioConfig;
import com.zhangyichuang.medicine.admin.common.storage.service.FileUploadService;
import com.zhangyichuang.medicine.admin.common.storage.service.MinioStorageService;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.model.vo.FileUploadVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传服务实现类
 *
 * @author Chuang
 * created on 2025/9/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final MinioStorageService minioStorageService;
    private final FileUploadProperties fileUploadProperties;
    private final MinioConfig minioConfig;

    @Override
    public FileUploadVo upload(MultipartFile file) {
        try {
            // 参数校验
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("文件不能为空");
            }

            // 获取文件原始名称
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new IllegalArgumentException("文件名不能为空");
            }

            String contentType = file.getContentType();
            if (!isAllowedFileType(originalFilename, contentType)) {
                throw new IllegalArgumentException("不支持的文件类型, 文件名: " + originalFilename + ", Content-Type: " + contentType);
            }

            // 构建完整的对象路径
            String objectName = buildObjectName(originalFilename);

            // 上传文件并返回访问地址
            String bucketName = resolveBucketName();
            String fileUrl = minioStorageService.uploadFile(bucketName, objectName, file);

            // 构建返回结果
            FileUploadVo uploadVo = new FileUploadVo();
            uploadVo.setFileName(originalFilename);
            uploadVo.setFileSize(file.getSize());
            uploadVo.setFileType(file.getContentType());
            uploadVo.setFileUrl(fileUrl);

            log.info("File uploaded successfully: {}", originalFilename);
            return uploadVo;

        } catch (Exception e) {
            log.error("File upload failed", e);
            throw new ServiceException("文件上传失败");
        }
    }

    /**
     * 功能描述：构建完整的 MinIO 对象路径，统一追加上传路径前缀、年月目录和随机文件名。
     * <p>
     * 参数说明：
     *
     * @param originalFilename String 原始文件名。
     *                         返回值：{@code String}，完整对象路径，格式为 uploadPath/yyyy/MM/uuid.ext。
     *                         异常说明：无。
     */
    private String buildObjectName(String originalFilename) {
        String uploadPath = resolveUploadPath();
        String folderPath = generateYearMonthFolderPath();
        String uniqueFileName = generateUniqueFileName(originalFilename);
        return uploadPath + "/" + folderPath + "/" + uniqueFileName;
    }

    /**
     * 生成年月文件夹路径
     * 格式：yyyy/MM
     *
     * @return 文件夹路径
     */
    private String generateYearMonthFolderPath() {
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM");
        return now.format(formatter);
    }

    /**
     * 生成唯一文件名
     *
     * @param originalFilename 原始文件名
     * @return 唯一文件名
     */
    private String generateUniqueFileName(String originalFilename) {
        String fileExtension = "";
        if (originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID() + fileExtension;
    }

    /**
     * 功能描述：校验上传文件类型是否在白名单中，支持 MIME 类型与文件扩展名两种匹配方式。
     * <p>
     * 参数说明：
     *
     * @param originalFilename String 原始文件名，用于提取文件扩展名。
     * @param contentType      String 文件 MIME 类型，可能为空。
     *                         返回值：boolean，返回 true 表示类型合法；返回 false 表示类型不在白名单中。
     *                         异常说明：无。
     */
    private boolean isAllowedFileType(String originalFilename, String contentType) {
        Set<String> allowedTypeSet = fileUploadProperties.getAllowedTypeSet();
        if (allowedTypeSet.isEmpty()) {
            return false;
        }

        if (contentType != null && allowedTypeSet.contains(contentType.toLowerCase(Locale.ROOT))) {
            return true;
        }

        String extension = extractFileExtension(originalFilename);
        return !extension.isEmpty() && allowedTypeSet.contains(extension);
    }

    /**
     * 功能描述：从原始文件名中提取扩展名并标准化为小写（包含 "." 前缀）。
     * <p>
     * 参数说明：
     *
     * @param originalFilename String 原始文件名。
     *                         返回值：String，文件扩展名（如 ".pdf"）；当不存在扩展名时返回空字符串。
     *                         异常说明：无。
     */
    private String extractFileExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }

        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex < 0 || lastDotIndex == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(lastDotIndex).toLowerCase(Locale.ROOT);
    }

    /**
     * 功能描述：获取当前上传使用的桶名称，缺失时直接阻断上传。
     * <p>
     * 参数说明：无。
     * 返回值：{@code String}，配置中的桶名称。
     * 异常说明：当 minio.bucket-name 未配置时抛出 {@link IllegalStateException}。
     */
    private String resolveBucketName() {
        String bucketName = minioConfig.getBucketName();
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalStateException("minio.bucket-name 未配置");
        }
        return bucketName;
    }

    /**
     * 功能描述：获取标准化后的上传路径前缀，保证对象不会直接落在桶根目录。
     * <p>
     * 参数说明：无。
     * 返回值：{@code String}，标准化后的上传路径前缀。
     * 异常说明：无。
     */
    private String resolveUploadPath() {
        return minioConfig.getNormalizedUploadPath();
    }
}
