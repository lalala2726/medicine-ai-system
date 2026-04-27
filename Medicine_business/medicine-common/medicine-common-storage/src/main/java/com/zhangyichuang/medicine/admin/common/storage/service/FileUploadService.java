package com.zhangyichuang.medicine.admin.common.storage.service;

import com.zhangyichuang.medicine.model.vo.FileUploadVo;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Chuang
 * <p>
 * created on 2025/9/25
 */
public interface FileUploadService {

    FileUploadVo upload(MultipartFile file);

}
