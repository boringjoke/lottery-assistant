package com.hotchpotch.lottery.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * crawler HTTP 客户端基础配置。
 */
@Configuration
public class CrawlerClientConfig {

    /**
     * 创建 crawler 专用 RestClient Builder，并应用连接与读取超时。
     */
    @Bean
    @Qualifier("crawlerRestClientBuilder")
    public RestClient.Builder crawlerRestClientBuilder(CrawlerProperties crawlerProperties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(crawlerProperties.connectTimeoutMillis()))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(crawlerProperties.readTimeoutMillis()));

        return RestClient.builder().requestFactory(requestFactory);
    }
}
