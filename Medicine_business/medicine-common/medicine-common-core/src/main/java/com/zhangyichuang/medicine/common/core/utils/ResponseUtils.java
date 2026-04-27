package com.zhangyichuang.medicine.common.core.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * 响应工具类
 */
public class ResponseUtils {

    private static final Logger log = LoggerFactory.getLogger(ResponseUtils.class);

    /**
     * 异常消息返回(适用过滤器中处理异常响应)
     *
     * @param response   HttpServletResponse
     * @param resultCode 响应结果码
     */
    public static void writeErrMsg(HttpServletResponse response, ResponseCode resultCode, HttpStatus httpStatus) {
        writeErrMsg(response, httpStatus, resultCode, null);
    }

    public static void writeErrMsg(HttpServletResponse response, HttpStatus httpStatus, String message) {
        writeErrMsg(response, httpStatus, null, message);
    }

    /**
     * 响应消息返回(适用过滤器中处理异常响应)
     *
     * @param response   HttpServletResponse
     * @param resultCode 响应结果码
     * @param message    自定义错误消息
     */
    public static void writeErrMsg(HttpServletResponse response, HttpStatus httpStatus, ResponseCode resultCode, String message) {
        response.setStatus(httpStatus.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try (PrintWriter writer = response.getWriter()) {
            AjaxResult<?> ajaxResult;
            if (resultCode != null && message != null) {
                ajaxResult = AjaxResult.error(resultCode, message);
            } else if (resultCode != null) {
                ajaxResult = AjaxResult.error(resultCode);
            } else {
                ajaxResult = AjaxResult.error(message);
            }
            String jsonResponse = toJsonWithoutNullData(ajaxResult);
            writer.print(jsonResponse);
            writer.flush(); // 确保将响应内容写入到输出流
        } catch (IOException e) {
            log.error("响应异常处理失败", e);
        }
    }

    /**
     * 异常消息返回(适用过滤器中处理异常响应)
     *
     * @param response   HttpServletResponse
     * @param resultCode 响应结果码
     * @param message    自定义错误消息
     */
    public static void writeErrMsg(HttpServletResponse response, ResponseCode resultCode, HttpStatus httpStatus, String message) {
        writeErrMsg(response, httpStatus, resultCode, message);
    }

    /**
     * 功能描述：将 AjaxResult 序列化为 JSON，并在 data 字段为 null 时移除该字段，避免响应体出现 "data":null。
     *
     * @param ajaxResult 响应对象，类型为 {@link AjaxResult}，包含 code、message、timestamp、data 等字段
     * @return 返回序列化后的 JSON 字符串；当 data 为空时不包含 data 字段
     * @throws RuntimeException 异常说明：无显式抛出；若序列化失败则由调用方按运行时异常处理
     */
    private static String toJsonWithoutNullData(AjaxResult<?> ajaxResult) {
        JsonElement jsonElement = JSONUtils.toJsonTree(ajaxResult);
        if (jsonElement != null && jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject.has("data") && jsonObject.get("data").isJsonNull()) {
                jsonObject.remove("data");
            }
            return JSONUtils.toJson(jsonObject);
        }
        return JSONUtils.toJson(ajaxResult);
    }

}
