package com.zhangyichuang.medicine.model.vo;

import com.zhangyichuang.medicine.common.core.annotation.DataMasking;
import com.zhangyichuang.medicine.common.core.enums.MaskingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户
 */
@Schema(description = "用户信息视图对象")
@Data
public class UserVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "昵称", example = "张三")
    private String nickName;

    @Schema(description = "头像", example = "https://zhangchuangla.cn/avatar.png")
    private String avatar;

    @Schema(description = "手机号", example = "13800000000")
    @DataMasking(type = MaskingType.MOBILE_PHONE)
    private String phoneNumber;

    @Schema(description = "邮箱", example = "admin@example.com")
    @DataMasking(type = MaskingType.EMAIL)
    private String email;

    @Schema(description = "性别", example = "男")
    private String gender;

    @Schema(description = "状态", example = "正常")
    private String status;
}
