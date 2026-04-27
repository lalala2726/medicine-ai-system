package com.zhangyichuang.medicine.common.core.utils;

import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 图片相关工具类。
 */
public final class ImageUtils {

    private static final int ONE_MB = 1024 * 1024;
    private static final String JPEG_MIME_TYPE = "image/jpeg";
    private static final double TEXT_MIN_SCALE = 0.7d;
    private static final double DEFAULT_MIN_SCALE = 0.5d;
    private static final double START_SCALE = 0.95d;
    private static final double SCALE_STEP = 0.05d;

    private ImageUtils() {
    }

    /**
     * 确保图片不超过 1MB，超出则有损压缩为 JPEG。
     *
     * @param data             原始图片二进制
     * @param originalMimeType 原始 mime 类型，用于在无需压缩时保持返回值一致
     * @return 压缩后的图片数据和对应 mimeType
     */
    public static EncodedImage ensureUnder(byte[] data, String originalMimeType) {
        return compressIfExceeding(data, originalMimeType, ONE_MB, 0.9f, false);
    }

    /**
     * 在压缩时优先保证文字清晰度，适合票据、截图等以文字为主的场景。
     *
     * @param data             原始图片二进制
     * @param originalMimeType 原始 mime 类型
     * @return 压缩后的图片数据和对应 mimeType
     */
    public static EncodedImage ensureUnderForText(byte[] data, String originalMimeType) {
        return compressIfExceeding(data, originalMimeType, ONE_MB, 0.92f, true);
    }

    /**
     * 按需压缩图片：当超过指定字节数时按给定质量压缩，质量为 0-1 的浮点数。
     * maxBytes 传 0 或负数则不做大小限制；prioritizeTextClarity 为 true 时缩放下限更高以保文字清晰。
     */
    public static EncodedImage compressIfExceeding(byte[] data,
                                                   String originalMimeType,
                                                   long maxBytes,
                                                   float targetQuality,
                                                   boolean prioritizeTextClarity) {
        if (data == null || data.length == 0) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "图片内容不能为空");
        }

        if (targetQuality <= 0.0f || targetQuality > 1.0f) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "压缩质量需在 (0,1] 区间内");
        }

        if (maxBytes <= 0 || data.length <= maxBytes) {
            return new EncodedImage(data, originalMimeType);
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
                throw new ServiceException(ResponseCode.PARAM_ERROR, "图片格式无法解析");
            }

            double minScale = prioritizeTextClarity ? TEXT_MIN_SCALE : DEFAULT_MIN_SCALE;

            byte[] compressed = compressWithThumbnailator(image, targetQuality, 1.0d);
            if (compressed.length <= maxBytes) {
                return new EncodedImage(compressed, JPEG_MIME_TYPE);
            }

            for (double scale = START_SCALE; scale >= minScale; scale -= SCALE_STEP) {
                compressed = compressWithThumbnailator(image, targetQuality, scale);
                if (compressed.length <= maxBytes) {
                    return new EncodedImage(compressed, JPEG_MIME_TYPE);
                }
            }

            throw new ServiceException(ResponseCode.OPERATION_ERROR, "图片压缩后仍超过限制，请使用更小的图片");
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "图片压缩失败，请稍后重试");
        }
    }

    private static byte[] compressWithThumbnailator(BufferedImage image, float quality, double scale) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Thumbnails.of(image)
                    .scale(scale)
                    .outputFormat("jpeg")
                    .outputQuality(quality)
                    .toOutputStream(baos);
            return baos.toByteArray();
        }
    }

    public record EncodedImage(byte[] data, String mimeType) {
    }
}
