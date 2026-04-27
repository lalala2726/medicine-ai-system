package com.zhangyichuang.medicine.common.http.model;

import lombok.Getter;
import okhttp3.Headers;
import okhttp3.HttpUrl;

/**
 * @author Chuang
 * <p>
 * created on 2026/1/31
 */
@Getter
public final class ClientRequest {

    private final HttpMethod method;
    private final HttpUrl url;
    private final Headers headers;
    private final String body;

    /**
     * 构造客户端请求。
     *
     * @param method  请求方法
     * @param url     请求URL
     * @param headers 请求头
     * @param body    请求体
     */
    private ClientRequest(HttpMethod method, HttpUrl url, Headers headers, String body) {
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.body = body;
    }

    /**
     * 获取建造器。
     *
     * @return 请求建造器
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private HttpMethod method;
        private HttpUrl url;
        private Headers headers;
        private String body;
        private HttpUrl.Builder urlBuilder;
        private Headers.Builder headersBuilder;

        /**
         * 直接传入 URL 字符串并支持继续追加 query 参数。
         */
        public Builder url(String url) {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("url must not be blank");
            }
            HttpUrl parsed = HttpUrl.parse(url);
            if (parsed == null) {
                throw new IllegalArgumentException("url is invalid: " + url);
            }
            this.urlBuilder = parsed.newBuilder();
            this.url = null;
            return this;
        }

        /**
         * 直接传入 HttpUrl 并支持继续追加 query 参数。
         */
        public Builder url(HttpUrl url) {
            if (url == null) {
                throw new IllegalArgumentException("url must not be null");
            }
            this.urlBuilder = url.newBuilder();
            this.url = null;
            return this;
        }

        /**
         * 追加 query 参数（需要先设置 url）。
         */
        public Builder addQueryParameter(String name, String value) {
            ensureUrlBuilder();
            this.urlBuilder.addQueryParameter(name, value);
            return this;
        }

        /**
         * 追加请求头（在原 headers 基础上追加）。
         *
         * @param name  请求头名称
         * @param value 请求头值
         * @return 当前建造器
         */
        public Builder addHeader(String name, String value) {
            ensureHeadersBuilder();
            this.headersBuilder.add(name, value);
            return this;
        }

        /**
         * 直接设置完整请求头集合。
         *
         * @param headers 请求头集合
         * @return 当前建造器
         */
        public Builder headers(Headers headers) {
            this.headers = headers;
            this.headersBuilder = null;
            return this;
        }

        /**
         * 构建 ClientRequest，确保 url 与 headers 最终一致。
         */
        public ClientRequest build() {
            if (urlBuilder != null) {
                this.url = urlBuilder.build();
            }
            if (headersBuilder != null) {
                this.headers = headersBuilder.build();
            }
            if (this.url == null) {
                throw new IllegalStateException("url must not be null");
            }
            return buildInternal();
        }

        /**
         * 设置请求方法。
         *
         * @param method 请求方法
         * @return 当前建造器
         */
        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        /**
         * 设置请求体。
         *
         * @param body 请求体
         * @return 当前建造器
         */
        public Builder body(String body) {
            this.body = body;
            return this;
        }

        /**
         * 构建内部请求对象。
         *
         * @return 客户端请求
         */
        public ClientRequest buildInternal() {
            return new ClientRequest(method, url, headers, body);
        }

        private void ensureUrlBuilder() {
            if (this.urlBuilder == null) {
                if (this.url == null) {
                    throw new IllegalStateException("url must be set before adding query parameters");
                }
                this.urlBuilder = this.url.newBuilder();
                this.url = null;
            }
        }

        private void ensureHeadersBuilder() {
            if (this.headersBuilder == null) {
                this.headersBuilder = this.headers != null
                        ? this.headers.newBuilder()
                        : new Headers.Builder();
                this.headers = null;
            }
        }
    }
}
