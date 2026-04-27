package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 激活码明细视图对象。
 */
@Data
@Builder
@Schema(description = "激活码明细")
public class ActivationCodeGeneratedItemVo {

    /**
     * 激活码ID。
     */
    @Schema(description = "激活码ID", example = "10001")
    private Long id;

    /**
     * 激活码明文。
     */
    @Schema(description = "激活码明文", example = "ABCD1234EFGH5678JKLM")
    private String plainCode;

    /**
     * 激活码状态。
     */
    @Schema(description = "激活码状态", example = "ACTIVE")
    private String status;

    /**
     * 成功使用次数。
     */
    @Schema(description = "成功使用次数", example = "0")
    private Integer successUseCount;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 最近一次成功激活时间。
     */
    @Schema(description = "最近一次成功激活时间")
    private Date lastSuccessTime;

    /**
     * 最近一次成功激活客户端IP。
     */
    @Schema(description = "最近一次成功激活客户端IP", example = "192.168.1.8")
    private String lastSuccessClientIp;

    /**
     * 最近一次成功激活用户ID。
     */
    @Schema(description = "最近一次成功激活用户ID", example = "1001")
    private Long lastSuccessUserId;

    /**
     * 最近一次成功激活用户名。
     */
    @Schema(description = "最近一次成功激活用户名", example = "zhangsan")
    private String lastSuccessUserName;
}
