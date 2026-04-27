package com.zhangyichuang.medicine.common.core.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用API响应结果封装类
 *
 * @author Chuang
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Tag(name = "通用API响应结果封装类", description = "通用API响应结果封装类")
public class AjaxResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    @Schema(description = "状态码", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer code;

    /**
     * 返回消息
     */
    @Schema(description = "返回消息", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    /**
     * 时间戳 (毫秒)
     */
    @Schema(description = "时间戳 (毫秒)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long timestamp;

    /**
     * 数据
     */
    @Schema(description = "数据")
    private T data;

    // --- 私有构造函数 ---

    /**
     * 基础构造函数，初始化核心字段
     *
     * @param code    状态码
     * @param message 消息
     * @param data    数据
     */
    private AjaxResult(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 通过 ResponseCode 枚举和数据构造
     *
     * @param responseCode ResponseCode 枚举实例
     * @param data         数据
     */
    private AjaxResult(ResponseCode responseCode, T data) {
        this(responseCode.getCode(), responseCode.getMessage(), data);
    }

    /**
     * 通过 ResponseCode 枚举构造 (无数据)
     *
     * @param responseCode ResponseCode 枚举实例
     */
    private AjaxResult(ResponseCode responseCode) {
        this(responseCode, null);
    }


    /**
     * 成功返回 (无消息，无数据)
     */
    @Schema(description = "成功返回 (无消息，无数据)")
    public static <T> AjaxResult<T> success() {
        return new AjaxResult<>(ResponseCode.SUCCESS);
    }

    /**
     * 成功返回 (带自定义消息，无数据)
     *
     * @param message 自定义成功消息
     */
    public static <T> AjaxResult<T> success(String message) {
        return new AjaxResult<>(ResponseCode.SUCCESS.getCode(), message, null);
    }

    /**
     * 成功返回 (使用默认消息，带数据)
     *
     * @param data 返回的数据
     */
    public static <T> AjaxResult<T> success(T data) {
        return new AjaxResult<>(ResponseCode.SUCCESS, data);
    }

    /**
     * 成功返回 (带自定义消息和数据)
     *
     * @param message 自定义成功消息
     * @param data    返回的数据
     */
    public static <T> AjaxResult<T> success(String message, T data) {
        return new AjaxResult<>(ResponseCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败返回 (使用默认错误码和消息，无数据)
     */
    public static <T> AjaxResult<T> error() {
        return new AjaxResult<>(ResponseCode.ERROR);
    }

    public static <T> AjaxResult<T> error(String message, Integer code) {
        return new AjaxResult<>(code, message, null);
    }


    /**
     * 失败返回 (使用默认错误码，带自定义消息，无数据)
     *
     * @param message 自定义错误消息
     */
    public static <T> AjaxResult<T> error(String message) {
        return new AjaxResult<>(ResponseCode.ERROR.getCode(), message, null);
    }

    /**
     * 失败返回 (使用指定的 ResponseCode，无数据)
     *
     * @param responseCode 响应码枚举
     */
    public static <T> AjaxResult<T> error(ResponseCode responseCode) {
        return new AjaxResult<>(responseCode);
    }

    /**
     * 失败返回 (使用指定的 ResponseCode 和自定义消息，无数据)
     *
     * @param responseCode 响应码枚举
     * @param message      自定义错误消息 (将覆盖枚举中的默认消息)
     */
    public static <T> AjaxResult<T> error(ResponseCode responseCode, String message) {
        return new AjaxResult<>(responseCode.getCode(), message, null);
    }

    /**
     * 失败返回 (使用指定的自定义错误码和自定义消息，无数据)
     *
     * @param code    自定义错误码
     * @param message 自定义错误消息
     */
    public static <T> AjaxResult<T> error(Integer code, String message) {
        return new AjaxResult<>(code, message, null);
    }

    /**
     * 警告返回 (使用默认警告码，带自定义消息，无数据)
     *
     * @param message 自定义警告消息
     */
    public static <T> AjaxResult<T> warning(String message) {
        return new AjaxResult<>(ResponseCode.WARNING.getCode(), message, null);
    }

}
