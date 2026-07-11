package com.hotchpotch.lottery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Python crawler 服务调用配置。
 */
@ConfigurationProperties(prefix = "lottery.crawler")
public class CrawlerProperties {

    /**
     * crawler 服务基础地址。
     */
    private String baseUrl = "http://127.0.0.1:8001";

    /**
     * 连接超时时间，单位毫秒。
     */
    private int connectTimeoutMillis = 3000;

    /**
     * 读取超时时间，单位毫秒。
     */
    private int readTimeoutMillis = 10000;

    public String baseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int connectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int readTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }
}
