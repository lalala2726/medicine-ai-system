package com.zhangyichuang.medicine.common.systemauth.inbound;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 支持重复读取请求体的包装请求。
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    /**
     * 缓存后的请求体字节。
     */
    private final byte[] cachedBody;

    /**
     * 读取并缓存原始请求体。
     *
     * @param request 原始请求
     * @throws IOException 读取请求体失败时抛出
     */
    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    /**
     * 返回缓存的请求体副本，避免外部修改内部状态。
     */
    public byte[] getCachedBody() {
        return cachedBody.clone();
    }

    /**
     * 基于缓存内容重新构造输入流，供后续过滤器与控制器重复读取。
     */
    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override
            public int read() {
                return inputStream.read();
            }

            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // no-op
            }
        };
    }

    /**
     * 返回基于缓存内容的字符流读取器。
     */
    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
