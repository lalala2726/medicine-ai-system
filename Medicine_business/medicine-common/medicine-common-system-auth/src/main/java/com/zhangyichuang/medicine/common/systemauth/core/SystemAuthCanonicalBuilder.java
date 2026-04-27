package com.zhangyichuang.medicine.common.systemauth.core;

import okhttp3.HttpUrl;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 系统签名 canonical 字符串构造器。
 */
@Component
public class SystemAuthCanonicalBuilder {

    /**
     * Hex 编码器，统一输出小写十六进制。
     */
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    /**
     * 构造 canonical 字符串（六段换行拼接）。
     *
     * @param method      HTTP 方法
     * @param path        请求路径
     * @param sortedQuery 排序后的 query 字符串
     * @param timestamp   时间戳
     * @param nonce       一次性随机串
     * @param bodySha256  请求体哈希
     * @return 可直接参与 HMAC 计算的 canonical 字符串
     */
    public String buildCanonical(String method,
                                 String path,
                                 String sortedQuery,
                                 String timestamp,
                                 String nonce,
                                 String bodySha256) {
        return String.join("\n",
                normalize(method).toUpperCase(),
                normalize(path),
                normalize(sortedQuery),
                normalize(timestamp),
                normalize(nonce),
                normalize(bodySha256));
    }

    /**
     * 基于原始 query 字符串构造排序后 query。
     *
     * @param rawQuery 原始 query 字符串
     * @return 排序并编码后的 query 字符串
     */
    public String buildSortedQuery(String rawQuery) {
        List<QueryPair> pairs = parseRawQuery(rawQuery);
        return formatSortedQuery(pairs);
    }

    /**
     * 基于 OkHttp URL 构造排序后 query。
     *
     * @param url OkHttp URL
     * @return 排序并编码后的 query 字符串
     */
    public String buildSortedQuery(HttpUrl url) {
        if (url == null) {
            return "";
        }
        List<QueryPair> pairs = new ArrayList<>();
        Set<String> names = url.queryParameterNames();
        for (String name : names) {
            List<String> values = url.queryParameterValues(name);
            if (values == null || values.isEmpty()) {
                pairs.add(new QueryPair(name, ""));
                continue;
            }
            for (String value : values) {
                pairs.add(new QueryPair(name, value));
            }
        }
        return formatSortedQuery(pairs);
    }

    /**
     * 计算 body SHA-256（小写 hex）。
     *
     * @param bodyBytes 请求体原始字节
     * @return 小写 hex 格式的 SHA-256 哈希
     */
    public String sha256Hex(byte[] bodyBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] raw = digest.digest(bodyBytes == null ? new byte[0] : bodyBytes);
            return HEX_FORMAT.formatHex(raw);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }

    /**
     * 对 query 对进行稳定排序并重新拼接。
     */
    private String formatSortedQuery(List<QueryPair> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return "";
        }
        pairs.sort(Comparator.comparing(QueryPair::key).thenComparing(QueryPair::value));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pairs.size(); i++) {
            QueryPair pair = pairs.get(i);
            if (i > 0) {
                builder.append('&');
            }
            builder.append(encode(pair.key())).append('=').append(encode(pair.value()));
        }
        return builder.toString();
    }

    /**
     * 解析原始 query，保留重复参数与空值。
     */
    private List<QueryPair> parseRawQuery(String rawQuery) {
        List<QueryPair> pairs = new ArrayList<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return pairs;
        }
        String[] segments = rawQuery.split("&", -1);
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            int index = segment.indexOf('=');
            if (index < 0) {
                pairs.add(new QueryPair(decode(segment), ""));
                continue;
            }
            String key = segment.substring(0, index);
            String value = segment.substring(index + 1);
            pairs.add(new QueryPair(decode(key), decode(value)));
        }
        return pairs;
    }

    /**
     * 对 query key/value 进行 RFC 3986 风格编码。
     */
    private String encode(String value) {
        String source = value == null ? "" : value;
        return URLEncoder.encode(source, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }

    /**
     * 对 query key/value 做 URL 解码。
     */
    private String decode(String value) {
        String source = value == null ? "" : value;
        try {
            return URLDecoder.decode(source, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return source;
        }
    }

    /**
     * 将 null 归一化为空串，避免 canonical 构造出现空指针。
     */
    private String normalize(String value) {
        return value == null ? "" : value;
    }

    /**
     * query 参数键值对。
     */
    private record QueryPair(String key, String value) {
    }
}
