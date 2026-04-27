package com.zhangyichuang.medicine.common.redis.support;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BeanPropertyBindingResult;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 防重复提交参数指纹构建器测试。
 */
class PreventDuplicateSubmitFingerprintBuilderTests {

    /**
     * 待测试的参数指纹构建器。
     */
    private final PreventDuplicateSubmitFingerprintBuilder fingerprintBuilder =
            new PreventDuplicateSubmitFingerprintBuilder();

    /**
     * 验证相同业务参数在 Map 键顺序不同时仍生成相同指纹。
     */
    @Test
    void buildFingerprint_WhenMapKeyOrderDiffers_ShouldReturnSameFingerprint() {
        Map<String, Object> leftPayload = new LinkedHashMap<>();
        leftPayload.put("name", "coupon");
        leftPayload.put("amount", 100);

        Map<String, Object> rightPayload = new LinkedHashMap<>();
        rightPayload.put("amount", 100);
        rightPayload.put("name", "coupon");

        String leftFingerprint = fingerprintBuilder.buildFingerprint(
                new String[]{"request"},
                new Object[]{leftPayload}
        );
        String rightFingerprint = fingerprintBuilder.buildFingerprint(
                new String[]{"request"},
                new Object[]{rightPayload}
        );

        assertEquals(leftFingerprint, rightFingerprint);
    }

    /**
     * 验证显式传入 null 参数会参与指纹计算，
     * 避免与“完全未传该参数”的场景混淆。
     */
    @Test
    void buildFingerprint_WhenNullArgumentExists_ShouldIncludeNullIntoFingerprint() {
        String fingerprintWithNull = fingerprintBuilder.buildFingerprint(
                new String[]{"request", "remark"},
                new Object[]{Map.of("id", 1L), null}
        );
        String fingerprintWithoutSecondArgument = fingerprintBuilder.buildFingerprint(
                new String[]{"request"},
                new Object[]{Map.of("id", 1L)}
        );

        assertNotEquals(fingerprintWithNull, fingerprintWithoutSecondArgument);
    }

    /**
     * 验证 ServletRequest 与 BindingResult 不参与指纹计算，
     * 避免无关上下文对象影响重复提交判定。
     */
    @Test
    void buildFingerprint_WhenRequestAndBindingResultProvided_ShouldIgnoreFilteredObjects() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mall/coupon/template");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");

        String filteredFingerprint = fingerprintBuilder.buildFingerprint(
                new String[]{"payload", "request", "bindingResult"},
                new Object[]{Map.of("id", 2L), request, bindingResult}
        );
        String simpleFingerprint = fingerprintBuilder.buildFingerprint(
                new String[]{"payload"},
                new Object[]{Map.of("id", 2L)}
        );

        assertEquals(simpleFingerprint, filteredFingerprint);
    }

    /**
     * 验证文件参数的关键元数据会参与指纹计算，
     * 防止不同文件内容元信息被错误视为同一次提交。
     */
    @Test
    void buildFingerprint_WhenMultipartMetadataDiffers_ShouldReturnDifferentFingerprint() {
        MockMultipartFile firstFile = new MockMultipartFile(
                "file",
                "coupon-a.xlsx",
                "application/vnd.ms-excel",
                "A".getBytes()
        );
        MockMultipartFile secondFile = new MockMultipartFile(
                "file",
                "coupon-b.xlsx",
                "application/vnd.ms-excel",
                "A".getBytes()
        );

        String firstFingerprint = fingerprintBuilder.buildFingerprint(
                new String[]{"file"},
                new Object[]{firstFile}
        );
        String secondFingerprint = fingerprintBuilder.buildFingerprint(
                new String[]{"file"},
                new Object[]{secondFile}
        );

        assertNotEquals(firstFingerprint, secondFingerprint);
    }
}
