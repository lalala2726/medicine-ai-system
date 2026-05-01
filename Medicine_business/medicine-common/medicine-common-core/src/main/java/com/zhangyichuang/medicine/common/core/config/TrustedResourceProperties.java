package com.zhangyichuang.medicine.common.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 资源地址可信域名配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "resource.trusted")
public class TrustedResourceProperties {

    /**
     * 可信域名列表（仅域名部分）。
     */
    private List<String> domains = new ArrayList<>(List.of("localhost", "127.0.0.1", "::1"));

    /**
     * 允许的文件名列表（精确匹配）。
     */
    private List<String> fileNames = new ArrayList<>();

    /**
     * 文件名正则（可选）。
     */
    private String fileNamePattern;

}
