package com.zhangyichuang.medicine.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件上传返回参数。
 *
 * @author Chuang
 * created on 2025/9/25
 */
@Data
@Schema(description = "文件上传返回参数")
public class FileUploadVo {

    /**
     * 文件名。
     */
    @Schema(description = "文件名")
    private String fileName;

    /**
     * 文件大小。
     */
    @Schema(description = "文件大小")
    private Long fileSize;

    /**
     * 文件类型。
     */
    @Schema(description = "文件类型")
    private String fileType;

    /**
     * 文件公网访问地址。
     */
    @Schema(description = "文件公网访问地址", example = "https://medicine-cdn.zhangchuangla.cn/2026/03/example.pdf")
    private String fileUrl;
}
