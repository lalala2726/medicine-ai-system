package com.zhangyichuang.medicine.shared.utils;

import com.zhangyichuang.medicine.common.core.annotation.DataMasking;
import com.zhangyichuang.medicine.common.core.enums.MaskingType;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.DataMaskingUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 激活码工具类。
 */
public final class CouponActivationCodeUtils {

    /**
     * 激活码字符集。
     */
    private static final char[] CODE_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    /**
     * 激活码长度。
     */
    private static final int CODE_LENGTH = 20;

    /**
     * 激活码脱敏规则注解。
     */
    private static final DataMasking ACTIVATION_CODE_MASKING = new DataMasking() {

        /**
         * 获取脱敏类型。
         *
         * @return 脱敏类型
         */
        @Override
        public MaskingType type() {
            return MaskingType.CUSTOM;
        }

        /**
         * 获取自定义正则表达式。
         *
         * @return 自定义正则表达式
         */
        @Override
        public String regex() {
            return "";
        }

        /**
         * 获取自定义替换表达式。
         *
         * @return 自定义替换表达式
         */
        @Override
        public String replacement() {
            return "";
        }

        /**
         * 获取脱敏字符。
         *
         * @return 脱敏字符
         */
        @Override
        public String maskChar() {
            return "*";
        }

        /**
         * 获取前缀保留字符数。
         *
         * @return 前缀保留字符数
         */
        @Override
        public int prefixKeep() {
            return 4;
        }

        /**
         * 获取后缀保留字符数。
         *
         * @return 后缀保留字符数
         */
        @Override
        public int suffixKeep() {
            return 4;
        }

        /**
         * 获取是否保留原始长度。
         *
         * @return 是否保留原始长度
         */
        @Override
        public boolean preserveLength() {
            return false;
        }

        /**
         * 获取脱敏字符长度。
         *
         * @return 脱敏字符长度
         */
        @Override
        public int maskLength() {
            return 4;
        }

        /**
         * 获取注解类型。
         *
         * @return 注解类型
         */
        @Override
        public Class<? extends Annotation> annotationType() {
            return DataMasking.class;
        }
    };

    /**
     * 批次号前缀。
     */
    private static final String BATCH_NO_PREFIX = "ACT";

    /**
     * 批次号时间格式。
     */
    private static final DateTimeFormatter BATCH_NO_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 安全随机数生成器。
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 私有构造函数。
     */
    private CouponActivationCodeUtils() {
    }

    /**
     * 生成激活码明文。
     *
     * @return 激活码明文
     */
    public static String generateCode() {
        StringBuilder codeBuilder = new StringBuilder(CODE_LENGTH);
        for (int index = 0; index < CODE_LENGTH; index++) {
            int randomIndex = SECURE_RANDOM.nextInt(CODE_CHARACTERS.length);
            codeBuilder.append(CODE_CHARACTERS[randomIndex]);
        }
        return codeBuilder.toString();
    }

    /**
     * 生成激活码批次号。
     *
     * @return 激活码批次号
     */
    public static String generateBatchNo() {
        return BATCH_NO_PREFIX
                + LocalDateTime.now().format(BATCH_NO_TIME_FORMATTER)
                + randomDigits(4);
    }

    /**
     * 规范化激活码文本。
     *
     * @param rawCode 原始激活码文本
     * @return 规范化后的激活码
     */
    public static String normalizeCode(String rawCode) {
        if (!StringUtils.isNotBlank(rawCode)) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "激活码不能为空");
        }
        return rawCode.replaceAll("\\s+", "").trim().toUpperCase();
    }

    /**
     * 计算激活码哈希值。
     *
     * @param normalizedCode 规范化后的激活码
     * @return 激活码哈希值
     */
    public static String hashCode(String normalizedCode) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = messageDigest.digest(normalizedCode.getBytes(StandardCharsets.UTF_8));
            StringBuilder hashBuilder = new StringBuilder(bytes.length * 2);
            for (byte currentByte : bytes) {
                hashBuilder.append(String.format("%02x", currentByte));
            }
            return hashBuilder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new ServiceException(ResponseCode.SYSTEM_ERROR, "激活码哈希算法不可用");
        }
    }

    /**
     * 构建激活码脱敏展示值。
     *
     * @param normalizedCode 规范化后的激活码
     * @return 脱敏后的激活码
     */
    public static String maskCode(String normalizedCode) {
        return DataMaskingUtils.mask(normalizedCode, ACTIVATION_CODE_MASKING);
    }

    /**
     * 生成指定长度的随机数字串。
     *
     * @param length 随机数字长度
     * @return 随机数字串
     */
    private static String randomDigits(int length) {
        StringBuilder digitsBuilder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            digitsBuilder.append(SECURE_RANDOM.nextInt(10));
        }
        return digitsBuilder.toString();
    }
}
