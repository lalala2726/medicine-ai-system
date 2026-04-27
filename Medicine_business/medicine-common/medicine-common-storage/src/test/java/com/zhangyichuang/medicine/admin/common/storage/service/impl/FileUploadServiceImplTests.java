package com.zhangyichuang.medicine.admin.common.storage.service.impl;

import com.zhangyichuang.medicine.admin.common.storage.config.FileUploadProperties;
import com.zhangyichuang.medicine.admin.common.storage.config.MinioConfig;
import com.zhangyichuang.medicine.admin.common.storage.service.MinioStorageService;
import com.zhangyichuang.medicine.model.vo.FileUploadVo;
import io.minio.messages.Bucket;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件上传服务对象路径测试。
 */
class FileUploadServiceImplTests {

    /**
     * 默认文件桶名称。
     */
    private static final String BUCKET_NAME = "medicine";

    /**
     * 默认 CDN 地址。
     */
    private static final String CDN_FILE_URL = "http://localhost:9000/medicine/resources/2026/03/test-file.pdf";

    @Test
    void uploadBuildsObjectNameWithNormalizedUploadPath() {
        RecordingMinioStorageService minioStorageService = new RecordingMinioStorageService();
        minioStorageService.setUploadResult(CDN_FILE_URL);
        FileUploadProperties fileUploadProperties = new FileUploadProperties();
        MinioConfig minioConfig = buildMinioConfig("/resources/");
        FileUploadServiceImpl fileUploadService = new FileUploadServiceImpl(minioStorageService, fileUploadProperties, minioConfig);
        MockMultipartFile multipartFile = buildPdfFile();

        FileUploadVo uploadVo = fileUploadService.upload(multipartFile);

        String actualObjectName = minioStorageService.getLastObjectName();
        assertObjectNameMatches(actualObjectName);
        assertEquals(BUCKET_NAME, minioStorageService.getLastBucketName());
        assertEquals(CDN_FILE_URL, uploadVo.getFileUrl());
    }

    @Test
    void uploadDefaultsUploadPathToResourcesWhenBlank() {
        RecordingMinioStorageService minioStorageService = new RecordingMinioStorageService();
        minioStorageService.setUploadResult(CDN_FILE_URL);
        FileUploadProperties fileUploadProperties = new FileUploadProperties();
        MinioConfig minioConfig = buildMinioConfig("   ");
        FileUploadServiceImpl fileUploadService = new FileUploadServiceImpl(minioStorageService, fileUploadProperties, minioConfig);
        MockMultipartFile multipartFile = buildPdfFile();

        fileUploadService.upload(multipartFile);

        String actualObjectName = minioStorageService.getLastObjectName();
        assertObjectNameMatches(actualObjectName);
        assertEquals(BUCKET_NAME, minioStorageService.getLastBucketName());
        assertTrue(actualObjectName.startsWith("resources/"));
    }

    /**
     * 功能描述：构建最小 MinIO 配置对象，供上传路径规则测试复用。
     * <p>
     * 参数说明：
     *
     * @param uploadPath String 待测试的上传路径前缀。
     *                   返回值：{@link MinioConfig}，已填充桶名与上传路径的配置对象。
     *                   异常说明：无。
     */
    private MinioConfig buildMinioConfig(String uploadPath) {
        MinioConfig minioConfig = new MinioConfig();
        minioConfig.setBucketName(BUCKET_NAME);
        minioConfig.setUploadPath(uploadPath);
        return minioConfig;
    }

    /**
     * 功能描述：构建测试用 PDF 文件，确保通过默认白名单校验。
     * <p>
     * 参数说明：无。
     * 返回值：{@link MockMultipartFile}，测试用上传文件对象。
     * 异常说明：无。
     */
    private MockMultipartFile buildPdfFile() {
        return new MockMultipartFile("file", "manual.pdf", "application/pdf", "test".getBytes());
    }

    /**
     * 功能描述：断言上传对象路径符合 resources/yyyy/MM/uuid.ext 的结构约束。
     * <p>
     * 参数说明：
     *
     * @param actualObjectName String 实际生成的对象路径。
     *                         返回值：无。
     *                         异常说明：断言失败时由测试框架抛出异常。
     */
    private void assertObjectNameMatches(String actualObjectName) {
        String expectedPrefix = "resources/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM")) + "/";
        assertTrue(actualObjectName.startsWith(expectedPrefix));
        assertTrue(actualObjectName.endsWith(".pdf"));
    }

    /**
     * 记录上传参数的 MinIO 测试替身。
     */
    private static final class RecordingMinioStorageService implements MinioStorageService {

        /**
         * 上传返回地址。
         */
        private String uploadResult;

        /**
         * 最近一次上传的桶名称。
         */
        private String lastBucketName;

        /**
         * 最近一次上传的对象路径。
         */
        private String lastObjectName;

        /**
         * 设置上传返回地址。
         *
         * @param uploadResult 上传返回地址
         */
        private void setUploadResult(String uploadResult) {
            this.uploadResult = uploadResult;
        }

        /**
         * 获取最近一次上传的桶名称。
         *
         * @return 桶名称
         */
        private String getLastBucketName() {
            return lastBucketName;
        }

        /**
         * 获取最近一次上传的对象路径。
         *
         * @return 对象路径
         */
        private String getLastObjectName() {
            return lastObjectName;
        }

        @Override
        public void checkBucketExists(String bucketName) {
        }

        @Override
        public String uploadFile(String bucketName, String objectName, InputStream inputStream, long contentLength, String contentType) {
            throw new UnsupportedOperationException("当前测试不使用输入流上传分支");
        }

        @Override
        public String uploadFile(String bucketName, String objectName, MultipartFile file) {
            this.lastBucketName = bucketName;
            this.lastObjectName = objectName;
            return uploadResult;
        }

        @Override
        public boolean deleteFile(String bucketName, String objectName) {
            return false;
        }

        @Override
        public String getFileUrl(String bucketName, String objectName) {
            return null;
        }

        @Override
        public List<Bucket> listBuckets() {
            return List.of();
        }

        @Override
        public boolean fileExists(String bucketName, String objectName) {
            return false;
        }
    }
}
