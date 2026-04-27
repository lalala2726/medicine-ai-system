package com.zhangyichuang.medicine.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户列表查询参数")
@Data
public class UserListQueryRequest extends PageRequest {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1")
    private Long id;

    /**
     * 用户名
     */
    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    /**
     * 昵称
     */
    @Schema(description = "昵称", example = "张三")
    private String nickname;

    /**
     * 头像
     */
    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    /**
     * 角色ID
     */
    @Schema(description = "角色ID", example = "1")
    private Long roleId;

    /**
     * 身份证号
     */
    @Schema(description = "身份证号", example = "110101199001011234")
    private String idCard;

    /**
     * 手机号
     */
    @Schema(description = "手机号", example = "13800000000")
    private String phoneNumber;

    /**
     * 真实姓名
     */
    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    /**
     * 状态
     */
    @Schema(description = "状态", example = "1")
    private Integer status;


    /**
     * 创建人
     */
    @Schema(description = "创建人", example = "admin")
    private String createBy;

}
