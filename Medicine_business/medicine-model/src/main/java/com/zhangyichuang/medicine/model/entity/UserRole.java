package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户角色中间表
 */
@TableName(value = "user_role")
@Data
public class UserRole {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色ID
     */
    private Long roleId;
}
