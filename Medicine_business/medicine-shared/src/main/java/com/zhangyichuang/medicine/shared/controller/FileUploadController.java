package com.zhangyichuang.medicine.shared.controller;

import com.zhangyichuang.medicine.admin.common.storage.service.FileUploadService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.vo.FileUploadVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口（共享）。
 *
 * @author Chuang
 * created on 2025/9/25
 */
@RestController
@RequestMapping("/file")
@Tag(name = "文件上传接口", description = "文件上传接口")
@RequiredArgsConstructor
@PreventDuplicateSubmit
public class FileUploadController extends BaseController {

    /**
     * 文件上传服务。
     */
    private final FileUploadService fileUploadService;

    /**
     * 文件上传接口。
     *
     * @param file 上传文件
     * @return 文件上传结果
     */
    @PostMapping("/upload")
    @Operation(summary = "文件上传")
    public AjaxResult<FileUploadVo> upload(MultipartFile file) {
        FileUploadVo upload = fileUploadService.upload(file);
        return success(upload);
    }
}
