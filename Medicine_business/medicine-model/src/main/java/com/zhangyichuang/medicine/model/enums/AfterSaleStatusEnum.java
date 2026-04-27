package com.zhangyichuang.medicine.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 售后状态枚举
 *
 * @author Chuang
 * created 2025/11/08
 */
@Getter
public enum AfterSaleStatusEnum {

    /**
     * 待审核
     */
    PENDING("PENDING", "待审核", "用户已提交售后申请，等待管理员审核"),

    /**
     * 已通过
     */
    APPROVED("APPROVED", "已通过", "管理员审核通过，等待处理退款"),

    /**
     * 已拒绝
     */
    REJECTED("REJECTED", "已拒绝", "管理员审核拒绝"),

    /**
     * 处理中
     */
    PROCESSING("PROCESSING", "处理中", "正在处理退款或换货"),

    /**
     * 已完成
     */
    COMPLETED("COMPLETED", "已完成", "售后处理完成"),

    /**
     * 已取消
     */
    CANCELLED("CANCELLED", "已取消", "用户取消售后申请");

    /**
     * 枚举值(存储到数据库的值)
     */
    @EnumValue
    @JsonValue
    private final String status;

    /**
     * 中文名称
     */
    private final String name;

    /**
     * 描述信息
     */
    private final String description;

    AfterSaleStatusEnum(String status, String name, String description) {
        this.status = status;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据 status 获取枚举
     *
     * @param status 售后状态
     * @return 枚举对象
     */
    public static AfterSaleStatusEnum fromCode(String status) {
        if (status == null) {
            return null;
        }
        for (AfterSaleStatusEnum afterSaleStatus : values()) {
            if (afterSaleStatus.status.equals(status)) {
                return afterSaleStatus;
            }
        }
        return null;
    }
}

