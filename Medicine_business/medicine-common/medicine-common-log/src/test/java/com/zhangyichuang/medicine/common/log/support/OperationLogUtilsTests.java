package com.zhangyichuang.medicine.common.log.support;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BeanPropertyBindingResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationLogUtilsTests {

    /**
     * 验证 toJson 在序列化时会对密码、令牌等敏感字段做脱敏处理，
     * 防止敏感信息以明文形式进入操作日志。
     */
    @Test
    void toJson_ShouldMaskSensitiveFields() {
        Map<String, Object> payload = Map.of(
                "username", "admin",
                "password", "123456",
                "phoneNumber", "13800000000",
                "email", "user@example.com",
                "idCard", "110101199001011234",
                "nested", Map.of("accessToken", "abc", "receiverPhone", "13900000000", "normal", "ok")
        );

        String json = OperationLogUtils.toJson(payload);

        assertTrue(json.contains("\"password\":\"***\""));
        assertTrue(json.contains("\"accessToken\":\"***\""));
        assertTrue(json.contains("\"phoneNumber\":\"***\""));
        assertTrue(json.contains("\"email\":\"***\""));
        assertTrue(json.contains("\"idCard\":\"***\""));
        assertTrue(json.contains("\"receiverPhone\":\"***\""));
        assertFalse(json.contains("123456"));
        assertFalse(json.contains("\"abc\""));
        assertFalse(json.contains("13800000000"));
        assertFalse(json.contains("user@example.com"));
    }

    /**
     * 验证 JSON 序列化异常时不会回退到对象 toString()，避免敏感明文泄露。
     */
    @Test
    void toJson_WhenSerializationFails_ShouldReturnMaskedPlaceholder() {
        String json = OperationLogUtils.toJson(new NaNHolder());
        assertTrue(json.contains("MASKED_SERIALIZATION_ERROR"));
    }

    /**
     * 验证 Servlet 请求、文件上传、绑定结果、流对象等类型会被识别为过滤对象，
     * 避免在日志中记录不可序列化或无意义的大对象。
     */
    @Test
    void isFilterObject_ShouldReturnTrueForFilteredTypes() {
        assertTrue(OperationLogUtils.isFilterObject(new MockHttpServletRequest()));
        assertTrue(OperationLogUtils.isFilterObject(new MockMultipartFile("file", new byte[]{1, 2, 3})));
        assertTrue(OperationLogUtils.isFilterObject(new BeanPropertyBindingResult(new Object(), "target")));
        assertTrue(OperationLogUtils.isFilterObject(new ByteArrayInputStream(new byte[]{1})));
        assertTrue(OperationLogUtils.isFilterObject(new ByteArrayOutputStream()));
    }

    /**
     * 验证普通业务对象不会被误判为过滤对象，
     * 确保正常请求参数可以被日志模块采集。
     */
    @Test
    void isFilterObject_ShouldReturnFalseForNormalObject() {
        assertFalse(OperationLogUtils.isFilterObject(Map.of("name", "alice")));
    }

    private static class NaNHolder {
        private final Double amount = Double.NaN;
    }
}
