package com.zhangyichuang.medicine.common.captcha.config;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.ImageCaptchaProperties;
import cloud.tianai.captcha.application.TACBuilder;
import cloud.tianai.captcha.cache.CacheStore;
import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.generator.impl.transform.Base64ImageTransform;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import cloud.tianai.captcha.resource.impl.provider.ClassPathResourceProvider;
import com.zhangyichuang.medicine.common.captcha.cache.TianaiRedisCacheStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 验证码自动配置。
 *
 * @author Chuang
 */
@Configuration
@EnableConfigurationProperties(CaptchaProperties.class)
public class CaptchaAutoConfiguration {

    /**
     * 滑块验证码类型。
     */
    private static final String CAPTCHA_TYPE_SLIDER = CaptchaTypeConstant.SLIDER;

    /**
     * classpath 资源前缀。
     */
    private static final String CLASS_PATH_RESOURCE_PREFIX = "classpath:";

    /**
     * 文件资源前缀。
     */
    private static final String FILE_RESOURCE_PREFIX = "file:";

    /**
     * HTTP 资源前缀。
     */
    private static final String HTTP_RESOURCE_PREFIX = "http://";

    /**
     * HTTPS 资源前缀。
     */
    private static final String HTTPS_RESOURCE_PREFIX = "https://";

    /**
     * 天爱验证码 classpath 资源类型。
     */
    private static final String CLASS_PATH_RESOURCE_TYPE = "classpath";

    /**
     * 天爱验证码 file 资源类型。
     */
    private static final String FILE_RESOURCE_TYPE = "file";

    /**
     * 天爱验证码 url 资源类型。
     */
    private static final String URL_RESOURCE_TYPE = "url";

    /**
     * classpath 通配扫描前缀。
     */
    private static final String CLASS_PATH_SCAN_PREFIX = "classpath*:";

    /**
     * 递归扫描通配后缀。
     */
    private static final String RECURSIVE_SCAN_PATTERN = "/**/*";

    /**
     * 支持自动扫描的图片后缀集合。
     */
    private static final Set<String> SUPPORTED_IMAGE_SUFFIXES = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    /**
     * 创建验证码缓存实现。
     *
     * @param redisTemplate 项目统一 RedisTemplate
     * @return 验证码缓存实现
     */
    @Bean
    public CacheStore captchaCacheStore(RedisTemplate<Object, Object> redisTemplate) {
        return new TianaiRedisCacheStore(redisTemplate);
    }

    /**
     * 创建验证码应用实例。
     *
     * @param captchaCacheStore 验证码缓存实现
     * @param captchaProperties 验证码配置
     * @return 验证码应用实例
     */
    @Bean
    public ImageCaptchaApplication imageCaptchaApplication(CacheStore captchaCacheStore,
                                                           CaptchaProperties captchaProperties,
                                                           ResourceLoader resourceLoader) {
        ImageCaptchaProperties imageCaptchaProperties = new ImageCaptchaProperties();
        Map<String, Long> expireMap = new HashMap<>();
        expireMap.put(CAPTCHA_TYPE_SLIDER, captchaProperties.getChallengeExpireMs());
        imageCaptchaProperties.setPrefix(captchaProperties.getRedisKeyPrefix() + ":challenge");
        imageCaptchaProperties.setExpire(expireMap);

        TACBuilder captchaBuilder = TACBuilder.builder()
                .setCacheStore(captchaCacheStore)
                .setProp(imageCaptchaProperties)
                .setTransform(new Base64ImageTransform());
        if (captchaProperties.isInitDefaultTemplate()) {
            captchaBuilder.addDefaultTemplate();
        }
        configureClasspathResourceLoader(resourceLoader);
        registerSliderBackgroundResources(captchaBuilder, captchaProperties.getResourcePrefix());
        return captchaBuilder.build();
    }

    /**
     * 为天爱验证码的 classpath 资源读取器绑定应用类加载器。
     * <p>
     * 避免在 Web 请求线程、异步线程或 fat-jar 场景下因上下文类加载器不同，
     * 导致无法读取 captcha/backgrounds 下的背景图资源。
     *
     * @param resourceLoader Spring 资源加载器
     */
    private void configureClasspathResourceLoader(ResourceLoader resourceLoader) {
        if (resourceLoader == null || resourceLoader.getClassLoader() == null) {
            ClassPathResourceProvider.setClassLoader(CaptchaAutoConfiguration.class.getClassLoader());
            return;
        }
        ClassPathResourceProvider.setClassLoader(resourceLoader.getClassLoader());
    }

    /**
     * 注册滑块背景图资源。
     *
     * @param captchaBuilder 构建器
     * @param resourcePrefix 资源目录前缀
     */
    private void registerSliderBackgroundResources(TACBuilder captchaBuilder, String resourcePrefix) {
        List<Resource> sliderBackgroundResources = scanSliderBackgroundResources(resourcePrefix);
        if (sliderBackgroundResources.isEmpty()) {
            throw new IllegalStateException("验证码背景图目录下未扫描到可用图片: " + resourcePrefix);
        }
        for (Resource sliderBackgroundResource : sliderBackgroundResources) {
            captchaBuilder.addResource(CAPTCHA_TYPE_SLIDER, sliderBackgroundResource);
        }
    }

    /**
     * 扫描滑块背景图资源目录中的全部图片。
     *
     * @param resourcePrefix 资源目录前缀
     * @return 滑块背景图资源列表
     */
    private List<Resource> scanSliderBackgroundResources(String resourcePrefix) {
        if (!StringUtils.hasText(resourcePrefix)) {
            throw new IllegalArgumentException("验证码背景图资源前缀不能为空");
        }
        String trimmedPrefix = resourcePrefix.trim();
        if (trimmedPrefix.startsWith(CLASS_PATH_RESOURCE_PREFIX)) {
            return scanClasspathSliderBackgroundResources(trimmedPrefix.substring(CLASS_PATH_RESOURCE_PREFIX.length()));
        }
        if (trimmedPrefix.startsWith(FILE_RESOURCE_PREFIX)) {
            return scanFileSliderBackgroundResources(trimmedPrefix.substring(FILE_RESOURCE_PREFIX.length()));
        }
        if (trimmedPrefix.startsWith(HTTP_RESOURCE_PREFIX) || trimmedPrefix.startsWith(HTTPS_RESOURCE_PREFIX)) {
            throw new IllegalArgumentException("HTTP/HTTPS 资源目录不支持自动扫描，请改用 classpath 或 file 目录: " + resourcePrefix);
        }
        throw new IllegalArgumentException("不支持的验证码背景图资源前缀: " + resourcePrefix);
    }

    /**
     * 扫描 classpath 目录中的全部图片资源。
     *
     * @param resourceDirectory classpath 资源目录
     * @return 滑块背景图资源列表
     */
    private List<Resource> scanClasspathSliderBackgroundResources(String resourceDirectory) {
        String normalizedResourceDirectory = normalizeDirectory(resourceDirectory);
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        String scanPattern = CLASS_PATH_SCAN_PREFIX + normalizedResourceDirectory + RECURSIVE_SCAN_PATTERN;
        try {
            org.springframework.core.io.Resource[] classpathResources = resourcePatternResolver.getResources(scanPattern);
            return Stream.of(classpathResources)
                    .filter(org.springframework.core.io.Resource::isReadable)
                    .map(resource -> resolveClasspathResourceLocation(normalizedResourceDirectory, resource))
                    .filter(this::isSupportedImageResource)
                    .sorted()
                    .map(resourceLocation -> new Resource(CLASS_PATH_RESOURCE_TYPE, resourceLocation))
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException exception) {
            throw new IllegalStateException("扫描 classpath 验证码背景图失败: " + resourceDirectory, exception);
        }
    }

    /**
     * 扫描文件系统目录中的全部图片资源。
     *
     * @param resourceDirectory 文件系统资源目录
     * @return 滑块背景图资源列表
     */
    private List<Resource> scanFileSliderBackgroundResources(String resourceDirectory) {
        Path rootPath = Path.of(resourceDirectory).normalize();
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            throw new IllegalArgumentException("验证码背景图文件目录不存在: " + resourceDirectory);
        }
        try (Stream<Path> pathStream = Files.walk(rootPath)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .map(Path::toAbsolutePath)
                    .map(Path::normalize)
                    .filter(path -> isSupportedImageResource(path.getFileName().toString()))
                    .sorted()
                    .map(path -> new Resource(FILE_RESOURCE_TYPE, path.toString()))
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException exception) {
            throw new IllegalStateException("扫描文件系统验证码背景图失败: " + resourceDirectory, exception);
        }
    }

    /**
     * 解析 classpath 资源的相对路径。
     *
     * @param resourceDirectory classpath 资源目录
     * @param classpathResource Spring 资源对象
     * @return classpath 相对路径
     */
    private String resolveClasspathResourceLocation(String resourceDirectory,
                                                    org.springframework.core.io.Resource classpathResource) {
        try {
            String resourceUrl = classpathResource.getURL().toString().replace('\\', '/');
            String directoryPrefix = resourceDirectory + "/";
            int directoryIndex = resourceUrl.indexOf(directoryPrefix);
            if (directoryIndex < 0) {
                throw new IllegalArgumentException("无法解析 classpath 验证码背景图路径: " + resourceUrl);
            }
            return resourceUrl.substring(directoryIndex);
        } catch (IOException exception) {
            throw new IllegalStateException("读取 classpath 验证码背景图路径失败: " + classpathResource, exception);
        }
    }

    /**
     * 归一化资源目录字符串。
     *
     * @param resourceDirectory 资源目录
     * @return 归一化后的目录
     */
    private String normalizeDirectory(String resourceDirectory) {
        String normalizedDirectory = resourceDirectory.trim().replace('\\', '/');
        if (normalizedDirectory.startsWith("/")) {
            normalizedDirectory = normalizedDirectory.substring(1);
        }
        if (normalizedDirectory.endsWith("/")) {
            normalizedDirectory = normalizedDirectory.substring(0, normalizedDirectory.length() - 1);
        }
        return normalizedDirectory;
    }

    /**
     * 判断资源是否为支持的图片类型。
     *
     * @param resourceLocation 资源路径或文件名
     * @return true 表示为支持的图片资源
     */
    private boolean isSupportedImageResource(String resourceLocation) {
        String normalizedResourceLocation = resourceLocation.toLowerCase(Locale.ROOT);
        for (String supportedImageSuffix : SUPPORTED_IMAGE_SUFFIXES) {
            if (normalizedResourceLocation.endsWith(supportedImageSuffix)) {
                return true;
            }
        }
        return false;
    }
}
