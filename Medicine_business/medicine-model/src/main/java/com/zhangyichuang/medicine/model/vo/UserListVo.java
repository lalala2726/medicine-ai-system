package com.zhangyichuang.medicine.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhangyichuang.medicine.common.core.annotation.DataMasking;
import com.zhangyichuang.medicine.common.core.enums.MaskingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 用户
 */
@Schema(description = "用户列表视图对象")
@Data
public class UserListVo {

    @Schema(description = "用户ID", format = "int64", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "手机号", example = "13800000000")
    @DataMasking(type = MaskingType.MOBILE_PHONE)
    private String phoneNumber;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @DataMasking(type = MaskingType.EMAIL)
    private String email;

    @Schema(description = "角色", example = "admin")
    private String roles;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "2023-01-01 00:00:00")
    private Date createTime;

}
