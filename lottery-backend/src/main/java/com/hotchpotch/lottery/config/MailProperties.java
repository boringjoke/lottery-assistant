package com.hotchpotch.lottery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SMTP 邮件发送配置。
 */
@ConfigurationProperties(prefix = "lottery.mail")
public class MailProperties {

    /**
     * 是否启用邮件发送能力。
     */
    private boolean enabled = false;

    /**
     * SMTP 服务器地址。
     */
    private String host = "";

    /**
     * SMTP 服务器端口。
     */
    private int port = 465;

    /**
     * SMTP 登录账号，通常是发件邮箱。
     */
    private String username = "";

    /**
     * SMTP 授权码或应用专用密码。
     */
    private String password = "";

    /**
     * 发件人邮箱。
     */
    private String from = "";

    /**
     * 是否启用 SMTP 认证。
     */
    private boolean auth = true;

    /**
     * 是否启用 SSL 连接。
     */
    private boolean sslEnabled = true;

    /**
     * 是否启用 STARTTLS。
     */
    private boolean starttlsEnabled = false;

    /**
     * 连接超时时间，单位毫秒。
     */
    private int connectTimeoutMillis = 5000;

    /**
     * 读取超时时间，单位毫秒。
     */
    private int readTimeoutMillis = 10000;

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String host() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int port() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String username() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String password() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String from() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public boolean auth() {
        return auth;
    }

    public void setAuth(boolean auth) {
        this.auth = auth;
    }

    public boolean sslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public boolean starttlsEnabled() {
        return starttlsEnabled;
    }

    public void setStarttlsEnabled(boolean starttlsEnabled) {
        this.starttlsEnabled = starttlsEnabled;
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
