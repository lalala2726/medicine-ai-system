package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 操作日志列表。
 */
@Data
@Schema(description = "操作日志列表")
public class OperationLogListVo {

    @Schema(description = "日志ID", example = "1")
    private long id = 0;

    @Schema(description = "业务模块", example = "用户管理")
    private String module;

    @Schema(description = "操作说明", example = "新增用户")
    private String action;

    @Schema(description = "操作人账号", example = "admin")
    private String username;

    @Schema(description = "请求IP", example = "localhost")
    private String ip;

    @Schema(description = "是否成功：1成功 0失败", example = "1")
    private Integer success;

    @Schema(description = "耗时(ms)", example = "120")
    private Long costTime;

    @Schema(description = "创建时间", example = "2026-02-12 10:00:00")
    private Date createTime;
}
